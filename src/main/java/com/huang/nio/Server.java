package com.huang.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

	private static final Logger log = LoggerFactory.getLogger(Server.class);
	
	private ServerSocketChannel serverSocketChannel = null;
	
	private int port;
	
	public Server(int port) {
		this.port = port;
	}
	
	private volatile boolean isRunning = true;
	
	
	private ServerSocketChannel getServerSocketChannel() throws IOException {
		final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		return serverSocketChannel;
	}
	
	public void startup() throws IOException{
		serverSocketChannel = getServerSocketChannel();
		serverSocketChannel.bind(new InetSocketAddress(port),100);
		new Thread(accepter).start();
		log.info("server[port={}] startup successed.",port);
	}
	
	public void shutdown() throws IOException{
		
		isRunning = false;
		if(null!=serverSocketChannel){
			serverSocketChannel.close();
		}
		
		log.info("server[port={}] shutdown successed.",port);
		
	}
	
	final Runnable accepter = new Runnable() {

		@Override
		public void run() {
			Thread.currentThread().setName("server-accepter");
			try (final Selector selector = Selector.open()) {
				serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

				while (isRunning) {
					selector.select();
					final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
					while (iter.hasNext()) {
						final SelectionKey key = iter.next();
						iter.remove();

						if (key.isAcceptable()) {
							final SocketChannel socketChannel = serverSocketChannel.accept();
							socketChannel.configureBlocking(false);
							new ChildHandler(socketChannel);
							log.info("{} was connected.",socketChannel.socket());
						}
					}
				}
			} catch (Exception e) {
				log.warn("server[port={}] accept failed.",port,e);
			}
		}
	};
	
	private class ChildHandler {

		private final SocketChannel socketChannel;
		private final AtomicInteger reqCounter = new AtomicInteger(1);

		private ChildHandler(SocketChannel socketChannel) {
			this.socketChannel = socketChannel;
			new Thread(childReader).start();
			new Thread(childWriter).start();
		}

		final Runnable childReader = new Runnable() {

			@Override
			public void run() {
				Thread.currentThread().setName("child-" + socketChannel.socket() + "-reader");

				final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
				try (final Selector selector = Selector.open()) {
					socketChannel.register(selector, SelectionKey.OP_READ);
					while (isRunning) {
						selector.select();
						final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
						while (iter.hasNext()) {
							final SelectionKey key = iter.next();
							iter.remove();

							if (key.isReadable()) { //获取数据
								log.info("before read:"+buffer.position()+","+buffer.limit()+","+buffer.remaining());
								socketChannel.read(buffer);
								log.info("before flip:"+buffer.position()+","+buffer.limit()+","+buffer.remaining());
								buffer.flip();
								log.info("after flip:"+buffer.position()+","+buffer.limit()+","+buffer.remaining());
								while (true) { //有几个int，一次读取完
									//先获取一个长度
									if (buffer.remaining() < Integer.BYTES) { //半包，不处理，直接返回
//										log.info("server half packet");
										break;
									}else{
										final int req = buffer.getInt();
										reqCounter.getAndIncrement();
										log.info("client req:"+req);
									}
								}
								
								//由于有compact存在，则每次半包后，可以回归到原始状态。
								buffer.compact();
							}
						}
					}
				} catch (Exception e) {
					log.info("{} was disconnect for read.", socketChannel.socket());
				}
			}
		};

		final Runnable childWriter = new Runnable() {

			@Override
			public void run() {
				Thread.currentThread().setName("child-" + socketChannel.socket() + "-writer");
				Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
				final ByteBuffer buffer = ByteBuffer.allocateDirect(128);
				final WritableByteChannel writableByteChannel = socketChannel;
				try (final Selector selector = Selector.open()) {
					socketChannel.register(selector, SelectionKey.OP_WRITE);
					while (isRunning) {
						int req = reqCounter.get();
						if(req<0){
							Thread.sleep(100);
						}else{
							req = reqCounter.decrementAndGet();
						}
						String res = "res:"+req;
						int currentLen = buffer.remaining()-res.getBytes().length-4;
						if(currentLen<0){
							
							log.info("remaining size less 100:"+currentLen);
							selector.select();
							final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
							while (iter.hasNext()) {
								final SelectionKey key = iter.next();
								iter.remove();
								if (key.isWritable()) {
									buffer.flip();
									while (buffer.hasRemaining()) {
										writableByteChannel.write(buffer);
									}
									buffer.compact();
									log.info("write a buffer to client");
								}
							}
							break;
						}else if(reqCounter.get()>0){
							buffer.putInt(res.getBytes().length);
							buffer.put(res.getBytes());
						}
					}
				} catch (Exception e) {
					log.info("{} was disconnect for write.",socketChannel.socket());
				}
			}
		};

	}
	
	public static void main(String[] args) throws IOException {
		Server server = new Server(9999);
		server.startup();
		LockSupport.park();
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			try {
				server.shutdown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
		
	}
	
}
