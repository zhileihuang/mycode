package com.huang.rpc.client;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huang.rpc.channel.CompressReadableByteChannel;
import com.huang.rpc.common.Constant;
import com.huang.rpc.common.Options;
import com.huang.rpc.datasource.DataPersistence;
import com.huang.rpc.datasource.Row;
import com.huang.rpc.util.Utils;

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
							//block until connect finished
							channel.finishConnect();
							break WAITING_FOR_CONNECT;
						}
					}
					
				}
			}
		}
		log.info("{} connect successed.",socketChannel.socket());
	}
	
	//写线程
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
					//超过最大值了
					if(buffer.remaining()>=Integer.MAX_VALUE){
						buffer.putInt(Constant.PRO_REQ_GETDATA);
						continue;
					}else{
						socketChannel.register(selector, SelectionKey.OP_WRITE);
						buffer.flip();
					}
					
					selector.select();
					
					final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
					while(iter.hasNext()){
						final SelectionKey key = iter.next();
						iter.remove();
						if(key.isWritable()){
							socketChannel.write(buffer);
							buffer.compact();
							key.interestOps(key.interestOps()&~SelectionKey.OP_WRITE);
						}
					}
					
					
				}
				
			}catch (Exception e) {
				if(!socketChannel.socket().isClosed()){
					log.warn("{} write failed.",socketChannel.socket(),e);
				}
			}
		}
	};
	
	final Runnable reader = new Runnable() {
		
		@Override
		public void run() {
			
			Thread.currentThread().setName("client-"+socketChannel.socket());
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			
			final ByteBuffer buffer = ByteBuffer.allocateDirect(options.getClientReceiverBufferSize());
			
			final ReadableByteChannel readableByteChannel = options.isEnableCompress()?new CompressReadableByteChannel(socketChannel, options.getCompressSize()):socketChannel;
			
			try(final Selector selector = Selector.open()){
				try{
					cyclicBarrier.await();
				}catch(Exception e){
					log.warn("workCB await failed.",e);
				}
				
				int type;
				int lineNum = 0;
				int len = 0;
				final Row row = new Row();
				
				DecodeState state = DecodeState.READ_TYPE;
				socketChannel.register(selector, SelectionKey.OP_READ);
				MAIN_LOOP:
				while(isRunning){
					selector.select();
					final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
					
					while(iter.hasNext()){
						final SelectionKey key = iter.next();
						iter.remove();
						
						if(key.isReadable()){
							readableByteChannel.read(buffer);
							buffer.flip();
							boolean hasMore = true;
							while(hasMore){
								hasMore = false;
								switch (state) {
									case READ_TYPE:{
										if(buffer.remaining()<Integer.MAX_VALUE){
											break;
										}
										type = buffer.getInt();
										if(type == Constant.PRO_RESP_GET_DATA){
											state = DecodeState.READ_GETDATA_LINENUM;
										}else if(type == Constant.PRO_RESP_GETEOF){
											state = DecodeState.READ_GETEOF;
											break;
										}else{
											throw new IOException("decode failed,illegal type = "+ type);
										}
									}
									case READ_GETDATA_LINENUM:{
										if(buffer.remaining()<Integer.MAX_VALUE){
											break;
										}
										lineNum = buffer.getInt();
										state = DecodeState.READ_GETDATA_LEN;
									}
									case READ_GETDATA_LEN:{
										if(buffer.remaining()<Integer.MAX_VALUE){
											break;
										}
										len = buffer.getInt();
										state = DecodeState.READ_GETDATA_DATA;
									}
									case READ_GETDATA_DATA:{
										if(buffer.remaining()<len){
											break;
										}
										final byte[] data = new byte[len];
										buffer.get(data);
										Utils.reverse(data);
										
										state = DecodeState.READ_TYPE;
										hasMore = true;
										
										//由于这里没有做任何异步化操作，包括dataPersistence中也没有
										//所以这里优化将new去掉，避免过多的对象分配
										row.setLineNum(lineNum);
										row.setData(data);
										dataPersistence.putRow(row);
										break;
									}
									case READ_GETEOF:{
										//收到eof，结束整个client
										isRunning = false;
										countDownLatch.countDown();
										log.info("{} receive eof.",socketChannel.socket());
										break MAIN_LOOP;
									}
									default:
										throw new IOException("decode failed,illegal state = " + state);
								}
							}
							
							buffer.compact();
						}	
					}
					
				}
				
			}catch (Exception e) {
				if(!socketChannel.socket().isClosed()){
					log.warn("{} read failed.",socketChannel.socket(),e);
				}
			}
		}
	};
	
	public void work() throws IOException{
		executorService.execute(writer);
		executorService.execute(reader);
	}
	
	public void disconnect() throws IOException{
		isRunning = false;
		if(null!=socketChannel){
			socketChannel.close();
			log.info("{} discount successed.",socketChannel.socket());
		}else{
			log.info("{} disconnect successed.");
		}
	}
	
	private static enum DecodeState{
		READ_TYPE,
		READ_GETDATA_LINENUM,
		READ_GETDATA_LEN,
		READ_GETDATA_DATA,
		READ_GETEOF
	}
	
}
