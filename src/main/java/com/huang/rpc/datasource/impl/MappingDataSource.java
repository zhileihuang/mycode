package com.huang.rpc.datasource.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huang.rpc.datasource.DataSource;
import com.huang.rpc.datasource.Row;
import com.huang.rpc.util.Utils;

public class MappingDataSource implements DataSource {

	private static final Logger log = LoggerFactory.getLogger(MappingDataSource.class);

	private static final int BUFFER_SIZE = 512 * 1024 * 1024;

	private final File dataFile;

	//先进先出队列
	private final ConcurrentLinkedQueue<Row> rowQueue = new ConcurrentLinkedQueue<>();

	public MappingDataSource(File dataFile) {
		this.dataFile = dataFile;
	}

	@Override
	public Row getRow(Row row) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Row getRow() throws IOException {
		final Row row = rowQueue.poll();
		if (null == row) {
			// 结束
			return new Row(-1, new byte[0]);
		}
		return row;
	}

	@SuppressWarnings("resource")
	@Override
	public void init() throws IOException {
		final long startTime = System.currentTimeMillis();
		int lineCounter = 0;
		try(final FileChannel fileChannel = new RandomAccessFile(dataFile, "r").getChannel()){
			long pos = 0;
			final ByteBuffer dataBuffer = ByteBuffer.allocate(1024);
			final long fileSize = fileChannel.size();
			while(pos<fileSize){
				final long loadStartTime = System.currentTimeMillis();
				// TODO MappedByteBuffer
				final MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY,pos,fixBufferSize(pos, fileSize)).load();
				final long loadEndTime = System.currentTimeMillis();
				log.info("[datasource] loading...,pos={},size={},cost={}",new Object[]{pos,buffer.capacity(),(loadEndTime - loadStartTime)});
				DecodeLineState state = DecodeLineState.READ_D;
				while(buffer.hasRemaining()){
					switch (state) {
						case READ_D:{
							final byte b = buffer.get();
							log.info("read_d:"+b+",charValue:"+(char)b+",position:"+buffer.position());
							if(b == '\r'){
								state = DecodeLineState.READ_R;
							}else{
								dataBuffer.put(b);
								break;
							}
						}
						case READ_R:{
							final byte b = buffer.get();
							log.info("read_d:"+b+",charValue:"+(char)b+"position:"+buffer.position());
							if(b!='\n'){
								throw new IOException("illegal format, \\n did not behind \\r, b=" + b);
							}
							state = DecodeLineState.READ_N;
						}
						case READ_N:{
							state = DecodeLineState.READ_D;
							dataBuffer.flip();
							final byte[] data = new byte[dataBuffer.limit()];
							dataBuffer.get(data);
							final Row row = new Row(lineCounter++, data);
							rowQueue.offer(row);
							dataBuffer.clear();
							break;
						}
						default:
							throw new IOException("init failed,illegal state="+state);
					}
				}
				pos+=buffer.capacity();
				Utils.unmap(buffer);
				Runtime.getRuntime().gc();
			}
		}//try
		final long endTime = System.currentTimeMillis();
		log.info("[datasource] was inited,cost={}",(endTime - startTime));
	}

	@Override
	public void destroy() throws IOException {
		rowQueue.clear();
		log.info("[datasource] was destroyed.");
	}

	private enum DecodeLineState {
		READ_D, // 读取数据
		READ_R, // 读取\r
		READ_N, // 读取\n
	}

	private long fixBufferSize(long pos, long fileSize) {
		if (pos + BUFFER_SIZE >= fileSize) {
			return fileSize - pos;
		} else {
			return BUFFER_SIZE;
		}
	}

}
