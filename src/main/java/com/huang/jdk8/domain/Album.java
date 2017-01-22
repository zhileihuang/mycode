package com.huang.jdk8.domain;

import java.util.List;

public class Album {

	private String name; //专辑名
	private List<Track> tracks; //专辑上所有曲目的列表
	private List<String> musicians; //参与创作本专辑的艺术家列表
	private String from;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Track> getTracks() {
		return tracks;
	}
	public void setTracks(List<Track> tracks) {
		this.tracks = tracks;
	}
	public List<String> getMusicians() {
		return musicians;
	}
	public void setMusicians(List<String> musicians) {
		this.musicians = musicians;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public boolean isFrom(String from){
		return this.from.equals(from);
	}
	
	@Override
	public String toString() {
		return "Album [name=" + name + ", tracks=" + tracks + ", musicians=" + musicians + ", from=" + from + "]";
	}
	
}
