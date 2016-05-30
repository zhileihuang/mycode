package com.huang.jvm;

import java.util.ArrayList;
import java.util.List;


// -Xmx20m -Xms20m -XX:NewRatio=1 -XX:+PrintGCDetails -XX:+HeapDumpOnOutOfMemoryError

/**
 *java堆用于存储对象实例，只要不断的创建对象，并且保证GC Roots到对象之间有可达路径来避免垃圾回收机制来清除这些对象，
 *那么对象数量到达最大堆容量限制后就会产生内存溢出异常 
 */

public class OutOfMemoryTest {

	public static void main(String[] args) {
		test1();
	}
	
	public static void test1(){
		List<OOMObject> list = new ArrayList<OutOfMemoryTest.OOMObject>();
		while(true){
			list.add(new OOMObject());
		}
	}
	
	public static class OOMObject{}
	
}
