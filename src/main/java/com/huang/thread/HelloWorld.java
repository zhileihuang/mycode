package com.huang.thread;

public class HelloWorld {
	
	public static void main(String[] args) throws InterruptedException{
		
		Thread t = new Thread(()->{
			System.out.println("Hello from new Thread");
		});
		t.start();
		Thread.yield();
		
		System.out.println("Hello from main thread");
		t.join();
		
	}

}
