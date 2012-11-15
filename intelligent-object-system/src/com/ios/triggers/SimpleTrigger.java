package com.ios.triggers;

import com.ios.Trigger;
import com.ios.Listener;

public abstract class SimpleTrigger extends Trigger {
	
	public SimpleTrigger(Listener... listeners) {
		for(Listener l: listeners)
			getListeners().add(l);
	}

}
