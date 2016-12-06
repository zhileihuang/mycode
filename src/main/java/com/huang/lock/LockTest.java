package com.huang.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

//waitStatus 当前节点的等待状态，有四个值
//cancel:1 表示该节点对应的线程没有被正常唤醒，比如：tryLock(long, TimeUnit)调用超时。
//signal:-1 表示该节点的线程在归还锁的时候，需要对下一个节点中的线程进行unpark操作。
//condition:-2 表示该节点当前阻塞在了一个condition队列中(后边讲condition的时候会讲到)
//propagate:-3 主要用于共享模式

public class LockTest {
	

	
	public static void main(String[] args) {
		
		Lock lock = new ReentrantLock();
		
		Thread t1 = new Thread(new Worker(lock),"worker1-lock");
		Thread t2 = new Thread(new Worker(lock),"worker2-lock");
		Thread t3 = new Thread(new Worker(lock),"worker3-lock");
		Thread t4 = new Thread(new Worker2(lock),"worker4-lockInterruptibly");
		Thread t5 = new Thread(new Worker3(lock),"worker5-tryLock");
		Thread t6 = new Thread(new Worker4(lock),"worker6-tryLock-time");
		
		t1.start();
		t2.start();
		t3.start();
		t4.start();
		t5.start();
		t6.start();
		
		System.out.println("hello world");
		
		t4.interrupt();
		
		
		LockSupport.park();
	}
	
	static class Worker implements Runnable{
		
		private Lock lock;
		
		public Worker(Lock lock) {
			this.lock = lock;
		}
		
		@Override
		public void run() {
			
			lock.lock();
			
			System.out.println("hello");
			
			lock.unlock();
			
		}
		
	}
	
	static class Worker2 implements Runnable{
		
		private Lock lock;
		
		public Worker2(Lock lock) {
			this.lock = lock;
		}
		
		@Override
		public void run() {
			
			try {
				lock.lockInterruptibly();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			System.out.println("hello");
			
			lock.unlock();
			
		}
		
	}
	
	static class Worker3 implements Runnable{
		
		private Lock lock;
		
		public Worker3(Lock lock) {
			this.lock = lock;
		}
		
		@Override
		public void run() {
			
			lock.tryLock();
			
			System.out.println("hello");
			
			lock.unlock();
			
		}
		
	}
	
	static class Worker4 implements Runnable{
		
		private Lock lock;
		
		public Worker4(Lock lock) {
			this.lock = lock;
		}
		
		@Override
		public void run() {
			
			try {
				lock.tryLock(1000, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			System.out.println("hello");
			
			lock.unlock();
			
		}
		
	}

}
