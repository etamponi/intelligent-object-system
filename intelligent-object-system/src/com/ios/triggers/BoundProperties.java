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
