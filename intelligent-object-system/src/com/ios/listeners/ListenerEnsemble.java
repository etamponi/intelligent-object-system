package com.ios.listeners;

import com.ios.Property;
import com.ios.Listener;

public class ListenerEnsemble extends Listener {
	
	private Listener[] listeners;
	
	public ListenerEnsemble(Listener... listeners) {
		this.listeners = listeners;
	}

	@Override
	public boolean isListeningOn(Property path) {
		for(Listener l: listeners) {
			if (l.isListeningOn(path))
				return true;
		}
		return false;
	}

}
