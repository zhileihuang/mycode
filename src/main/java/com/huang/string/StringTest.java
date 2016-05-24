package com.huang.string;

/*
 * String的String Pool是一个固定大小的 Hashtable ，默认值大小长度是1009，
 * 如果放进String Pool的String非常多，就会造成 Hash 冲突严重，从而导致链表会很长，
 * 而链表长了后直接会造成的影响就是当调用 String.intern 时性能会大幅下降（因为要一个一个找）
 * 
 */
public class StringTest {
	
	public static void main(String[] args) {
		
		String s = new String("abc");
		String s2 = "abc";
		s.intern();
		System.out.println(s == s2);
		
		String s3 = new String("ab")+ new String("c");
		String s4 = "abc";
		s3.intern();
		System.out.println(s3 == s4);
		
	}

}
