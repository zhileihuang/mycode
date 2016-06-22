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

//so_timeout。
//当输入流的read方法被阻塞时，如果设置timeout(timeout的单位是毫秒),那么系统在等待了timeout毫秒后会抛出一个interruptedIOException例外。
//在抛出例外后，输入流并未关闭，你可以继续通过read方法读取数据。如果将timeout设置为0，就意味着read将会无限等待下去，直到服务端程序关系这个socket。
//这也是timeout的默认值

//tcp_nodelay
//默认情况下，客户端向服务器发送数据时，会根据数据包的大小决定是否立即发送。当数据包中的数据很少时，如只有一个字节，而数据包的头却有几十个字节(IP头+TCP头)时，
//系统会在发送之前先将较小的包合并到较大的包中，一起将数据发送出去。在发送下一个数据包时，系统会等待服务器对前一个数据包的响应，当收到服务器的响应后，
//再发送下一个数据包，这就是所谓的nagle算法;在默认情况下，nagle算法是开启的。

//so_reuseaddr
//错误的说法:通过这个选项，可以使多个socket对象绑定在同一个端口上。
//正确的说明:如果端口忙，但tcp状态位于time_wait，可以重用端口。
//如果端口忙，而tcp状态位于其他状态，重用端口时依旧得到一个错误信息，抛出"address already in use: JVM_Bind".
//如果你的服务程序停止后想立即重启，不等60秒，而新套接字依旧使用同一个端口，此时so_reuseaddr选项非常有用。必须意识到，
//此时任何非期望数据到达，都可能导致服务程序反应混乱，不过这只是一种可能，事实上很不可能。

//so_linger
//这个socket选项可以影响close方法的行为。在默认情况下，当调用close方法后，将立即返回；如果这时仍然有未被送出的数据包，
//那么这些数据包将被丢弃。如果将linger参数设置为一个正整数n时(n的最大值是65535),在调用close方法后，将最多被阻塞n秒。
//在这n秒内，系统将尽量将未送出的数据包发送出去；如果超过了n秒，如果还有未发送的数据包，这些数据包将全部丢弃;而close方法
//会立即返回。如果将linger设置为0和关闭so_linger选项的作用是一样的。

//so_sndbuf
//在默认情况下，输出流的发送缓冲区是8096个字节(8k)。这个值是java所建立的输出缓冲区的大小。如果这个morenzhi
//不能满足要求，可以用setSendBufferSize方法来重新设置缓冲区的大小。但最好不要将输出缓冲区设得太小，
//否则会导致传输数据过于频繁，从而降低网络传输的效率

//so_rcvbuf
//在默认情况下，输入流的接收缓冲区是8096个字节(8K)。这个值是java所建立的输入缓冲区的大小。如果这个
//默认值不能满足要求，可以用setReceiveBufferSize方法来重新设置缓冲区的大小。但最好不要讲输入缓冲区
//设置的太小，否则会导致传输数据过于频繁，从而降低网络传输的效率

//so_keepalive
//如果将这个socket选项打开，客户端socket每隔一段时间(大约两个小时)就会利用空闲的连接向服务器发送一个数据包。
//这个数据包并没有其他的作用，只是为了检测一下服务器是否仍处于活动状态。如果服务器未响应这个数据包，在大约11分钟后，
//客户端socket再发送一个数据包，如果在12分钟内，服务器还没响应，那么客户端socket将关闭。
//如果将socket选项关闭，客户端socket在服务器无效的情况下可能会长时间不会关闭。
//so_keepalive选项在默认情况下是关闭的，可以使用setKeepAlive(true) 打开

//backlog
//服务端socket处理客户端socket连接是需要一定时间的。serversocket有一个队列，存放还没有来得及处理的客户端socket,
//这个队列的容量就是backlog的含义。如果队列已经被客户端socket占满了，如果还有新的连接过来，那么serversocket会拒绝
//新的连接。也就是说backlog提供了容量限制功能，避免太多的客户端socket占用太多的服务器资源。一旦accept，则会从队列中
//取出一个连接进行处理

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
		
		//参数：connectionTime:表示用最少时间建立连接。
		//参数：latency:表示最小延迟。
		//参数bandwidth:表示最高带宽。
		//假设：setPerformancePreferences(2,1,3)
		//则其表示最高带宽优先，其次是最少连接时间，最后是最小延迟
		socket.setPerformancePreferences(options.getServerChildPerformancePreferences()[0], options.getServerChildPerformancePreferences()[1], options.getServerChildPerformancePreferences()[2]);
		
		//设置发送数据包头的流量类型或服务类型字段，默认为8，意为吞吐量最大化传输   
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
