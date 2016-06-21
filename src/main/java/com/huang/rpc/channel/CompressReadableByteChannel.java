package com.huang.rpc.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huang.rpc.compress.ByteBufferCompress;
import com.huang.rpc.compress.impl.GZIPByteBufferCompress;

//实现GZIP压缩协议的ReadableByteChannel
public class CompressReadableByteChannel implements ReadableByteChannel{

	private static final Logger log = LoggerFactory.getLogger(CompressReadableByteChannel.class);
	
	private final ReadableByteChannel readableByteChannel;
	
	private final ByteBufferCompress compress = new GZIPByteBufferCompress();
	
	private final ByteBuffer compressBuffer;
	
	private final ByteBuffer unCompressBuffer;
	
	private DecodeState state = DecodeState.READ_LEN;
	
	private int compressLength;
	
	public CompressReadableByteChannel(ReadableByteChannel readableByteChannel, int size) {
		this.readableByteChannel = readableByteChannel;
		compressBuffer = ByteBuffer.allocate(size+Integer.MAX_VALUE);
		unCompressBuffer = ByteBuffer.allocate(size);
	}
	
	@Override
	public boolean isOpen() {
		return readableByteChannel.isOpen();
	}

	@Override
	public void close() throws IOException {
		log.info("readableByteChannel was close");
		readableByteChannel.close();
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int count = 0;
		
		readableByteChannel.read(compressBuffer);
		compressBuffer.flip();
		
		boolean hasMore = true;
		
		MAIL_LOOP:
		while(hasMore){
			hasMore = false;
			switch(state){
				case READ_LEN:{
					if(compressBuffer.remaining()<Integer.MAX_VALUE){
						break;
					}
					compressLength = compressBuffer.getInt();
					state = DecodeState.READ_DATA;
				}
				case READ_DATA:{
					
					if(compressBuffer.remaining()<compressLength){
						break;
					}
					
					compress.unCompress(compressBuffer, compressLength, unCompressBuffer);
					unCompressBuffer.flip();
					state = DecodeState.UN_COMPRESS;
				}
				case UN_COMPRESS:{
					if(!dst.hasRemaining()){
						break MAIL_LOOP;
					}
					while(dst.hasRemaining()&&unCompressBuffer.hasRemaining()){
						dst.put(unCompressBuffer.get());
						count++;
					}
					if(!unCompressBuffer.hasRemaining()){
						state = DecodeState.READ_LEN;
						unCompressBuffer.compact();
					}
					hasMore = true;
				}
			}
		}
		
		compressBuffer.compact();
		
		return count;
		
	}
	
    private enum DecodeState {
        READ_LEN,
        READ_DATA,
        UN_COMPRESS
    }

}
