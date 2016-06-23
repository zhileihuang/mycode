package com.huang.thread;

import java.util.concurrent.locks.ReentrantLock;

public class Uninterruptible {

	public static void main(String[] args) throws InterruptedException {

		final Object o1 = new Object();
		final Object o2 = new Object();

		final ReentrantLock l1 = new ReentrantLock();
		final ReentrantLock l2 = new ReentrantLock();

		Thread t1 = new Thread(() -> {
			try {
				synchronized (o1) {
					Thread.sleep(1000);
					synchronized (o2) {

					}
				}
			} catch (InterruptedException e) {
				System.out.println("t1 interrupted");
			}
		});

		Thread t2 = new Thread(() -> {
			try {
				synchronized (o2) {
					Thread.sleep(1000);
					synchronized (o1) {

					}
				}
			} catch (InterruptedException e) {
				System.out.println("t2 interrupted");
			}
		});

		Thread t3 = new Thread(() -> {
			try {
				l1.lockInterruptibly();
				System.out.println("t3 l1 lockInterruptibly");
				Thread.sleep(1000);
				System.out.println("t3 before l2 lockInterruptibly");
				l2.lockInterruptibly();
				System.out.println("t3 after l2 lockInterruptibly");
			} catch (InterruptedException e) {
				System.out.println("t3 interrupted");
			}
		});

		Thread t4 = new Thread(() -> {
			try {
				l2.lockInterruptibly();
				System.out.println("t4 l2 lockInterruptibly");
				Thread.sleep(1000);
				System.out.println("t4 before l1 lockInterruptibly");
				l1.lockInterruptibly();
				System.out.println("t4 after l1 lockInterruptibly");
			} catch (InterruptedException e) {
				System.out.println("t4 interrupted");
			}
		});

		// t1.start();
		//
		// t2.start();
		//
		// Thread.sleep(2000);
		//
		// t1.interrupt();
		//
		// t2.interrupt();
		//
		// t1.join();
		//
		// t2.join();

		t3.start();

		t4.start();

		Thread.sleep(2000);

		t3.interrupt();
		System.out.println("t3 start interrupt");

		t4.interrupt();
		System.out.println("t4 start interrupt");

		t3.join();

		t4.join();

	}

}
