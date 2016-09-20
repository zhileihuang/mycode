package com.huang.tinyhttp.nio.listener;

import java.nio.channels.SelectableChannel;

import com.huang.tinyhttp.nio.EventLoop;

public interface WriterListener {

	public void write(final SelectableChannel channel,final EventLoop manager);
	
}
