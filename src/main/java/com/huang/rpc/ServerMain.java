package com.huang.rpc;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import com.huang.rpc.common.Options;
import com.huang.rpc.datasource.DataSource;
import com.huang.rpc.datasource.impl.PageDataSource;
import com.huang.rpc.server.NioServer;
import com.huang.rpc.server.ServerConfig;

public class ServerMain {
	
	public static void startServer(String ...args) throws IOException,InterruptedException{
		
		final ServerConfig config = new ServerConfig();
		
		File dataFile = new File(ServerMain.class.getClassLoader().getResource(args[1]).getPath());
		
		config.setDataFile(dataFile);
		config.setPort(Integer.valueOf(args[2]));
		
		final Options options = new Options(new File(ServerMain.class.getClassLoader().getResource(args[3]).getPath()));
		final DataSource dataSource = new PageDataSource(config.getDataFile());
		
		dataSource.init();
		final ExecutorService executorService = Executors.newCachedThreadPool();
		
		final NioServer server = new NioServer(dataSource, executorService, config, options);
		server.startup();
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				Thread.currentThread().setName("server-shutdown-hook");
				try{
					dataSource.destroy();
					server.shutdown();
					executorService.shutdown();
				}catch(IOException e){
					
				}
			}
		});
		
		LockSupport.park();

	}
	
	public static void main(String... args) throws IOException, InterruptedException {
		args = new String[]{"hello world","newdata.txt","9999","mycode.properties"};
    	startServer(args);
	}
	
}
