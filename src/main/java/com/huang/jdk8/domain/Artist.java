package com.huang.jdk8.domain;

import java.util.List;

public class Artist {
	
	private String name; //艺术家的名字
	private List<String> members;// 乐队成员
	private String origin;// 乐队来自哪里
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<String> getMembers() {
		return members;
	}
	public void setMembers(List<String> members) {
		this.members = members;
	}
	public String getOrigin() {
		return origin;
	}
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	
	public boolean isFrom(String from){
		return this.origin.equals(from);
	}
	
}
