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
package com.ios.errorchecks;



import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class NoRepetitionCheck extends PropertyCheck<List> {
	
	public NoRepetitionCheck(String path) {
		super(path);
	}

	@Override
	protected String getError(List value) {
		Set set = new HashSet<>(value);
		if (set.size() < value.size())
			return "cannot have repetitions";
		else
			return null;
	}

}
