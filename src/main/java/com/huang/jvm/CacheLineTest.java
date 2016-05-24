package com.huang.jvm;

/**
 *在加载longs[i][j]的时候，longs[i][j+1]很可能也会被加载到cache中，所以立即访问longs[i][j+1]将会命中L1 cache
 *而如果访问longs[i+1][j]情况就不一样了，这时候可能会产生cache miss 导致效率低下 
 *
 */
public class CacheLineTest {
	
	private static final int RUNS = 10;
	private static final int DIMENSION_1 = 1024*1024;
	private static final int DIMENSION_2 = 62;
	
	private static long[][] longs;
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {
		
		Thread.sleep(10000);
		
		longs = new long[DIMENSION_1][];
		for(int i = 0;i<DIMENSION_1;i++){
			longs[i] = new long[DIMENSION_2];
			for(int j = 0;j<DIMENSION_2;j++){
				longs[i][j] = 0L;
			}
		}
		
		System.out.println("starting...");
		
		final long start = System.nanoTime();
		long sum = 0L;
		for(int r = 0;r<RUNS;r++){
			
//			for(int j = 0;j<DIMENSION_2;j++){
//				for(int i = 0;i<DIMENSION_1;i++){
//					sum += longs[i][j];
//				}
//			}
			
			for(int i = 0;i<DIMENSION_1;i++){
				for(int j = 0;j<DIMENSION_2;j++){
					sum+=longs[i][j];
				}
			}
			
		}
		
		System.out.println("duration = "+(System.nanoTime()-start));
		
		
	}

}
