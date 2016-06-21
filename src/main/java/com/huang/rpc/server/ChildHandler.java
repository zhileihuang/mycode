package com.huang.rpc.server;

import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class ChildHandler {

	private final SocketChannel socketChannel;
	
	private final AtomicInteger reqCounter = new AtomicInteger(0);
	
	public ChildHandler(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}
	
	
}
