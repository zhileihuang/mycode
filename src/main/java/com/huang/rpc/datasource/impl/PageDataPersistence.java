package com.huang.rpc.datasource.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.HashMap;
import java.util.Map;
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

public class PageDataPersistence implements DataPersistence{

	private static final Logger log = LoggerFactory.getLogger(PageDataPersistence.class);
	
	private final File dataFile;
	
	private static final byte[] LINE_DELIMITER = new byte[]{'\r','\n'};
	
	//一行数据构成：有效字节数(4B)+数据段(210B)+填充段(42B) = 256B
	private final int PAGE_ROW_SIZE = 256;
	
	//页行数,一页中总共有几行
	private final int PAGE_ROWS_NUM = 10000;
	
	//页码表大小，一共有几页
	private final int PAGE_TABLE_SIZE = 48;
	
	//页码表
	private Page[] pageTable = new Page[PAGE_TABLE_SIZE];
	
	//等待刷新缓存集合
	private Map<Integer,MappedByteBuffer> waitingFlushBufferMap = new HashMap<>();
	
	private FileChannel fileChannel;
	
	private final ReentrantLock pageSwitchLock = new ReentrantLock();
	
	//页面切换锁
	private final Condition pageSwitchWakeUpConditon = pageSwitchLock.newCondition();
	
	//页面切换者完成标记
	private final CountDownLatch pageSwitchDone = new CountDownLatch(1);
	
	//持久化数据源是否被刷新
	private volatile boolean isFlushFlag = false;
	
	public PageDataPersistence(File dataFile) {
		this.dataFile = dataFile;
	}
	
	@Override
	public void putRow(Row row) throws IOException {
		final int lineNum = row.getLineNum();
		//计算页码
		final int pageNum = lineNum/PAGE_ROWS_NUM;
		
		//计算页码表位置
		final int tableIdx = pageNum%PAGE_TABLE_SIZE;
	
		while(pageTable[tableIdx].pageNum!=pageNum){
			
		}
		
		final Page page = pageTable[tableIdx];
		
		//计算页面内行号
		final int rowNum = lineNum%PAGE_ROWS_NUM;
	
		final byte[] bytesOfLineNum = String.valueOf(lineNum).getBytes();
		
		//计算row中有效大小(B)
		final int validByteCount = 0 +bytesOfLineNum.length+row.getData().length+LINE_DELIMITER.length;
	
		final int offset = rowNum * PAGE_ROW_SIZE;
		
		ByteBuffer.wrap(page.data,offset,PAGE_ROW_SIZE)
		          .putInt(validByteCount)
		          .put(bytesOfLineNum)
		          .put(row.getData())
		          .put(LINE_DELIMITER);
		
		//更新页面数量
		page.byteCount.addAndGet(validByteCount);
		
		//如果页面已被写满，则需要唤醒页面切换者
		if(page.rowCount.incrementAndGet()==PAGE_ROWS_NUM){
			pageSwitchLock.lock();
			try{
				pageSwitchWakeUpConditon.signal();
			}finally{
				pageSwitchLock.unlock();
			}
		}
		
	}

	@SuppressWarnings("resource")
	@Override
	public void init() throws IOException {
		if(!dataFile.exists()){
			try{
				dataFile.createNewFile();
			}catch(IOException e){
				log.warn("create dataFile={} failed.",dataFile,e);
			}
		}
		
		//打开文件句柄
		fileChannel = new RandomAccessFile(dataFile, "rw").getChannel();
	
		//初始化页码表
		for(int i = 0;i<pageTable.length;i++){
			final Page page = new Page();
			page.pageNum = i;
			pageTable[i] = page;
		}
		
		final Thread pageSwitcher = new Thread(()->{
			
			//下一次要替换掉的页码表编号(0~PAGE_TABLE_SIZE)
			int nextSwitchPageTableIndex = 0;
			
			//文件写入偏移量
			long fileOffset = 0;
			
			while(true){
				
				//遍历页码表，主要做两件事
				//1、顺序的更换页码
				//2、将页码刷入文件缓存
				
				final Page page = pageTable[nextSwitchPageTableIndex];
				final int rowCount = page.rowCount.get();
				
				if(rowCount<PAGE_ROWS_NUM&&!isFlushFlag){
					//当前页还没被锁定且不是刷新状态，休眠等待被唤醒
					
					pageSwitchLock.lock();
					try{
						pageSwitchWakeUpConditon.await();
						continue;
					}catch(InterruptedException e){
						Thread.currentThread().interrupt();
					}finally{
						pageSwitchLock.unlock();
					}
				}
				
				if(page.rowCount.get()==PAGE_ROWS_NUM||(isFlushFlag&&rowCount>0)){
					
					//当前页面已被写需要立即刷入文件缓存中
					try{
						//写完文件缓存后丢入待刷新队列中
						final MappedByteBuffer mappedBuffer = fileChannel.map(MapMode.READ_WRITE, fileOffset, page.byteCount.get());
						final ByteBuffer dataBuffer = ByteBuffer.wrap(page.data);
						
						for(int rowIdx = 0;rowIdx<rowCount;rowIdx++){
							//当前行偏移量
							final int offsetOfRow = rowIdx*PAGE_ROW_SIZE;
							dataBuffer.position(offsetOfRow);
							final int validByteCount = dataBuffer.getInt();
							
							//当前行数据偏移量
							final int offsetOfRowData = offsetOfRow+Integer.BYTES;
							mappedBuffer.put(page.data,offsetOfRowData,validByteCount);
						}
						waitingFlushBufferMap.put(page.pageNum, mappedBuffer);
						
						//重设当前页码数据
						page.byteCount.set(0);
						page.rowCount.set(0);
						page.pageNum+=PAGE_TABLE_SIZE;
						fileOffset +=mappedBuffer.capacity();
						
					}catch(IOException e){
						log.warn("mapping file failed.",e);
						continue;
					}
					
					//最后一步，更新下一次要替换的页码表号
					nextSwitchPageTableIndex = (nextSwitchPageTableIndex+1) % PAGE_TABLE_SIZE;
					continue;
				}
				
				if(isFlushFlag&&rowCount == 0){
					break;
				}
				
			}
			pageSwitchDone.countDown();
			
		},"PageDataPersistence");
		pageSwitcher.setDaemon(true);
		pageSwitcher.start();
		log.info("PageDataPersistence(file:{}) was inited.",dataFile);
	}

	@Override
	public void flush() throws IOException {
		//标记数据源为刷新状态
		isFlushFlag = true;
		pageSwitchLock.lock();
		try{
			//唤醒页面切换者
			pageSwitchWakeUpConditon.signal();
		}finally{
			pageSwitchLock.unlock();
		}
		
		try{
			//等待页面切换者将所有页码表中数据都刷入文件缓存中
			pageSwitchDone.await();
		}catch(InterruptedException e){
			waitingFlushBufferMap.forEach((k,v)->v.force());
			log.info("PageDataPersistence(file:{}) was flushed.",dataFile);
		}
	}

	@Override
	public void destroy() throws IOException {
		waitingFlushBufferMap.forEach((k,v)->Utils.unmap(v));
		if(null!=fileChannel){
			fileChannel.close();
		}
		log.info("PageDataPersistence(file:{}) was destroyed.",dataFile);
	}
	
	
	//缓存页
	//一页有10^6行
	class Page{
		volatile int pageNum;
		AtomicInteger rowCount = new AtomicInteger(0);
		AtomicLong byteCount = new AtomicLong(0);
		byte[] data = new byte[PAGE_ROW_SIZE*PAGE_ROWS_NUM];
	}

}
