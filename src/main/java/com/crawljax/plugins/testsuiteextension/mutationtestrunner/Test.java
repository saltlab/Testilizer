package com.crawljax.plugins.testsuiteextension.mutationtestrunner;
import static org.junit.Assert.*;
public class Test {

	@org.junit.Test
	public void passingTest() {
		Code code = new Code();
		int actual = code.add(1, 1);
		assertEquals(2, actual);
	}

	@org.junit.Test
	public void failingTest() {
		Code code = new Code();
		int actual = code.add(1, 1);
		assertEquals(1, actual);
	}
}