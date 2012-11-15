package com.ios.listeners;

import com.ios.Property;
import com.ios.Listener;

public class SubPathListener extends Listener {
	
	private Property prefix;
	
	public SubPathListener(Property prefix) {
		this.prefix = prefix;
	}

	@Override
	public boolean isListeningOn(Property path) {
		return prefix.isPrefix(path, false);
	}

}
