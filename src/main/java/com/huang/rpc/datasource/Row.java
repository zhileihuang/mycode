package com.huang.rpc.datasource;

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
		StringBuffer sb = new StringBuffer();
		for(int i = 0;i<data.length;i++){
			sb.append((char)data[i]);
		}
		return "Row [lineNum=" + lineNum + ", data=" + sb.toString() + "]";
	}
	
}
