package com.ios.triggers;

import com.ios.IObject;
import com.ios.Property;
import com.ios.Trigger;

public class BoundProperties extends Trigger {
	
	public BoundProperties(IObject root, String... propertyNames) {
		for(String name: propertyNames)
			getBoundProperties().add(new Property(root, name));
	}

	@Override
	public void action(Property changedPath) {
		// Does nothing
	}

}
