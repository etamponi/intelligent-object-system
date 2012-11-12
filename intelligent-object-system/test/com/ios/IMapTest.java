package com.ios;

import static org.junit.Assert.*;

import org.junit.Test;

public class IMapTest {

	@Test
	public void test() {
		IMap<String> map = new IMap<>(String.class);
		map.setContent("test", "Test string");
		assertEquals("Test string", map.get("test"));
		assertEquals("Test string", map.getContent("test"));
	}

}
