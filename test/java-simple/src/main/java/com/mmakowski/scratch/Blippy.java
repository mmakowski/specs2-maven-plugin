package com.mmakowski.scratch;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

public class Blippy {
	public String foo() {
		return StringUtils.capitalize("boo!!!");
	}
	
	public String toString() {
	    return new ToStringBuilder(this).toString();
	}
}
