package com.mmakowski.scratch;

public class Appy {
	private final Blippy blippy = new Blippy();
	
	public boolean scared() {
		return blippy.foo().toLowerCase().contains("boo!");
	}
	
	public String name() {
	    return "Appy";
	}
}
