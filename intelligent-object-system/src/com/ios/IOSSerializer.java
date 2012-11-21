package com.ios;

import java.lang.reflect.ParameterizedType;

import com.esotericsoftware.kryo.Serializer;

public abstract class IOSSerializer<T> extends Serializer<T> {
	
	public Class<T> getSerializingType() {
		return (Class<T>) ((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

}
