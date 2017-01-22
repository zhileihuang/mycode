package com.huang.jdk8.domain;

public class Track {

	private String name; //曲目名称
	private int length; //长度
	
	public Track(String name, int length) {
		this.name = name;
		this.length = length;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	@Override
	public String toString() {
		return "Track [name=" + name + ", length=" + length + "]";
	}
	
}
