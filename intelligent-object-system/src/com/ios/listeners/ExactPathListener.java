package com.ios.listeners;

import com.ios.Property;
import com.ios.Listener;

public class ExactPathListener extends Listener {
	
	private final Property path;
	
	public ExactPathListener(Property path) {
		this.path = path;
	}

	@Override
	public boolean isListeningOn(Property other) {
		return path.includes(other);
	}

}
