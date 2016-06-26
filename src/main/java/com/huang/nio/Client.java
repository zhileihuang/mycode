package com.huang.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {

	private static final Logger log = LoggerFactory.getLogger(Client.class);

	private SocketChannel socketChannel;
	private volatile boolean isRunning = true;
	private int serverPort;
	
	public Client(int serverPort) {
		this.serverPort = serverPort;
	}
	
	private AtomicInteger req = new AtomicInteger(10000);

	private SocketChannel getAndConfigSocketChannel() throws IOException {
		final SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		return socketChannel;
	}

	public void connect() throws IOException {
		socketChannel = getAndConfigSocketChannel();
		socketChannel.connect(new InetSocketAddress(serverPort));
		try (final Selector selector = Selector.open()) {
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
			WAITING_FOR_CONNECT: for (;;) {
				selector.select();
				final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
				while (iter.hasNext()) {
					final SelectionKey key = iter.next();
					iter.remove();

					if (key.isConnectable()) {
						final SocketChannel channel = (SocketChannel) key.channel();
						if (channel.isConnectionPending()) {
							// block until connect finished
							channel.finishConnect();
							break WAITING_FOR_CONNECT;
						}
					}

				}
			}
		}
		log.info("{} connect successed.", socketChannel.socket());
	}

	public void disconnect() throws IOException {
		isRunning = false;
		if (null != socketChannel) {
			socketChannel.close();
			log.info("{} discount successed.", socketChannel.socket());
		} else {
			log.info("{} disconnect successed.");
		}
	}

	// 写线程
	final Runnable writer = new Runnable() {

		@Override
		public void run() {
			Thread.currentThread().setName("client-" + socketChannel.socket());
			try (final Selector selector = Selector.open()) {
				final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
				socketChannel.register(selector, SelectionKey.OP_WRITE);
				
				while (isRunning) {
					selector.select();

					final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
					while (iter.hasNext()) {
						final SelectionKey key = iter.next();
						iter.remove();
						if (key.isWritable()) {
							log.info("send size:" + buffer.limit());
							int reqNum = req.getAndIncrement();
							buffer.putInt(reqNum);
							buffer.flip();
							byte[] data = new byte[4];
							buffer.get(data);
							

							
							for(int i = 0;i<data.length;i++){
								buffer.clear();
								buffer.put(data[i]);
								while(buffer.hasRemaining()){
									socketChannel.write(buffer);
								}
								
								Thread.sleep(100);
							}
							log.info("req success:"+reqNum+",len:"+data.length);
							buffer.compact();
						}
					}

				}

			} catch (Exception e) {
				if (!socketChannel.socket().isClosed()) {
					log.warn("{} write failed.", socketChannel.socket(), e);
				}
			}
		}
	};

	final Runnable reader = new Runnable() {

		@Override
		public void run() {

			Thread.currentThread().setName("client-" + socketChannel.socket());
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

			final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

			DecodeState state = DecodeState.READ_LEN;
			
			final ReadableByteChannel readableByteChannel = socketChannel;

			try (final Selector selector = Selector.open()) {
				socketChannel.register(selector, SelectionKey.OP_READ);
				while (isRunning) {
					selector.select();
					final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

					while (iter.hasNext()) {
						final SelectionKey key = iter.next();
						iter.remove();

						if (key.isReadable()) {
							readableByteChannel.read(buffer);
							buffer.flip();
							boolean hasMore = true;
							int len = 0;
							while (hasMore) {
								hasMore = false;
									switch (state) {
										case READ_LEN:{
											if(buffer.remaining()<Integer.BYTES){
												log.info("client receive a half pack");
												break;
											}else{
												len = buffer.getInt();
												state = DecodeState.READ_BYTES;
											}
										}
										case READ_BYTES:{
											if(buffer.remaining()<len){
												log.info("client receive two a half pack:"+len);
												break;
											}else{
												final byte[] data = new byte[len];
												buffer.get(data);
												log.info("client receive a data:"+new String(data));
												hasMore = true;
												state = DecodeState.READ_LEN;
												break;
											}
										}
										default:
											break;
									}
							}
							buffer.compact();
						}
					}

				}

			} catch (Exception e) {
				if (!socketChannel.socket().isClosed()) {
					log.warn("{} read failed.", socketChannel.socket(), e);
				}
			}
		}
	};
	
	public void work() throws IOException{
		new Thread(reader).start();
		new Thread(writer).start();
	}
	
	private static enum DecodeState{
		READ_LEN,
		READ_BYTES,
	}
	
	public static void main(String[] args) throws IOException {
		Client client = new Client(9999);
		client.connect();
		client.work();
		
		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			try {
				client.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
		
	}

}
