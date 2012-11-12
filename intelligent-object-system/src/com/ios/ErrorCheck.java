package com.ios;

import java.util.List;

public interface ErrorCheck<T> {
	
	public List<String> getErrors(T value);

}
