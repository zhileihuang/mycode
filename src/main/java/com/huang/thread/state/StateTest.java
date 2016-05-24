package com.huang.thread.state;

import java.util.concurrent.locks.LockSupport;

import org.junit.Test;

/**
 * 
 */
public class StateTest {
	
	@Test
	public void newState(){
		Thread t = new Thread();
		System.out.println(t.getState());
	}
	
	
	@Test
	public void runnableState(){
		Thread t = new Thread(){
			public void run() {
				for(int i = 0;i<Integer.MAX_VALUE;i++){
					System.out.println(i);
				}
			};
		};
		t.start();
		LockSupport.park();
	}
	
	@Test
	public void blockedState(){
		
		final Object lock = new Object();
		Runnable run = new Runnable() {
			@Override
			public void run() {
				for(int i = 0;i<Integer.MAX_VALUE;i++){
					synchronized (lock) {
						System.out.println(i);
					}
				}
			}
		};
		
		Thread t1 = new Thread(run);
		t1.setName("t1");
		Thread t2 = new Thread(run);
		t2.setName("t2");
		
		t1.start();
		t2.start();
		
		LockSupport.park();
		
	}
	
	@Test
	public void waitingState(){
		
		final Object lock = new Object();
		
		Thread t1 = new Thread(){
			@Override
			public void run() {
				
				int i = 0;
				
				while(true){
					synchronized (lock) {
						try {
							lock.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						System.out.println(i++);
					}
				}
			}
		};
		
		Thread t2 = new Thread(){
			@Override
			public void run() {
				
				while(true){
					synchronized (lock) {
						for(int i = 0;i<Integer.MAX_VALUE;i++){
							System.out.println(i);
						}
						lock.notifyAll();
					}
				}
				
			}
		};
		
		t1.setName("t1");
		t2.setName("t2");
		
		t1.start();
		t2.start();
		
		LockSupport.park();
		
		
	}
	
	
	@Test
	public void timeWaitingState(){
		
		final Object lock = new Object();
		
		Thread t1 = new Thread(){
			@Override
			public void run() {
				
				int i = 0;
				
				while(true){
					synchronized (lock) {
						try {
							lock.wait(60*10000L);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						System.out.println(i++);
					}
				}
			}
		};
		
		Thread t2 = new Thread(){
			@Override
			public void run() {
				
				while(true){
					synchronized (lock) {
//						for(int i = 0;i<Integer.MAX_VALUE;i++){
//							System.out.println(i);
//						}
						try {
							sleep(30*10000L);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						lock.notifyAll();
					}
				}
				
			}
		};
		
		t1.setName("t1");
		t2.setName("t2");
		
		t1.start();
		t2.start();
		
		LockSupport.park();
		
	}
}
