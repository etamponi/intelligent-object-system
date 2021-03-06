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


public class PositivenessCheck extends PropertyCheck<Number> {
	
	private boolean zeroAccepted;
	
	public PositivenessCheck(String path, boolean zeroAccepted) {
		super(path);
		this.zeroAccepted = zeroAccepted;
	}

	@Override
	protected String getError(Number value) {
		if (value.doubleValue() < 0 || (!zeroAccepted && value.doubleValue() == 0))
			return "should be positive" + (zeroAccepted ? " or zero" : "");
		else
			return null;
	}

}
