package com.huang.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class LockTypeTest {
	
	public static void main(String[] args) {
		
		new Thread(new Blocked(),"BlockedThread-1").start();
		new Thread(new Blocked(),"BlockedThread-2").start();
		
		ReentrantLock lock = new ReentrantLock();
		new Thread(new Locked(lock),"lock-1").start();
		new Thread(new Locked(lock),"lock-2").start();
		
		LockSupport.park();
	}
	
	static class Blocked implements Runnable{
		
		@Override
		public void run() {
			synchronized (Blocked.class) {
				while(true){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	static class Locked implements Runnable{

		private Lock lock;
		
		public Locked(Lock lock) {
			this.lock = lock;
		}


		@Override
		public void run() {
			while(true){
				lock.lock();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				lock.unlock();
			}
		}
		
	}

}
