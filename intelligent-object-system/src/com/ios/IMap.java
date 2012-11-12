package com.ios;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.esotericsoftware.kryo.serializers.FieldSerializer;

public class IMap<V> extends IObject implements Map<String, V> {
	
	static {
		getKryo().addDefaultSerializer(IMap.class, FieldSerializer.class);
	}

	private final Class<V> valueType;
	
	private final Map<String, V> internal;
	
	public IMap(Class<V> valueType) {
		this.valueType = valueType;
		
		this.internal = new HashMap<>();
	}
	
	public IMap(Class<V> valueType, Map<String, V> internal) {
		this.valueType = valueType;
		
		this.internal = internal;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean containsKey(Object key) {
		return internal.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return internal.containsValue(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, V>> entrySet() {
		return internal.entrySet();
	}

	@Override
	public V get(Object key) {
		return internal.get(key);
	}

	@Override
	public boolean isEmpty() {
		return internal.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return internal.keySet();
	}

	@Override
	public V put(String key, V value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends V> other) {
		// TODO Auto-generated method stub

	}

	@Override
	public V remove(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		return internal.size();
	}

	@Override
	public Collection<V> values() {
		return internal.values();
	}

	@Override
	protected Object getLocal(String propertyName) {
		// TODO Auto-generated method stub
		return super.getLocal(propertyName);
	}

	@Override
	protected void setLocal(String propertyName, Object content) {
		// TODO Auto-generated method stub
		super.setLocal(propertyName, content);
	}

	@Override
	protected List<Property> getInstanceProperties() {
		// TODO Auto-generated method stub
		return super.getInstanceProperties();
	}

	@Override
	public Class<?> getType(String propertyName, boolean runtime) {
		// TODO Auto-generated method stub
		return super.getType(propertyName, runtime);
	}
	
	public Class<V> getValueType() {
		return valueType;
	}

}
