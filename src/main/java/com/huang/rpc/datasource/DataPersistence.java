package com.huang.rpc.datasource;

import java.io.IOException;

public interface DataPersistence {

	//保存一行数据
	void putRow(Row row) throws IOException;
	
	//初始化数据源
	void init() throws IOException;
	
	//刷新持久化数据
	void flush() throws IOException;
	
	//销毁数据持久化
	void destroy() throws IOException;
	
}
