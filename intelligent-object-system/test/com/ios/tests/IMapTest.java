/*******************************************************************************
 * Copyright (c) 2012 Emanuele Tamponi.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Emanuele Tamponi - initial API and implementation
 ******************************************************************************/
package com.ios.tests;

import static org.junit.Assert.*;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.ios.IMap;
import com.ios.IObject;
import com.ios.PluginManager;
import com.ios.PluginManager.PluginConfiguration;
import com.ios.triggers.MasterSlaveTrigger;


public class IMapTest {
	
	static {
		PluginManager.initialize(new PluginConfiguration());
	}
	
	public static class A extends IObject {
		public String property1;
		
		public IMap<String> map1;
		public IMap<A> map2;
		
		public A() {
			setContent("map1", new IMap<>(String.class));
			setContent("map2", new IMap<>(A.class));
			
			addTrigger(new MasterSlaveTrigger(this, "property1", "map1.copy1"));
			addTrigger(new MasterSlaveTrigger(this, "map1.copy1", "map2.nested1.property1"));
			addTrigger(new MasterSlaveTrigger(this, "map2.nested2.property1", "map1.copy2"));
		}
	}

	@Test
	public void test() {
		IMap<String> map = new IMap<>(String.class);
		map.setContent("test", "Test string");
		assertEquals("Test string", map.get("test"));
		assertEquals("Test string", map.getContent("test"));
		
		map.remove("test");
		assertTrue(map.isEmpty());
		assertFalse(map.containsKey("test"));
		assertEquals(null, map.get("test"));
		
		A a = new A();
		
		a.setContent("property1", "Hello property1!");
		assertEquals("Hello property1!", a.map1.getContent("copy1"));
		assertEquals("Hello property1!", a.map1.get("copy1"));
		
		a.map2.put("nested1", new A());
		assertEquals("Hello property1!", a.getContent("map2.nested1.property1"));
		a.map2.setContent("nested2", new A());
		a.setContent("map2.nested2.property1", "Hello nested property1!");
		assertEquals("Hello nested property1!", a.map1.get("copy2"));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		a.write(out);
		
		A copy = IObject.load(new ByteArrayInputStream(out.toByteArray()));
		
		copy.setContent("map2.nested2.property1", "Hello nested property1! (copy)");
		assertEquals("Hello nested property1! (copy)", copy.getContent("map1.copy2"));
		assertEquals("Hello nested property1!", a.map1.get("copy2"));
		
		copy.map2.remove("nested2");
		assertEquals(null, copy.map1.get("copy2"));
	}

}
