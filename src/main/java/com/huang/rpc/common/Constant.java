package com.huang.rpc.common;

public class Constant {
	
	//mc
	public static final int PRO_MC = 0x0CFF;

	//mc掩码
	public static final int PRO_MC_MASK = 0xFFFF0000;
	
	//获取数据请求
	public static final int PRO_REQ_GETDATA = PRO_MC << 16 | 0x01;
	
	//返回数据请求
	public static final int PRO_RESP_GET_DATA = PRO_MC << 16 | 0x02;
	
	//返回数据结束
	public static final int PRO_RESP_GETEOF = PRO_MC << 16 | 0x03;
	
	
	public static void main(String[] args) {
		System.out.println(PRO_MC);
		System.out.println(PRO_MC_MASK);
		System.out.println(PRO_REQ_GETDATA);
		System.out.println(PRO_RESP_GET_DATA);
		System.out.println(PRO_RESP_GETEOF);
	}
	
	
}
