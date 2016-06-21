package com.huang.rpc.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import com.huang.rpc.compress.ByteBufferCompress;
import com.huang.rpc.compress.impl.GZIPByteBufferCompress;

public class CompressWritableByteChannel implements WritableByteChannel{

	private final WritableByteChannel writableByteChannel;
	
	private final ByteBuffer compressBuffer;
	private final ByteBuffer unCompressBuffer;
	
	private final ByteBufferCompress compress = new GZIPByteBufferCompress();
	
	private DecodeState state = DecodeState.READ;
	
	public CompressWritableByteChannel(WritableByteChannel writableByteChannel,int size) {
		this.writableByteChannel = writableByteChannel;
		this.compressBuffer = ByteBuffer.allocate(size+Integer.MAX_VALUE);
		this.unCompressBuffer = ByteBuffer.allocate(size);
	}
	
	@Override
	public boolean isOpen() {
		return writableByteChannel.isOpen();
	}

	@Override
	public void close() throws IOException {
		writableByteChannel.close();
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return write(src, false);
	}
	
	public int write(ByteBuffer src,boolean immediately) throws IOException {
		int count = 0;
		boolean hasMore = true;
		while(hasMore){
			hasMore = false;
			switch (state) {
				case READ:{
					if(!src.hasRemaining()){
						break;
					}
					
					while(src.hasRemaining()&&unCompressBuffer.hasRemaining()){
						unCompressBuffer.put(src.get());
						count++;
					}
					
					if(!unCompressBuffer.hasRemaining()||immediately){
						unCompressBuffer.flip();
						state = DecodeState.COMPRESS;
					}
					break;
				}
				case COMPRESS:{
					compress.compress(unCompressBuffer, unCompressBuffer.limit(), compressBuffer);
					unCompressBuffer.compact();
					compressBuffer.flip();
					state = DecodeState.WRITE_DATA;
				}
				case WRITE_DATA:{
					writableByteChannel.write(compressBuffer);
					if(!compressBuffer.hasRemaining()){
						compressBuffer.compact();
						state = DecodeState.READ;
					}
					hasMore = true;
				}
			}
		}
		return count;
	}
	
	enum DecodeState {
        READ,
        COMPRESS,
        WRITE_DATA
    }

}
