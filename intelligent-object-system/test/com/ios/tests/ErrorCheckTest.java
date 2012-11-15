package com.ios.tests;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ios.IObject;
import com.ios.Property;
import com.ios.errorchecks.RangeCheck;
import com.ios.errorchecks.RangeCheck.Bound;


public class ErrorCheckTest {
	
	public static class A extends IObject {
		public A property1;
		
		public String property2;
		
		public int property3;
		
		public double property4;
		
		public float property5;
		
		public A() {
			addErrorCheck("property3", new RangeCheck(1, 2));
			addErrorCheck("property4", new RangeCheck(3, Bound.LOWER));
			addErrorCheck("property5", new RangeCheck(-3, Bound.UPPER));
		}
	}

	@Test
	public void test() {
		A a = new A();
		
		System.out.println(a.printErrors());
		
		Map<Property, List<String>> errors = a.getErrors();
		assertEquals("is null", errors.get(new Property(a, "property2")).get(0));
	}

}