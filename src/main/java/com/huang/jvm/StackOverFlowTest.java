package com.huang.jvm;

// -Xms20m -Xmx20m -Xss128k -XX:+PrintGCDetails
/**
 *操作系统分配给每个线程的内存是有限的 
 *
 */
public class StackOverFlowTest {

	private int stackLength = 1;
	
	public void stackLeak(){
		stackLength++;
		stackLeak();
	}
	
	public static void main(String[] args) {
		
		StackOverFlowTest test = new StackOverFlowTest();
		try{
			test.stackLeak();
		}catch(Throwable e){
			System.out.println("stack lenght:"+test.stackLength);
			throw e;
		}
		
	}
	
}
