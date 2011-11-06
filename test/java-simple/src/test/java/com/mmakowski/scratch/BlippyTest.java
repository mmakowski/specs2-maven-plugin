package com.mmakowski.scratch;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BlippyTest {
	@Test
	public void foo_produces_an_exclamation() {
		assertTrue(new Blippy().foo().endsWith("!"));
	}
}
