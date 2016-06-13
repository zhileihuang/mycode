package com.huang.ref;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * 
 * 这个案例1中，首先构造MyObject对象，并将其赋值给object变量，构成强引用。然后使用SoftReference构造这个MyObject对象的软引用softRef，并注册到softQueue引用队列。当softRef被回收时，会被加入softQueue队列。设置obj=null，删除这个强引用，因此，系统内对MyObject对象的引用只剩下软引用。此时，显示调用GC，通过软引用的get()方法，取得MyObject对象的引用，发现对象并未被回收，这说明GC在内存充足的情况下，不会回收软引用对象。 
 * 接着，请求一块大的堆空间5*1024*928，这个操作会使系统堆内存使用紧张，从而产生新一轮的GC。在这次GC后，softRef.get()不再返回MyObject对象，而是返回null，说明在系统内存紧张的情况下，软引用被回收。软引用被回收时，会被加入注册的引用队列。 
 * 如果将上面案例中的数组再改大点，比如5*1024*1024，就会抛出OOM异常：
 *
 */

//-Xmx5M -XX:+PrintGCDetails
public class SoftRefTest {

	private static ReferenceQueue<MyObject> softQueue = new ReferenceQueue<>();
	
	public static class MyObject{
		
		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			System.out.println("MyObject's finalize called");
		}
		
		@Override
		public String toString() {
			return "I am MyObject";
		}
		
	}
	
	public static class CheckRefQueue implements Runnable{

		Reference<MyObject> obj = null;
		
		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			try{
				obj = (Reference<SoftRefTest.MyObject>)softQueue.remove();
			}catch(Exception e){
				e.printStackTrace();
			}
			if(obj!=null){
				System.out.println("Object for soft reference is "+obj.get());
			}
		}
		
	}
	
	public static void main(String[] args) {
		
		MyObject obj = new MyObject();
		SoftReference<MyObject> softRef = new SoftReference<SoftRefTest.MyObject>(obj, softQueue);
		new Thread(new CheckRefQueue()).start();
		
		obj = null; //删除强引用
		System.gc();
		System.out.println("After GC:Soft Get="+softRef.get());
		System.out.println("分配大块内存");
		@SuppressWarnings("unused")
		byte[] b = new byte[5*1024*715];
		System.out.println("After new byte[]:Soft Get="+softRef.get());
		System.gc();
		
	}
	
}
