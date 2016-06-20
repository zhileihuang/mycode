package com.huang.rpc.util;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class Utils {

	public static byte[] process(byte[] data){
		final int size = data.length;
		final int sub = size/3;
		final byte[] newData = new byte[size-sub];
		System.arraycopy(data, 0, newData, 0, sub);
		System.arraycopy(data, sub+sub, newData, sub, newData.length-sub);
		return newData;
	}
	
	public static byte[] reverse(byte[] data){
		if(data!=null&&data.length!=0){
			byte temp;
			int len = data.length;
			for(int i = 0;i<data.length/2;i++){
				temp = data[i];
				data[i] = data[len-i-1];
				data[len-i-1] = temp;
			}
		}
		return data;
	}
	
	public static void unmap(final MappedByteBuffer buffer){
		if(buffer == null){
			return;
		}
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				try{
					Method getCleanerMethod = buffer.getClass().getMethod("cleaner");
					if(getCleanerMethod!=null){
						getCleanerMethod.setAccessible(true);
						Object cleaner = getCleanerMethod.invoke(buffer);
						Method cleanMethod = cleaner.getClass().getMethod("clean");
						if(cleanMethod!=null){
							cleanMethod.invoke(cleaner);
						}
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				return null;
			}
		});
	}
	
}
