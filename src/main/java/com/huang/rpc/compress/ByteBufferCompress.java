package com.huang.rpc.compress;

import java.io.IOException;
import java.nio.ByteBuffer;

//压缩
public interface ByteBufferCompress {
	
	public void compress(ByteBuffer src,int len,ByteBuffer dst) throws IOException;
	
	public void unCompress(ByteBuffer src,int len,ByteBuffer dst) throws IOException;

}
