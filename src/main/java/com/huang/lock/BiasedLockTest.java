package com.huang.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * 使用偏向锁，可以获得5%的性能提升
 **/

//arraylist
//-XX:-UseBiasedLocking   out: 3985
//-XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0    out:4009

// vector:
// -XX:-UseBiasedLocking   out:2728
// -XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0    out:2482
public class BiasedLockTest {
	
	public static List<Integer> numberList = new Vector<Integer>();
//	public static List<Integer> numberList = new ArrayList<Integer>();
	
	public static void main(String[] args) {
		
		long begin = System.currentTimeMillis();
		int count = 0;
		int startnum = 0;
		while(count<10000000){
			numberList.add(startnum);
			startnum+=2;
			count++;
		}
		long end = System.currentTimeMillis();
		System.out.println(end-begin);

	}

}
