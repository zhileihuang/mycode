package com.huang.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class WeakHashMapTest {

	public static void test1() {
		byte[][] key = null;
		WeakHashMap<byte[][], byte[][]> maps = new WeakHashMap<byte[][], byte[][]>();
		for (int i = 0; i < 10000; i++) {
			key = new byte[1000][1000];
			maps.put(key, new byte[1000][1000]);
//			 System.gc();
			System.err.println(i+",size:"+maps.size());
		}
	}

	public static void test2() {
		List<byte[][]> keys = new ArrayList<byte[][]>();
		byte[][] key = null;
		WeakHashMap<byte[][], byte[][]> maps = new WeakHashMap<byte[][], byte[][]>();
		for (int i = 0; i < 10000; i++) {
			key = new byte[1000][1000];
			keys.add(key);
			maps.put(key, new byte[1000][1000]);
			// System.gc();
			System.err.println(i+",size:"+maps.size());
		}
	}
	
	public static void test3() {
		byte[][] key = null;
		Map<byte[][], byte[][]> maps = new SoftHashMap<byte[][], byte[][]>();
		for (int i = 0; i < 10000; i++) {
			key = new byte[1000][1000];
			maps.put(key, new byte[1000][1000]);
//			 System.gc();
			System.err.println(i+",size:"+maps.size());
		}
	}
	
	public static void test4() {
		List<byte[][]> keys = new ArrayList<byte[][]>();
		byte[][] key = null;
		Map<byte[][], byte[][]> maps = new SoftHashMap<byte[][], byte[][]>();
		for (int i = 0; i < 10000; i++) {
			key = new byte[1000][1000];
			keys.add(key);
			maps.put(key, new byte[1000][1000]);
			// System.gc();
			System.err.println(i+",size:"+maps.size());
		}
	}

	public static void main(String[] args) {

		test1();

//		test2();
		
//		test3();
		
//		test4();
		
		
	}

}
