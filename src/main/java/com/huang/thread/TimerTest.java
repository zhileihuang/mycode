package com.huang.thread;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.LockSupport;

import org.junit.Test;

public class TimerTest {

	/**
	 * 一秒后开始执行task，每两秒执行一次
	 */
	@Test
	public void test1(){
		Timer timer = new Timer("timer-test", true);
		timer.schedule(new MyTask(), 1000,2000);
		LockSupport.park();
	}
	
	
	class MyTask extends TimerTask{
		
		@Override
		public void run() {
			
			System.out.println("add");
			
		}
		
	}
	
}
