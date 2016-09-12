package com.huang.thread;

import java.util.concurrent.TimeUnit;

public class Interrupted {
	
	public static void main(String[] args) throws Exception{
		Thread t1 = new Thread(new SleepRunner(),"s1");
		t1.setDaemon(true);
		Thread t2 = new Thread(new BusyRunner(),"s2");
		t2.setDaemon(true);
		t1.start();
		t2.start();
		TimeUnit.SECONDS.sleep(5);
		t1.interrupt();
		t2.interrupt();
		System.out.println("t1:"+t1.isInterrupted());
		System.out.println("t2:"+t2.isInterrupted());
		second(100);
	}
	
	static class SleepRunner implements Runnable{
		@Override
		public void run() {
			while(true){
				second(10);
			}
		}
	}
	
	static class BusyRunner implements Runnable{
		@Override
		public void run() {
			while(true){
				
			}
		}
	}
	
	public static final void second(long seconds){
		try{
			TimeUnit.SECONDS.sleep(seconds);
		}catch(InterruptedException e){
			System.out.println(e);
		}
	}

}
