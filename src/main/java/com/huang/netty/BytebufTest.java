package com.huang.netty;

import java.nio.ByteBuffer;

import org.junit.Test;

public class BytebufTest {
	
	@Test
	public void test1(){
		
		ByteBuffer buf = ByteBuffer.allocate(100);
		buf.putLong(100);
		buf.put((byte)1);
		buf.flip();
		
		buf.compact();
		
		long longValue = buf.getLong();
		System.out.println(longValue);
		byte byteValue = buf.get();
		System.out.println(byteValue);
		
	}
	
	@Test
	public void test2(){
		
		ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
		buffer.putChar('a');
		System.out.println(buffer);
		buffer.putChar('c');
		System.out.println(buffer);
		buffer.putInt(10);
		System.out.println(buffer);
		
	}
	
	@Test
	public void test3(){
		
		ByteBuffer buffer = ByteBuffer.allocateDirect(10);
		buffer.put(new byte[]{1,2,3,4});
		System.out.println("刚写完数据:"+buffer);
		buffer.flip();
		System.out.println("flip之后:"+buffer);
		byte[] target = new byte[buffer.limit()];
		buffer.get(target);
		for(byte b: target){
			System.out.println(b);
		}
		System.out.println("读取完数组:"+buffer);
		
	}
	
	@Test
	public void test4(){
		ByteBuffer buffer = ByteBuffer.allocateDirect(20);
		buffer.put(new byte[]{1,2,3,4,5,6,7,8,9});
		System.out.println("刚写完数据:"+buffer);
		buffer.flip();
		System.out.println("flip之后:"+buffer);
		byte[] target = new byte[2];
		buffer.get(target);
		for(byte b: target){
			System.out.println(b);
		}
		System.out.println("before compact:"+buffer);
		buffer.compact();
		//compact 后，pos 指向当前可用的下一个位置。因为移动的是7个数据，所以pos为7
		System.out.println("after compact:"+buffer);
		buffer.flip();
		System.out.println("after flip:"+buffer);
		target = new byte[buffer.limit()];
		buffer.get(target);
		for(byte b: target){
			System.out.println(b);
		}
	}

}
