package com.huang.rpc.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huang.rpc.channel.CompressWritableByteChannel;
import com.huang.rpc.common.Constant;
import com.huang.rpc.common.Options;
import com.huang.rpc.datasource.DataSource;
import com.huang.rpc.datasource.Row;

public class NioServer {

	private static final Logger log = LoggerFactory.getLogger(NioServer.class);

	private final DataSource dataSource;
	private final ExecutorService executorService;
	private final ServerConfig config;
	private final Options options;

	private ServerSocketChannel serverSocketChannel;

	private volatile boolean isRunning = true;
	private boolean isReaderRunning = true;
	private boolean isWriterRunning = true;

	public NioServer(DataSource dataSource, ExecutorService executorService, ServerConfig config, Options options) {
		this.dataSource = dataSource;
		this.executorService = executorService;
		this.config = config;
		this.options = options;
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
							configSocketChannel(socketChannel);
							new ChildHandler(socketChannel);
							log.info("{} was connected.",socketChannel.socket());
						}
					}
				}
			} catch (Exception e) {
				log.warn("server[port={}] accept failed.",config.getPort(),e);
			}
		}
	};

	private ServerSocketChannel getServerSocketChannel() throws IOException {
		final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.socket().setSoTimeout(options.getServerSocketTimeout());
		return serverSocketChannel;
	}

	private void configSocketChannel(SocketChannel socketChannel) throws IOException {

		socketChannel.configureBlocking(false);
		final Socket socket = socketChannel.socket();
		socket.setTcpNoDelay(options.isServerChildTcpNoDelay());
		socket.setReceiveBufferSize(options.getServerChildSocketReceiverBufferSize());
		socket.setSendBufferSize(options.getServerChildSocketSendBufferSize());
		socket.setSoTimeout(options.getServerChildSocketTimeout());
		socket.setPerformancePreferences(options.getServerChildPerformancePreferences()[0], options.getServerChildPerformancePreferences()[1], options.getServerChildPerformancePreferences()[2]);
		socket.setTrafficClass(options.getServerChildTrafficClass());

	}

	private class ChildHandler {

		private final SocketChannel socketChannel;
		private final AtomicInteger reqCounter = new AtomicInteger(0);

		private ChildHandler(SocketChannel socketChannel) {
			this.socketChannel = socketChannel;
			executorService.execute(childReader);
			executorService.execute(childWriter);
		}

		final Runnable childReader = new Runnable() {

			@Override
			public void run() {
				Thread.currentThread().setName("child-" + socketChannel.socket() + "-reader");

				final ByteBuffer buffer = ByteBuffer.allocateDirect(options.getServerChildReceiverBufferSize());
				try (final Selector selector = Selector.open()) {
					socketChannel.register(selector, SelectionKey.OP_READ);
					while (isRunning && isReaderRunning) {
						selector.select();
						final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
						while (iter.hasNext()) {
							final SelectionKey key = iter.next();
							iter.remove();

							if (key.isReadable()) {
								socketChannel.read(buffer);
								buffer.flip();
								while (true) {
									if (buffer.remaining() < Integer.BYTES) {
										break;
									}
									final int type = buffer.getInt();
									if (type != Constant.PRO_REQ_GETDATA) {
										throw new IOException("decode failed,illegal type=" + type);
									}
									reqCounter.incrementAndGet();
								}
								buffer.compact();
							}
						}

					}
				} catch (Exception e) {
					log.info("{} was disconnect for read.", socketChannel.socket());
				} finally {
					isReaderRunning = false;
				}
			}
		};

		final Runnable childWriter = new Runnable() {

			@Override
			public void run() {
				Thread.currentThread().setName("child-" + socketChannel.socket() + "-writer");
				Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

				final ByteBuffer buffer = ByteBuffer.allocateDirect(options.getServerChildSendBufferSize());

				final WritableByteChannel writableByteChannel = options.isEnableCompress() ? new CompressWritableByteChannel(socketChannel, options.getCompressSize()) : socketChannel;

				boolean isEOF = false;

				final Row row = new Row();
				try (final Selector selector = Selector.open()) {
					final int LIMIT_REMAINING = 212; // type(4b)+linenum(4b)+len(4b)+data(200b)
					socketChannel.register(selector, SelectionKey.OP_WRITE);
					DecodeState state = DecodeState.FILL_BUFF;
					boolean isNeedSend = false;

					while (isRunning && isWriterRunning) {
						switch (state) {
							case FILL_BUFF: {
								// 一进来就先判断是否到达了eof,如果已经到达了，则不需要访问数据源
								if (isEOF) {
									reqCounter.decrementAndGet();
									buffer.putInt(Constant.PRO_RESP_GETEOF);
									isNeedSend = true;
								} else {
									if (reqCounter.get() > 0) {
										reqCounter.decrementAndGet();
										dataSource.getRow(row);
	
										if (row.getLineNum() < 0) {
											buffer.putInt(Constant.PRO_RESP_GETEOF);
											isEOF = true;
											isNeedSend = true;
										} else {
											buffer.putInt(Constant.PRO_RESP_GET_DATA);
											buffer.putInt(row.getLineNum());
											buffer.putInt(row.getData().length);
											buffer.put(row.getData());
											if (buffer.remaining() < LIMIT_REMAINING) {
												// TODO 目前这里利用了data
												// 长度不超过200的限制，没有足够的通用性，后续改掉
												isNeedSend = true;
											}
										}
									}
								}
	
								// 前边层层处理之后是否需要发送
								if (isNeedSend) {
									buffer.flip();
									state = DecodeState.SEND_BUFF;
									isNeedSend = false;
								}
								break;
							}
	
							case SEND_BUFF: {
								selector.select();
								final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
								while (iter.hasNext()) {
									final SelectionKey key = iter.next();
									iter.remove();
	
									if (key.isWritable()) {
	
										while (buffer.hasRemaining()) {
											writableByteChannel.write(buffer);
										}
										buffer.compact();
										state = DecodeState.FILL_BUFF;
	
									}
	
								}
								break;
							}
						}
					}
				} catch (Exception e) {
					log.info("{} was disconnect for write.",socketChannel.socket());
				}finally{
					isWriterRunning = false;
				}
			}
		};

	}
	
	public void startup() throws IOException{
		serverSocketChannel = getServerSocketChannel();
		serverSocketChannel.bind(new InetSocketAddress(config.getPort()),options.getServerBacklog());
		executorService.execute(accepter);
		log.info("server[port={}] startup successed.",config.getPort());
	}
	
	public void shutdown() throws IOException{
		
		isRunning = false;
		if(null!=serverSocketChannel){
			serverSocketChannel.close();
		}
		
		log.info("server[port={}] shutdown successed.",config.getPort());
		
	}

	enum DecodeState {
		FILL_BUFF, SEND_BUFF
	}

}
