package com.huang.jdk8;

import java.util.ArrayList;

import static java.util.Arrays.*;

import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Before;

import com.huang.jdk8.domain.Album;
import com.huang.jdk8.domain.Artist;
import com.huang.jdk8.domain.Track;
import com.sun.media.sound.AlawCodec;

public class Test {
	
	private List<Artist> list = new ArrayList<>();
	
	@Before
	public void before(){
		
		Artist artist1 = new Artist();
		artist1.setName("name1");
		artist1.setOrigin("origin1");
		List<String> members1 = new ArrayList<>();
		members1.add("m1");
		members1.add("m2");
		members1.add("m3");
		artist1.setMembers(members1);
		
		Artist artist2 = new Artist();
		artist2.setName("name2");
		artist2.setOrigin("origin2");
		List<String> members2 = new ArrayList<>();
		members2.add("m21");
		members2.add("m22");
		members2.add("m23");
		artist2.setMembers(members2);
		
		Artist artist3 = new Artist();
		artist3.setName("name3");
		artist3.setOrigin("origin3");
		List<String> members3 = new ArrayList<>();
		members3.add("m31");
		members3.add("m32");
		members3.add("m33");
		artist3.setMembers(members3);
		
		Artist artist4 = new Artist();
		artist4.setName("name4");
		artist4.setOrigin("London");
		List<String> members4 = new ArrayList<>();
		members4.add("m41");
		members4.add("m42");
		members4.add("m43");
		artist4.setMembers(members4);
		
		Artist artist5 = new Artist();
		artist5.setName("name5");
		artist5.setOrigin("origin5");
		List<String> members5 = new ArrayList<>();
		members5.add("m51");
		members5.add("m52");
		members5.add("m53");
		artist5.setMembers(members5);
		
		Artist artist6 = new Artist();
		artist6.setName("name6");
		artist6.setOrigin("origin6");
		List<String> members6 = new ArrayList<>();
		members6.add("m61");
		members6.add("m62");
		members6.add("m63");
		artist6.setMembers(members6);
		
		
		list.add(artist1);
		list.add(artist2);
		list.add(artist3);
		list.add(artist4);
		list.add(artist5);
		list.add(artist6);
		
		System.out.println("before");
		
	}
	
	/**
	 * 外部迭代
	 */
	@org.junit.Test
	public void test1(){
		
		int count = 0;
		Iterator<Artist> iterator = list.iterator();
		while(iterator.hasNext()){
			Artist artist = iterator.next();
			if(artist.isFrom("London")){
				count++;
			}
		}
		
		System.out.println("count:"+count);
		
	}
	
	/**
	 * 内部迭代
	 */
	@org.junit.Test
	public void test2(){
		
		long count = list.stream().filter(artist -> artist.isFrom("London")).count();
		System.out.println(count);
		
	}
	
	@org.junit.Test
	public void test3(){
		
		List<String> collected = Stream.of("a","b","c").collect(Collectors.toList());
		System.out.println(collected);
		
	}
	
	@org.junit.Test
	public void test4(){
		
		List<String> collected = new ArrayList<>();
		List<String> list = new ArrayList<>();
		list.add("a");
		list.add("b");
		list.add("hello");
		for(String string:list){
			String uppercaseString = string.toUpperCase();
			collected.add(uppercaseString);
		}
		System.out.println(collected);
		
	}
	
	@org.junit.Test
	public void test5(){
		
		List<String> collected = Stream.of("a","b","hello")
				                       .map(string->string.toUpperCase())
				                       .collect(Collectors.toList());
		System.out.println(collected);
	}
	
	@org.junit.Test
	public void test6(){
		
		List<Integer> together = Stream.of(asList(1,2),asList(3,4))
				                       .flatMap(numbers->numbers.stream())
				                       .collect(Collectors.toList());
		
		System.out.println(together);
		
	}
	
	@org.junit.Test
	public void test7(){
		
		List<Track> tracks = asList(new Track("Bakai", 524),
				                    new Track("Violets for Your Furs", 378),
				                    new Track("Time Was", 451));
		
		Track shortestTrack = tracks.stream()
				                    .min(Comparator.comparing(track -> track.getLength()))
				                    .get();
		
		System.out.println(shortestTrack);
		
	}
	
	@org.junit.Test
	public void test8(){
		
		int count = Stream.of(1,2,3)
				          .reduce(4, (acc,element)->acc+element);
		
		System.out.println(count);
		
	}
	
	@org.junit.Test
	public void test9(){
		Album album = new Album();
		
		IntSummaryStatistics trackLengthStats = album.getTracks().stream()
				                                     .mapToInt(track->track.getLength())
				                                     .summaryStatistics();
		
		System.out.printf("Max:%d,Min:%d,Ave:%f,Sum:%d",trackLengthStats.getMax(),
				                                         trackLengthStats.getMin(),
				                                         trackLengthStats.getAverage(),
				                                         trackLengthStats.getSum());
		
	}

	@org.junit.Test
	public void test10(){
		
		Optional<String> a = Optional.of("a");
		System.out.println(a.get());
		
		Optional emptyOptional = Optional.empty();
		Optional alsoEmpty = Optional.ofNullable(null);
		
		System.out.println(emptyOptional.isPresent());
		
	}
	
	@org.junit.Test
	public void test11(){
		
		List<Integer> numbers = asList(1,2,3,4);
		List<Integer> sameOrder = numbers.stream().collect(Collectors.toList());
		System.out.println(numbers+","+sameOrder);
		
	}
	
	@org.junit.Test
	public void test12(){
		
		
		
	}
	
	
	
}
