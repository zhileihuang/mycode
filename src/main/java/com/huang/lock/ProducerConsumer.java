package com.huang.lock;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerConsumer {
	
	private static final int CAPACITY = 10;
	private final Queue<Product> queue = new LinkedList<>();
	private final Random random = new Random();
	
	private final Lock lock = new ReentrantLock();
	
	private final Condition bufferIsEmpty = lock.newCondition();
	
	private final Condition bufferIsFull = lock.newCondition();

	public void put() throws InterruptedException{
		lock.lock();
		try{
			while(queue.size()==CAPACITY){
				System.out.println(Thread.currentThread().getName()+", buffer is full,waiting");
				bufferIsFull.await();
			}
			
			int num = random.nextInt();
			boolean isAdded = queue.offer(new Product(num));
			if(isAdded){
				System.out.printf("%s added %d into queue %n",Thread.currentThread().getName(),num);
				System.out.println(Thread.currentThread().getName()+",signalling that buffer is no more empty now");
				bufferIsEmpty.signalAll();
			}else{
				System.out.println("random has inserted:"+num);
			}
		}finally{
			lock.unlock();
		}
	}

	public void get() throws InterruptedException{
		lock.lock();
		try{
			while(queue.size()==0){
				System.out.println(Thread.currentThread().getName()+", buffer is empty,waiting");
				bufferIsEmpty.await();
			}
			
			Product p = queue.poll();
			
			if(p!=null){
				System.out.printf("%s consumed %d from queue %n", Thread.currentThread().getName(),p.getId());
				System.out.println(Thread.currentThread().getName()+",signalling that buffer may be empty now");
				bufferIsFull.signalAll();
			}
			
		}finally{
			lock.unlock();
		}
	}
	
	static class Producer extends Thread{
		ProducerConsumer pc;
		
		public Producer(ProducerConsumer pc) {
			this.pc = pc;
		}
		
		@Override
		public void run() {
			try{
				while(true){
					Thread.sleep(new Random().nextInt(10000));
					pc.put();
				}
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
	
	static class Consumer extends Thread{
		ProducerConsumer pc;
		
		public Consumer(ProducerConsumer pc) {
			this.pc = pc;
		}
		
		@Override
		public void run() {
			try{
				while(true){
					Thread.sleep(new Random().nextInt(10000));
					pc.get();
				}
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
	
	static class Product{
		private final int id;

		public Product(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Product other = (Product) obj;
			if (id != other.id)
				return false;
			return true;
		}
		
	}
}
