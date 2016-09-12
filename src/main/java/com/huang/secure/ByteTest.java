package com.huang.secure;

public class ByteTest {

	
	public static void main(String[] args) {
		//三位
		String str = "黄";
		byte[] bytes = str.getBytes();
		for(byte b:bytes){
			System.out.println(b);
		}
		
	}
}
