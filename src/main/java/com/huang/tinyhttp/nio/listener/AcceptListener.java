package com.huang.tinyhttp.nio.listener;

import java.nio.channels.ServerSocketChannel;

import com.huang.tinyhttp.nio.EventLoop;

public interface AcceptListener {

	public void accept(final ServerSocketChannel channel,final EventLoop manager);
	
}
