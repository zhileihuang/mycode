package com.huang.rpc;

import java.nio.channels.SelectionKey;

import org.junit.Test;

public class ByteTest {
	
	@Test
	public void test1(){
		byte r = '\r';
		System.out.println(r);
		byte n = '\n';
		System.out.println(n);
		byte t = '\t';
		System.out.println(t);
	}
	
	@Test
	public void test2(){
		System.out.println(SelectionKey.OP_WRITE); //4
		System.out.println(SelectionKey.OP_READ);//1
		System.out.println(SelectionKey.OP_WRITE&~SelectionKey.OP_WRITE); //0
		System.out.println(SelectionKey.OP_READ&~SelectionKey.OP_WRITE); //1
	}

}
