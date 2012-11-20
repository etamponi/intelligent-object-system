package com.ios;

import com.esotericsoftware.kryo.Serializer;

public abstract class IOSSerializer<T> extends Serializer<T> {
	
	public abstract Class<T> getSerializingType();

}
