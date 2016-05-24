package com.huang.jvm;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Count {
	
	public int count = 0;
	
    static class Job implements Runnable{
        private CountDownLatch countDown;
        private Count count;
        public Job(Count count,CountDownLatch countDown){
            this.count = count;
            this.countDown = countDown;
        }
        @Override
        public void run() {
            count.count++;
            countDown.countDown();
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDown = new CountDownLatch(1500);
        Count count = new Count();
        ExecutorService ex = Executors.newFixedThreadPool(5);
        for(int i = 0; i < 1500; i ++){
            ex.execute(new Job(count,countDown));
        }
        countDown.await();
        System.out.println(count.count);
        ex.shutdown();
    }

}
