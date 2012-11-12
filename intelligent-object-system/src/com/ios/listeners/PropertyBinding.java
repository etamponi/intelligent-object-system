package com.ios.listeners;

import java.util.ArrayList;
import java.util.List;

import com.ios.ChangeListener;
import com.ios.IObject;
import com.ios.Property;

public class PropertyBinding implements ChangeListener {
	
	private final Property master;
	private final List<Property> slaves = new ArrayList<>();
	private final List<Property> listening = new ArrayList<>();

	public PropertyBinding(IObject root, String masterPath, String... slavePaths) {
		master = new Property(root, masterPath);
		
		listening.add(new Property(root, masterPath));
		for(String slavePath: slavePaths) {
			if (slavePath.split(".").length >= IObject.MAXIMUM_CHANGE_PROPAGATION)
				System.err.println("Warning: change propagation will be incomplete");
			
			slaves.add(new Property(root, slavePath));
			
			if (!slavePath.contains("."))
				continue;
			String prefix = slavePath.substring(0, slavePath.lastIndexOf('.'));
			listening.add(new Property(root, prefix));
		}
	}

	@Override
	public List<Property> getBoundProperties(Property prefixPath, IObject root) {
		List<Property> ret = new ArrayList<>();
		for(Property slave: slaves) {
			if (prefixPath.isParent(slave))
				ret.add(new Property(root, slave.getLastPart()));
		}
		return ret;
	}

	@Override
	public boolean isListening(Property path) {
		for(Property l: listening) {
			if (path.isPrefix(l, false))
				return true;
		}
		return false;
	}

	@Override
	public void onChange(Property changedPath) {
		Object masterContent = master.getContent();
		if (changedPath.isPrefix(master, false)) {
			for(Property slave: slaves)
				slave.setContent(masterContent);
		} else {
			for(Property slave: slaves) {
				if (changedPath.isPrefix(slave, false))
					slave.setContent(masterContent);
			}
		}
	}

}
