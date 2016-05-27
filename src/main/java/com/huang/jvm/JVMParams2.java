package com.huang.jvm;

import java.util.concurrent.locks.LockSupport;

//-Xmx20m 
//-Xms20m 
//-XX:NewRatio=1
//-XX:+PrintGCDetails


/**
 *-Xmx20m -Xms20m -Xmn5m -XX:+PrintGCDetails 
 *-XX:+TraceClassLoading 跟踪类加载
 *-XX:+PrintClassHistogram 打印类的信息
 *
 */
@SuppressWarnings("unused")
public class JVMParams2 {
	
	public static void main(String[] args) throws Exception {
		
		System.out.println("start");
		
//		test1();
		
		System.out.println();
		
	}
	
	/**
	 * 
	 */
	public static void test1() throws Exception{
		byte[] b = null;
		for(int i = 0;i<10;i++){
			System.out.println("current:"+i);
			Thread.sleep(1000);
			b = new byte[3*1024*1024];
		}
//		print();
	}
	
	private static void print(){
		//最大JVM内存
		System.out.print("Xmx=");
		System.out.println(Runtime.getRuntime().maxMemory()/1024.0/1024+"M");
		
		//剩余内存空间
		System.out.print("free mem=");
		System.out.println(Runtime.getRuntime().freeMemory()/1024.0/1024+"M");
		
		//JVM可支配的最大内存值
		System.out.print("total mem=");
		System.out.println(Runtime.getRuntime().totalMemory()/1024.0/1024+"M");
	}
	
	// 6144+512 = 6656
    // 6656 + 13824 = 
	
	
}
