package com.ios.errorchecks;

import java.util.ArrayList;
import java.util.List;

import com.ios.ErrorCheck;

public class RangeCheck implements ErrorCheck<Number> {
	
	private Number lowerBound, upperBound;
	
	public enum Bound {
		LOWER, UPPER
	}
	
	public RangeCheck(Number lowerBound, Number upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}
	
	public RangeCheck(Number bound, Bound type) {
		if (type == Bound.LOWER) {
			this.lowerBound = bound;
			this.upperBound = Double.POSITIVE_INFINITY;
		} else {
			this.upperBound = bound;
			this.lowerBound = Double.NEGATIVE_INFINITY;
		}
	}

	@Override
	public List<String> getErrors(Number value) {
		List<String> ret = new ArrayList<>();
		if (value.doubleValue() > upperBound.doubleValue())
			ret.add("upper bound is " + upperBound + " (current value: " + value + ")");
		if (value.doubleValue() < lowerBound.doubleValue())
			ret.add("lower bound is " + lowerBound + " (current value: " + value + ")");
		return ret;
	}

}
