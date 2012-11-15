package com.ios.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ios.IList;
import com.ios.IObject;
import com.ios.triggers.MasterSlaveTrigger;


public class IListTest {
	
	public static class A extends IObject {
		public IList<String> list;
		
		public IList<A> listA;
		
		public IList<A> listB;
		
		public String first;
		
		public String third;
		
		public String other;
		
		public A() {
			setContent("list", new IList<>(String.class));
			setContent("listA", new IList<>(A.class));
			setContent("listB", new IList<>(A.class));
			
			addTrigger(new MasterSlaveTrigger(this, "list.0", "first"));
			addTrigger(new MasterSlaveTrigger(this, "list.2", "third"));
			
			addTrigger(new MasterSlaveTrigger(this, "listA.0.first", "other"));
			
			addTrigger(new MasterSlaveTrigger(this, "other", "listB.*.first"));
		}
	}
	
	@Test
	public void test() {
		A a = new A();
		a.list.add("Hello first");
		a.list.add("Hello second");
		a.list.add("Hello third");
		assertEquals(a.getContent("list.0"), a.first);
		assertEquals(a.getContent("list.2"), a.third);
		
		a.list.remove(2);
		assertEquals(null, a.third);
		
		a.listA.add(a);
		a.listA.get(0).list.add("Hello first nested");
		assertEquals(a.listA.get(0).list.get(0), a.other);
		
		A copy = a.copy();
		
		assertEquals("Hello first", copy.list.get(0));
		assertEquals(copy, copy.listA.get(0));
		
		copy.list.set(0, "Hello first copy");
		assertEquals("Hello first", a.first);
		assertEquals("Hello first copy", copy.first);
		
		copy.listA.set(0, a);
		assertEquals("Hello first", copy.other);
		
		copy.listA.add(0, copy);
		
		copy.listA.remove(copy);
		
		copy.listB.add(new A());
		copy.listB.add(new A());
		assertEquals("Hello first", copy.listB.get(1).first);
	}

}
