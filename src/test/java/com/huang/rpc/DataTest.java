package com.huang.rpc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;

import org.junit.Test;

public class DataTest {
	
	//一行15个数据
	@Test
	public void createData() throws Exception{
		File temp = new File("newdata.txt");
		if(!temp.exists()){
			temp.createNewFile();
		}
		
		OutputStream out = new FileOutputStream(temp);
		
		for(int i = 0;i<2000;i++){
			out.write(createTemp().getBytes());
			out.write('\r');
			out.write('\n');
		}
		
		out.flush();
		out.close();
		
	}
	
	private String createTemp(){
		StringBuffer sb = new StringBuffer();
		for(int i = 0;i<15;i++){
			sb.append(new Random().nextInt(10));
		}
		return sb.toString();
	}

}
