package com.huang.rpc;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huang.rpc.common.Options;
import com.huang.rpc.datasource.DataSource;
import com.huang.rpc.datasource.impl.MappingDataSource;
import com.huang.rpc.server.NioServer;
import com.huang.rpc.server.ServerConfig;

public class Main {
	
	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	public static void startServer(String ...args) throws IOException,InterruptedException{
		
		final ServerConfig config = new ServerConfig();
		config.setDataFile(new File(args[1]));
		config.setPort(Integer.valueOf(args[2]));
		
		final Options options = new Options(new File(args[3]));
		final DataSource dataSource = new MappingDataSource(config.getDataFile());
		
		dataSource.init();
		final CountDownLatch countDownLatch = new CountDownLatch(1);
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
		
		countDownLatch.await();
	}

}
