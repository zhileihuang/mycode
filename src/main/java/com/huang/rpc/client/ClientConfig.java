package com.huang.rpc.client;

import java.io.File;
import java.net.InetSocketAddress;

public class ClientConfig {
	private final File dataFile;
	private final InetSocketAddress serverAddress;
	
	public ClientConfig(File dataFile, InetSocketAddress serverAddress) {
		this.dataFile = dataFile;
		this.serverAddress = serverAddress;
	}

	public File getDataFile() {
		return dataFile;
	}

	public InetSocketAddress getServerAddress() {
		return serverAddress;
	}
	
}
