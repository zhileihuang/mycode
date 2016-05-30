package com.huang.exception;

/**
 * 
 *新建一个异常对象比新建一个普通对象在耗时上多一个数量级，
 *抛出并捕获异常的耗时比新建一个异常在耗时上也要多一个数量级 
 *
 *创建一个异常对象比一个普通对象耗时多，捕获一个异常耗时更多
 *
 */
public class ExcepionTest {
	
	private int counts;
	
	public ExcepionTest(int counts){
		this.counts = counts;
	}
	
	public void newObject(){
		long l = System.nanoTime();
		for(int i = 0;i<counts;i++){
			new Object();
		}
		System.out.println("建立基础对象:"+(System.nanoTime()-l));
	}
	
	public void newOverrideObj(){
		long l = System.nanoTime();
		for(int i = 0;i<counts;i++){
			new Child();
		}
		System.out.println("建立继承对象:"+(System.nanoTime()-l));
	}
	
	public void newException(){
		long l = System.nanoTime();
		for(int i = 0;i<counts;i++){
			new Exception();
		}
		System.out.println("新建异常对象:"+(System.nanoTime()-l));
	}
	
	public void catchException(){
		long l = System.nanoTime();
		for(int i = 0;i<counts;i++){
			try{
				throw new Exception();
			}catch(Exception e){
				
			}
		}
		System.out.println("抛出并捕获异常:"+(System.nanoTime()-l));
	}
	
	public static void main(String[] args) {
		ExcepionTest test = new ExcepionTest(10000);
		test.newObject();
		test.newOverrideObj();
		test.newException();
		test.catchException();
	}
	
	public static class Child{
		
	}
	
	
}
