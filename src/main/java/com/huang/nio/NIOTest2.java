package com.huang.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NIOTest2 {
	
	public static void doSelect(Selector selector) throws Exception{
		
		while(true){
			int srt = selector.select();
			if(srt<=0){
				continue;
			}
			
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iter = keys.iterator();
			while(iter.hasNext()){
				SelectionKey key = iter.next();
				if(key.isAcceptable()){
					ServerSocketChannel sChannel = (ServerSocketChannel)key.channel();
					SocketChannel cChannel = sChannel.accept();
					cChannel.configureBlocking(false);
					cChannel.register(selector, SelectionKey.OP_READ);
				}else if(key.isReadable()){
					SocketChannel cChannel = (SocketChannel)key.channel();
					ByteBuffer bb = ByteBuffer.allocate(1024);
					int len =cChannel.read(bb);
					bb.flip();
					if(bb.hasArray() && len>0){
						System.out.println("from client "+":"+ new String(bb.array(),0,len));
						int newInterestOps = key.interestOps();
						newInterestOps |= SelectionKey.OP_WRITE;
						key.interestOps(newInterestOps);
					}else if(len<=0){
						System.out.println("no data");//在这里不能忘记关闭channel
						//TODO
						cChannel.close();
					}
					bb.clear();
				} 
				iter.remove();
			}
			
			
		}
		
	}
	/**
	 * 简单描述：Selector的select方法返回的key集合中有一个SelectionKey是可读的，
	 * 但是调用与此SelectionKey关联的channel的read方法，总是返回读取长度是-1。
	 * 既然返回-1,可以说明tcp链接已经断开。
	 * 在下次调用select方法不应再返回这个SelectionKey，也不应该此SelectionKey是可读状态的。
	 * 但事实并非如此
	 */
	public static void main(String[] args) throws Exception{
		Selector selector = Selector.open();
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		serverChannel.socket().bind(new InetSocketAddress(9000), 10);
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		
		doSelect(selector);
	}

}
