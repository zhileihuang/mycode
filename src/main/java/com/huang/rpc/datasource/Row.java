package com.huang.rpc.datasource;

import java.util.Arrays;

//一行数据
public class Row {
	private int lineNum;
	private byte[] data;
	
	public Row(int lineNum, byte[] data) {
		this.lineNum = lineNum;
		this.data = data;
	}
	
	public Row(){}

	public int getLineNum() {
		return lineNum;
	}

	public byte[] getData() {
		return data;
	}
	
	public void setLineNum(int lineNum) {
		this.lineNum = lineNum;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "Row [lineNum=" + lineNum + ", data=" + Arrays.toString(data) + "]";
	}
	
}
