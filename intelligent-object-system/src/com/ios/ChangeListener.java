package com.ios;

import java.util.List;

public interface ChangeListener {
	
	public List<Property> getBoundProperties(Property prefixPath, IntelligentObject root);
	
	public boolean isListening(Property path);

	public void onChange(Property changedPath);
	
}
