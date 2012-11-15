package com.ios.listeners;

import com.ios.IObject;
import com.ios.Listener;
import com.ios.Property;

public class BoundProperties extends Listener {
	
	public BoundProperties(IObject root, String... propertyNames) {
		for(String name: propertyNames)
			getSlaves().add(new Property(root, name));
	}

	@Override
	public void action(Property changedPath) {
		// Does nothing
	}

}
