package com.huang.jvm;

import java.util.Vector;

// -Xmx20m -Xms5m
public class OutOfMemoryTest {

	
	public static void main(String[] args) {
		
		Vector<byte[]> v = new Vector<>();
 		for(int i = 0;i<25;i++){
 			byte[] b = new byte[1*1024*1024];
 			v.add(b);
 		}
 		
	}
	
}
