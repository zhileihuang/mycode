package com.huang.ref;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.TimeUnit;

/**
 * 经过一次GC之后，系统找到了垃圾对象，并调用finalize()方法回收内存，但没有立即加入回收队列。
 * 第二次GC时，该对象真正被GC清楚，此时，加入虚引用队列
 * 虚引用的最大作用在于跟踪对象回收，清理被销毁对象的相关资源。 
 *
 */
public class PhantomRefTest {

	private static ReferenceQueue<MyObject> phanQueue = new ReferenceQueue<>();

	public static class MyObject {

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

	public static class CheckRefQueue implements Runnable {
		Reference<MyObject> obj = null;

		@Override
		public void run() {
			try {
				obj = (Reference<MyObject>) phanQueue.remove();
				System.out.println("删除的虚引用为：" + obj + "  but获取虚引用的对象obj.get()=" + obj.get());
				System.exit(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws InterruptedException {
		MyObject object = new MyObject();
		Reference<MyObject> phanRef = new PhantomReference<>(object, phanQueue);
		System.out.println("创建的虚引用为：" + phanRef);
		new Thread(new CheckRefQueue()).start();

		object = null;
		TimeUnit.SECONDS.sleep(1);
		int i = 1;
		while (true) {
			System.out.println("第" + i++ + "次gc");
			System.gc();
			TimeUnit.SECONDS.sleep(1);
		}
	}

}
