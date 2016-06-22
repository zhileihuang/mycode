package com.huang.nio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackLogTest {
	
	private static final Logger log = LoggerFactory.getLogger(BackLogTest.class);
	
	@Test
	public void test1() throws Exception{
		
		BufferedReader in = null;  
        PrintWriter out = null;  
        int backlog = 2;  
  
        ServerSocket serversocket = new ServerSocket(10000, backlog);  
        while (true) {  
        	log.debug("启动服务端......");  
            int i;  
            Socket socket = serversocket.accept();  
            log.debug("有客户端连上服务端, 客户端信息如下：" + socket.getInetAddress() + " : " + socket.getPort() + ".");  
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  
            out = new PrintWriter(socket.getOutputStream(), true);  
            do {  
                char[] c = new char[1024];  
                i = in.read(c);  
                log.debug("服务端收到信息: " + new String(c, 0, i));  
            } while (i == -1);  
            out.close();  
            in.close();  
            socket.close();  
            log.debug("关闭服务端......");  
        }  
		
	}

}
