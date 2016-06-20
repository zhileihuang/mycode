package com.huang.rpc.client;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huang.rpc.common.Options;
import com.huang.rpc.datasource.DataPersistence;

public class NioClient {

	private static final Logger log = LoggerFactory.getLogger(NioClient.class);
	
	private final CountDownLatch countDownLatch;
	private final CyclicBarrier cyclicBarrier;
	private final ExecutorService executorService;
	private final DataPersistence dataPersistence;
	private final ClientConfig config;
	private final Options options;
	private SocketChannel socketChannel;
	private volatile boolean isRunning = true;
	
	public NioClient(CountDownLatch countDownLatch, CyclicBarrier cyclicBarrier, ExecutorService executorService, DataPersistence dataPersistence, ClientConfig config, Options options) {
		this.countDownLatch = countDownLatch;
		this.cyclicBarrier = cyclicBarrier;
		this.executorService = executorService;
		this.dataPersistence = dataPersistence;
		this.config = config;
		this.options = options;
	}
	
	private SocketChannel getAndConfigSocketChannel() throws IOException{
		final SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		final Socket socket = socketChannel.socket();
		socket.setTcpNoDelay(options.isClientTcpNoDelay());
		socket.setReceiveBufferSize(options.getClientReceiverBufferSize());
		socket.setSendBufferSize(options.getClientSendBufferSize());
		socket.setSoTimeout(options.getClientSocketTimeout());
		socket.setPerformancePreferences(options.getClientPerformancePreferences()[0], 
										 options.getClientPerformancePreferences()[1], 
										 options.getClientPerformancePreferences()[2]);
		socket.setTrafficClass(options.getClientTrafficClass());
		return socketChannel;
	}
	
	public void connect() throws IOException{
		socketChannel = getAndConfigSocketChannel();
		socketChannel.connect(config.getServerAddress());
		try(final Selector selector = Selector.open()){
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
			WAITING_FOR_CONNECT:
			for(;;){
				selector.select();
				final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
				while(iter.hasNext()){
					final SelectionKey key = iter.next();
					iter.remove();
					
					if(key.isConnectable()){
						final SocketChannel channel = (SocketChannel)key.channel();
						if(channel.isConnectionPending()){
							channel.finishConnect();
							break WAITING_FOR_CONNECT;
						}
					}
					
				}
			}
		}
		log.info("{} connect successed.",socketChannel.socket());
	}
	
	final Runnable writer = new Runnable() {
		
		@Override
		public void run() {
			Thread.currentThread().setName("client-"+socketChannel.socket());
			try(final Selector selector = Selector.open()){
				try{
					cyclicBarrier.await();
				}catch(Exception e){
					log.warn("work cb await failed",e);
				}
				
				final ByteBuffer buffer = ByteBuffer.allocateDirect(options.getClientSendBufferSize());
				
				while(isRunning){
					if(buffer.remaining()>=Integer.MAX_VALUE){
//						buffer.putInt(value)
					}
				}
				
			}catch (Exception e) {
			}
		}
	};
	
	
}
