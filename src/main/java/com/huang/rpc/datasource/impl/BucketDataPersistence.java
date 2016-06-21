package com.huang.rpc.datasource.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huang.rpc.datasource.DataPersistence;
import com.huang.rpc.datasource.Row;
import com.huang.rpc.util.Utils;

public class BucketDataPersistence implements DataPersistence{

	private static final Logger log = LoggerFactory.getLogger(BucketDataPersistence.class);
	
	private static final int BUCKET_ROWS_SIZE = 1024*512; // 每个数据桶大小
	
	private static final int BUCKET_SIZE = 20; //桶总数
	
	private final File dataFile;
	
	private final Bucket[] buckets = new Bucket[BUCKET_SIZE];
	
	private final ReentrantLock writeLock = new ReentrantLock();
	private final Condition writeCondition = writeLock.newCondition();
	
	private boolean isFlushed = false;//是否被标记为finished，如果被标记则需要强刷最后一个未完成的桶
	private final CountDownLatch finishCountDown = new CountDownLatch(1);
	private final List<MappedByteBuffer> waitForFlushBuffers = new ArrayList<>();
	
	private static final byte[] ENDS = new byte[]{'\r','\n'};
	
	private final Runnable writer = new Runnable() {
		
		@Override
		public void run() {
			//上次完成的桶编号
			int lastFinishedBucketIndex = -1;
			//累计写入的文件大小
			long pos = 0;
			//检查文件是否存在，不存在则创建
			if(!dataFile.exists()){
				try{
					dataFile.createNewFile();
				}catch(IOException e){
					log.warn("create dataFile = {} failed.",dataFile,e);
				}
			}
			
			try(final FileChannel fileChannel = new RandomAccessFile(dataFile, "rw").getChannel()){
				while(!isFlushed){
					writeLock.lock();
					try{
						writeCondition.await();
					}catch(Exception e){
						
					}finally{
						writeLock.unlock();
					}
					for(int i = lastFinishedBucketIndex+1;i<BUCKET_SIZE;i++){
						final Bucket bucket = buckets[i];
						if(bucket.isFinished){
							lastFinishedBucketIndex = i;
							continue;
						}
						
						if(bucket.isFull()||(isFlushed&&!bucket.isEmpty())){
							final long byteCount = bucket.byteCount.get();
							final int rowCount = bucket.rowCount.get();
							final Row[] rows = bucket.rows;
							final MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, pos, byteCount);
							
							for(int rowIdx = 0;rowIdx<rowCount;rowIdx++){
								final Row row = rows[rowIdx];
								if(null == buffer){
									log.info("debug for null,pos={},byteCount={}",pos,byteCount);
								}
								if(null == row){
									log.info("debug for null,rowIdx={},bucket.rowCount={}",rowIdx,bucket.rowCount);
								}
								
								buffer.put(String.valueOf(row.getLineNum()).getBytes());
								buffer.put(row.getData());
								buffer.put(ENDS);
							}
							waitForFlushBuffers.add(buffer);
							pos+=byteCount;
							lastFinishedBucketIndex = i;
							
							//刷完一个桶则释放一个
							buckets[i] = null;
							
							log.info("bucket[{}] was finished,size={}",i,byteCount);
							
						}else{
							//只要顺序上有一个桶未被填满则立即中断循环，等待下次唤醒
							break;
						}
					}
				}
				finishCountDown.countDown();
			}catch (Exception e) {
				log.warn("bucket data persistence-write write failed.",e);
			}
			
		}
	};
	
	public BucketDataPersistence(File dataFile) {
		this.dataFile = dataFile;
	}
	
	@Override
	public void putRow(Row row) throws IOException {
		final int bIdx = row.getLineNum()/BUCKET_ROWS_SIZE;
		final int cIdx = row.getLineNum()%BUCKET_ROWS_SIZE;
		final Bucket bucket = buckets[bIdx];
		bucket.rows[cIdx] = row;
		bucket.rowCount.incrementAndGet();
		
		final byte[] lineNumBytes = String.valueOf(row.getLineNum()).getBytes();
		bucket.byteCount.addAndGet(lineNumBytes.length+row.getData().length+2);
		
		//如果桶被装满了，则需要唤醒写入线程
		if(bucket.isFull()){
			writeLock.lock();
			try{
				writeCondition.signal();
			}finally{
				writeLock.unlock();
			}
		}
	}

	@Override
	public void init() throws IOException {
		
		for(int index = 0;index<buckets.length;index++){
			buckets[index] = new Bucket();
		}
		
		//初始化写入线程
		new Thread(writer,"BucketDataPersistence-write").start();
		
		log.info("bucketDataPersistence(file:{}) was inited",dataFile);
		
	}

	@Override
	public void flush() throws IOException {
		isFlushed = true;
		writeLock.lock();
		try{
			writeCondition.signal();
		}finally{
			writeLock.unlock();
		}
		
		try{
			finishCountDown.await();
		}catch(InterruptedException e){
			
		}
		
		for(MappedByteBuffer buffer:waitForFlushBuffers){
			buffer.force();
			Utils.unmap(buffer);
		}
		
		log.info("bucketDataPersistence({file:{}}) was flushed.",dataFile);
		
	}

	@Override
	public void destroy() throws IOException {
		log.info("bucketDataPersistence(file:{}) was destroyed.",dataFile);
	}
	
	
	private class Bucket{
		private Row[] rows = new Row[BUCKET_ROWS_SIZE]; //桶中数据
		private AtomicLong byteCount = new AtomicLong(0);//桶字节大小
		private volatile boolean isFinished = false; //标记为已完成，被writer线程写入文件
		private AtomicInteger rowCount = new AtomicInteger(0);//桶中数据个数
		
		//判断桶是否已经装满
		public boolean isFull(){
			return rowCount.get() == BUCKET_ROWS_SIZE;
		}
		
		//判断桶是否从未装过数据
		public boolean isEmpty(){
			return rowCount.get() == 0;
		}
		
	}

}
