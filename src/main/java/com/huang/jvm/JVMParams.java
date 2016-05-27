package com.huang.jvm;

import java.util.concurrent.locks.LockSupport;


/**
 *-Xmx20m -Xms5m 
 *最大堆，最小堆
 *-Xmn 设置新生代大小
 *
 *-XX:NewRatio 
 *    新生代(eden+2*s) 和老年代(不包含永久区)的比值
 *
 *-XX:SurvivorRatio
 *    设置两个Survivor区和eden的比
 */
@SuppressWarnings("unused")
public class JVMParams {
	
	public static void main(String[] args) throws Exception {
		
//		test1();
//		test2();
		test3();
		
	}
	
	/**
	 *  分配了1M空间给数组
	 *  Xmx=18.0M
     *  free mem=3.4999847412109375M
     *  total mem=5.5M
	 */
	public static void test1(){
		byte[] b = new byte[1*1024*1024];
		System.out.println("分配了1M空间给数组");
		print();
	}
	
	/**
	 *  分配了4M空间给数组
	 *  Xmx=18.0M
     *  free mem=4.9999847412109375M
     *  total mem=10.0M
	 */

	public static void test2(){
		byte[] b = new byte[4*1024*1024];
		System.out.println("分配了4M空间给数组");
		print();
	}
	
	public static void test3() throws Exception{
		byte[] b = new byte[4*1024*1024];
		System.out.println("分配了4M空间给数组");
		print();
		
		b=null;
		System.gc();
		System.out.println("gc");
		print();
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

}
