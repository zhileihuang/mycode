package com.huang.rpc;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.huang.rpc.client.ClientConfig;
import com.huang.rpc.client.NioClient;
import com.huang.rpc.common.Options;
import com.huang.rpc.datasource.DataPersistence;
import com.huang.rpc.datasource.impl.BucketDataPersistence;
import com.huang.rpc.datasource.impl.PageDataPersistence;

public class ClientMain {
	
	private static final Logger log = LoggerFactory.getLogger(ClientMain.class);
	
	public static void startClient(String ...args) throws IOException, InterruptedException{
		
		final long startTime = System.currentTimeMillis();
		
		File dataFile = new File(args[3]);
		
		File optionFile = new File(ClientMain.class.getClassLoader().getResource(args[4]).getPath());
		
		final ClientConfig config = new ClientConfig(dataFile, new InetSocketAddress(args[1], Integer.valueOf(args[2])));
	    final Options options = new Options(optionFile);
	    final int worksNum = options.getClientWorkNumbers();
	    
	    final CountDownLatch countDown = new CountDownLatch(worksNum);
	    
	    final CyclicBarrier cyclicBarrier = new CyclicBarrier(worksNum*2+1);
	    
	    final ExecutorService executorService = Executors.newCachedThreadPool((r)->{
	    	final Thread t = new Thread(r);
	    	t.setDaemon(true);
	    	return t;
	    });
	    
	    final DataPersistence dataPersistence = new PageDataPersistence(config.getDataFile());
	    //异步初始化数据源
	    executorService.execute(()->{
	    	try{
	    		dataPersistence.init();
	    		try{
	    			cyclicBarrier.await();
	    		}catch(Exception e){
	    			
	    		}
	    	}catch (Exception e) {
	    		log.warn("DataPersistence.init failed.");
	    	}
	    });
	    
	    
	    //异步创建建立链接
	    final Set<NioClient> clients = new HashSet<>();
	    for(int i = 0;i<worksNum;i++){
	    	executorService.execute(()->{
	    		final NioClient client = new NioClient(countDown, cyclicBarrier, executorService, dataPersistence, config, options);
	    		try{
	    			client.connect();
	    			clients.add(client);
	    			client.work();
	    		}catch (Exception e) {
	    			log.warn("client connect failed.",e);
	    		}
	    	});
	    }
	    
	    //等待所有client完成
	    countDown.await();
	    
	    final long endTime = System.currentTimeMillis();
	    log.info("cost="+(endTime-startTime));
	    
	    //刷新结果
	    dataPersistence.flush();
	    dataPersistence.destroy();
	    
	    //register shutdown
	    Runtime.getRuntime().addShutdownHook(new Thread(()->{
	    	try{
	    		Thread.currentThread().setName("client-shutdown-hook");
	    		for(NioClient client:clients){
	    			client.disconnect();
	    		}
	    	}catch (Exception e2) {
			}
	    }));
	    
	}
	
	public static void main(String... args) throws IOException, InterruptedException {
		args = new String[]{"hello world","127.0.0.1","9999","newdata2.txt","mycode.properties"};
    	startClient(args);
	}
	
}
