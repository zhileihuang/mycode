package com.huang.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ScheduledExecutorServiceTest {
	
	public static void main(String[] args) {
		
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		
		executor.schedule(new Runnable() {
			@Override
			public void run() {
				System.out.println("60 seconds later");
			}
		}, 60, TimeUnit.SECONDS);
		
		LockSupport.park();
		
	}

}
