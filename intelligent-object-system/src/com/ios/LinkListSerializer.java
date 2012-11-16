package com.ios;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;

public class LinkListSerializer extends Serializer<Collection> {
	
	private CollectionSerializer internal = new CollectionSerializer();

	@Override
	public Collection read(Kryo kryo, Input input, Class<Collection> type) {
		return new LinkList(internal.read(kryo, input, type));
	}

	@Override
	public void write(Kryo kryo, Output output, Collection object) {
		IObject root = (IObject) kryo.getContext().get("root");
		List<Property> temp = new ArrayList<>(object);
		for(ListIterator<Property> it = temp.listIterator(); it.hasNext(); ) {
			Property property = it.next();
			if (!root.isAncestor(property.getRoot()))
				it.remove();
		}
		internal.write(kryo, output, temp);
	}

	@Override
	public Collection copy(Kryo kryo, Collection object) {
		IObject root = (IObject) kryo.getContext().get("root");
		List<Property> temp = new ArrayList<>(object);
		for(ListIterator<Property> it = temp.listIterator(); it.hasNext(); ) {
			Property property = it.next();
			if (!root.isAncestor(property.getRoot()))
				it.remove();
		}
		return new LinkList(internal.copy(kryo, temp));
	}

}
