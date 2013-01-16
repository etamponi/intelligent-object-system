/*******************************************************************************
 * Copyright (c) 2012 Emanuele Tamponi.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Emanuele Tamponi - initial API and implementation
 ******************************************************************************/
package com.ios;

import java.util.ArrayList;
import java.util.List;

public abstract class Trigger {
	
	private final List<Listener> listeners = new ArrayList<>();
	
	private final List<Property> boundPaths = new ArrayList<>();

	public abstract void action(Property changedPath);
	
	public void check(Property changedPath) {
		for(Listener listener: listeners) {
			if (listener.isListeningOn(changedPath)) {
				action(changedPath);
				break;
			}
		}
	}
	
	public List<Listener> getListeners() {
		return listeners;
	}
	
	public List<Property> getBoundProperties() {
		return boundPaths;
	}

	public List<Property> getLocalBoundProperties(Property parentPath) {
		List<Property> ret = new ArrayList<>();
		for(Property bound: boundPaths) {
			if (parentPath.isParent(bound))
				ret.add(new Property(parentPath.getContent(IObject.class), bound.getLastPart()));
		}
		return ret;
	}
	
}
