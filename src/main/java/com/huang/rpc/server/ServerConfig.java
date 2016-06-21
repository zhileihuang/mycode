package com.huang.rpc.server;

import java.io.File;

public class ServerConfig {

	private int port;
	private File dataFile;
	
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public File getDataFile() {
		return dataFile;
	}
	public void setDataFile(File dataFile) {
		this.dataFile = dataFile;
	}
	
}
