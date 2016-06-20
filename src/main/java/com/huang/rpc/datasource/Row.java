package com.huang.rpc.datasource;

import java.util.Arrays;

//一行数据
public class Row {
	private final int lineNum;
	private final byte[] data;
	
	public Row(int lineNum, byte[] data) {
		this.lineNum = lineNum;
		this.data = data;
	}

	public int getLineNum() {
		return lineNum;
	}

	public byte[] getData() {
		return data;
	}

	@Override
	public String toString() {
		return "Row [lineNum=" + lineNum + ", data=" + Arrays.toString(data) + "]";
	}
	
}
