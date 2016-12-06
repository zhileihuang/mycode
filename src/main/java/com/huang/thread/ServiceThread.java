package com.huang.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ServiceThread implements Runnable{

	private static final Logger log = LoggerFactory.getLogger(ServiceThread.class);
	
	private static final long joinTime = 90*1000;
	protected volatile boolean hasNotified = false;
	protected volatile boolean stoped = false;
	
	protected final Thread thread;
	
	public ServiceThread() {
		this.thread = new Thread(this,this.getServiceName());
	}
	
	public abstract String getServiceName();
	
	public void start(){
		this.thread.start();
	}
	
	public void shutdown(){
		
		this.shutdown(false);
		
	}
	
	public void stop(){
		
		this.stop(false);
		
	}
	
	public void makeStop(){
		this.stoped = true;
		log.info("makestop thread " + this.getServiceName());
	}
	
	public void stop(final boolean interrupt){
		
		this.stoped = true;
		log.info("stop thread "+this.getServiceName());
		synchronized (this) {
			
			if(!this.hasNotified){
				this.hasNotified = true;
				this.notify();
			}
			
		}
		
		if(interrupt){
			this.thread.interrupt();
		}
		
	}
	
	public void shutdown(final boolean interrupt){
		
		this.stoped = true;
		log.info("shutdown thread " +this.getServiceName());
		
		synchronized (this) {
			if(!this.hasNotified){
				this.hasNotified = true;
				this.notify();
			}
		}
		
		try{
			if(interrupt){
				this.thread.interrupt();
			}
			
			long beginTime = System.currentTimeMillis();
			this.thread.join(this.getJoinTime());
			long eclipseTime = System.currentTimeMillis() - beginTime;
			log.info("join thread " +this.getServiceName() +" eclipse time(ms) "+eclipseTime+" "+this.getJoinTime());
			
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		
	}
	
	public void wakeup(){
		synchronized (this) {
			if(!this.hasNotified){
				this.hasNotified = true;
				this.notify();
			}
		}
	}
	
	public void waitForRunning(long interval){
		synchronized (this) {
			
			if(this.hasNotified){
				this.hasNotified = false;
				this.onWaitEnd();
				return;
			}
			
			try{
				this.wait(interval);
			}catch(InterruptedException e){
				e.printStackTrace();
			}finally{
				this.hasNotified = false;
				this.onWaitEnd();
			}
			
		}
	}
	
	
    public long getJoinTime(){
    	return joinTime;
    }
    
    public boolean isStoped(){
    	return stoped;
    }
    
    public void onWaitEnd(){
    	
    }
	

}
