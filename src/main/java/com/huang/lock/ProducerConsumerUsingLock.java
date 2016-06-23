package com.huang.lock;

import java.util.concurrent.locks.LockSupport;

import com.huang.lock.ProducerConsumer.Consumer;
import com.huang.lock.ProducerConsumer.Producer;

public class ProducerConsumerUsingLock {
	
	public static void main(String[] args) {

		ProducerConsumer pc = new ProducerConsumer();

		Producer p = new Producer(pc);
		
		Consumer c = new Consumer(pc);
		
		Consumer c2 = new Consumer(pc);
		
		p.start();
		
		c.start();
		
		c2.start();
		
		LockSupport.park();
		
	}

}
