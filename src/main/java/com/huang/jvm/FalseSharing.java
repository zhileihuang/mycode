package com.huang.jvm;

/**
 * 默认4个线程修改一数组不同元素的内容。元素的类型是volatilelong
 * 只有一个长整型成员value和6个没用到的长整型成员。value设为volatile是
 * 为了让value的修改在所有线程都可见.
 * 由于valoatilelong只有1个长整型成员，所以整个数组都将被加载至同一缓存行，但是有4个线程同时操作这条缓存行，于是伪共享就发生了。
 *
 */
public class FalseSharing implements Runnable{

	public static int NUM_THREADS = 16;
	public static final long ITERATIONS = 500L * 1000L * 1000L;
	private final int arrayIndex;
	private static VolatileLong[] longs;
	
	public FalseSharing(int arrayIndex) {
		this.arrayIndex = arrayIndex;
	}
	
	@Override
	public void run() {
		long i = ITERATIONS + 1;
		while(0!=--i){
			longs[arrayIndex].value = i;
		}
	}
	
	public static void runTest() throws InterruptedException{
		
		Thread[] threads = new Thread[NUM_THREADS];
		for(int i = 0;i<threads.length;i++){
			threads[i] = new Thread(new FalseSharing(i));
		}
		for(Thread t:threads){
			t.start();
		}
		for(Thread t: threads){
			t.join();
		}
		
	}
	
	
	public static class VolatileLong{
		public volatile long value = 0L;
//		public long p1,p2,p3,p4,p5,p6;
//		public long p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,p11,p12,p13,p14,p15,p16,p17,p18,p19,p20;
	}
	
	
	public static void main(String[] args) throws Exception {
		
		Thread.sleep(10000);
		System.out.println("starting ... ");
		
		longs = new VolatileLong[NUM_THREADS];
		for(int i = 0;i<longs.length;i++){
			longs[i] = new VolatileLong();
		}
		final long start = System.nanoTime();
		runTest();
		System.out.println("duration="+(System.nanoTime()-start));
		
	}
	

}
