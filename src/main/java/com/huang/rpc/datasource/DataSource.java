package com.huang.rpc.datasource;

import java.io.IOException;

public interface DataSource {

	//获取一行数据
	Row getRow(Row row) throws IOException;
	
	//获取一行数据
	Row getRow() throws IOException;
	
	//初始化数据源
	void init() throws IOException;

	//销毁数据源
	void destroy() throws IOException;
	
}
