package com.ios;

import com.ios.listeners.SubPathListener;
import com.ios.triggers.SimpleTrigger;

public abstract class Observer extends IObject {
	
	private static final int nameLength = "observed".length();
	
	public IObject observed;
	
	public Observer(IObject observed) {
		setContent("observed", observed);
		addTrigger(new SimpleTrigger(new SubPathListener(new Property(this, "observed"))) {
			private Observer wrapper = Observer.this;
			@Override
			public void action(Property changedPath) {
				String subPath = changedPath.getPath().substring(Observer.nameLength);
				wrapper.action(new Property(wrapper.observed, subPath));
			}
		});
	}
	
	protected abstract void action(Property changedPath);

}