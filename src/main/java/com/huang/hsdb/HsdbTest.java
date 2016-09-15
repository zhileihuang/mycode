package com.huang.hsdb;


//这个程序的t1、t2、t3三个变量本身（而不是这三个变量所指向的对象）到底在哪里。
//t1在存Java静态变量的地方，概念上在JVM的方法区（method area）里
//t2在Java堆里，作为Test的一个实例的字段存在
//t3在Java线程的调用栈里，作为Test.fn()的一个局部变量存在
public class HsdbTest {

	static Test2 t1 = new Test2();
	Test2 t2 = new Test2();
	
	public void fn(){
		
		Test2 t3 = new Test2();
		
	}
	
}

class Test2{
	
}
