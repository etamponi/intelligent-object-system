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
import com.ios.listeners.PrefixListener;

public class MasterSlaveTrigger extends Trigger {
	
	private final Property master;

	public MasterSlaveTrigger(IObject root, String masterPath, String... slavePaths) {
		assert(slavePaths.length > 0);
		
		master = new Property(root, masterPath);
		
		getListeners().add(new PrefixListener(master));
		
		for(String slavePath: slavePaths) {
			if (slavePath.split(".").length >= IObject.MAXIMUM_CHANGE_PROPAGATION)
				System.err.println("Warning: change propagation may be incomplete");
			
			getBoundProperties().add(new Property(root, slavePath));
			
			if (!slavePath.contains("."))
				continue;
			String prefix = slavePath.substring(0, slavePath.lastIndexOf('.'));
			getListeners().add(new PrefixListener(new Property(root, prefix)));
		}
	}

	@Override
	public void action(Property changedPath) {
		Object content = transform(master.getContent());
		if (changedPath.isPrefix(master, false)) {
			for(Property slave: getBoundProperties())
				updateSlave(slave, content);
		} else {
			for(Property slave: getBoundProperties()) {
				if (changedPath.isPrefix(slave, false))
					updateSlave(slave, content);
			}
		}
	}
	
	protected void updateSlave(Property slave, Object content) {
		slave.setContent(content);
	}

	protected Object transform(final Object content) {
		return content;
	}

}
