package com.huang.lock;

//javap -v Synchronized.class > t1
public class Synchronized {
	
	public static void main(String[] args) {
		
		synchronized (Synchronized.class) {
			m();
		}
		
	}
	
	public static synchronized void m(){
		
	}

}
