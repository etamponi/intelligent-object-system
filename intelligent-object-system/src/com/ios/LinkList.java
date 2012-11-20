package com.ios;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoCopyable;

public class LinkList extends ArrayList<Property> implements KryoCopyable<LinkList> {

	public LinkList() {
		
	}
	
	public LinkList(Collection collection) {
		super(collection);
	}

	@Override
	public LinkList copy(Kryo kryo) {
		List<Property> temp = new ArrayList<>(this);
		IObject root = (IObject) kryo.getContext().get("root");
		if (root != null) {
			for(ListIterator<Property> it = temp.listIterator(); it.hasNext(); ) {
				Property property = it.next();
				if (!root.isAncestor(property.getRoot()))
					it.remove();
			}
		}
		return new LinkList(kryo.copy(temp));
	}
	
}
