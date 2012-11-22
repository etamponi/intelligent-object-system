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
package com.ios.triggers;

import com.ios.Trigger;
import com.ios.Listener;

public abstract class SimpleTrigger extends Trigger {
	
	public SimpleTrigger(Listener... listeners) {
		for(Listener l: listeners)
			getListeners().add(l);
	}

}
