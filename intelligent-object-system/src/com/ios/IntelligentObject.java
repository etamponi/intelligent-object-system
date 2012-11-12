package com.ios;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

import org.objenesis.strategy.StdInstantiatorStrategy;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.reflectasm.FieldAccess;



public class IntelligentObject {
	
	public static final int MAXIMUM_CHANGE_PROPAGATION = 10;
	
	private static final Kryo kryo = new Kryo();
	
	static {
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
	}
	
	private final List<Property> parentsLinkToThis = new ArrayList<>();
	
	private final List<Property> instanceProperties = new ArrayList<>();
	
	private final List<Property> intelligentProperties = new ArrayList<>();
	
	private final List<ChangeListener> listeners = new ArrayList<>();

	public String name = getClass().getSimpleName() + (hashCode() % 10);
	
	public Object get(String propertyPath) {
		if (propertyPath.isEmpty())
			return null;
		
		int firstSplit = propertyPath.indexOf('.');
		if (firstSplit < 0) {
			return getLocal(propertyPath);
		} else {
			String localProperty = propertyPath.substring(0, firstSplit);
			String remainingPath = propertyPath.substring(firstSplit+1);
			IntelligentObject local = (IntelligentObject)getLocal(localProperty);
			if (local != null)
				return local.get(remainingPath);
			else
				return null;
		}
	}
	
	public <T> T get(String propertyPath, Class<T> contentType) {
		return (T)get(propertyPath);
	}
	
	protected Object getLocal(String propertyName) {
		// TODO Add access through getter
		FieldAccess fieldAccess = FieldAccess.get(getClass());
		return fieldAccess.get(this, propertyName);
	}
	
	public void set(String propertyPath, Object content) {
		if (propertyPath.isEmpty())
			return;
		
		int firstSplit = propertyPath.indexOf('.');
		if (firstSplit < 0) {
			setLocal(propertyPath, content);
		} else {
			String localProperty = propertyPath.substring(0, firstSplit);
			String remainingPath = propertyPath.substring(firstSplit+1);
			IntelligentObject local = (IntelligentObject)getLocal(localProperty);
			if (local != null)
				local.set(remainingPath, content);
		}
	}
	
	private void setLocal(String propertyName, Object content) {
		Property property = new Property(this, propertyName);
		
		Object oldContent = getLocal(propertyName);
		
		if (oldContent == content)
			return;
		
		if (oldContent instanceof IntelligentObject) {
			this.intelligentProperties.remove(property);
			((IntelligentObject)oldContent).parentsLinkToThis.remove(property);
		}
		
		innerSetLocal(propertyName, content);
		
		if (content instanceof IntelligentObject) {
			this.intelligentProperties.add(property);
			((IntelligentObject)content).parentsLinkToThis.add(property);
		}
		
		propagateChange(property, HashTreePSet.<Property>empty(), 0);
	}
	
	protected void addListener(ChangeListener listener) {
		this.listeners.add(listener);
	}
	
	protected void innerSetLocal(String propertyName, Object content) {
		// TODO Access through setter
		FieldAccess fieldAccess = FieldAccess.get(getClass());
		fieldAccess.set(this, propertyName, content);
	}
	
	private void propagateChange(Property property, PSet<Property> seen, int level) {
		property.getRoot().notifyChange(property);
		
		if (level == MAXIMUM_CHANGE_PROPAGATION)
			return;
		
		for(Property linkToThis: parentsLinkToThis) {
			if (seen.contains(linkToThis))
				continue;
			linkToThis.getRoot().propagateChange(prependParent(linkToThis, property), seen.plus(linkToThis), level+1);
		}
	}
	
	private void notifyChange(Property changedPath) {
		for (ChangeListener listener: listeners) {
			if (listener.isListening(changedPath))
				listener.onChange(changedPath);
		}
	}
	
	public void detach() {
		for (Property linkToThis: new ArrayList<>(parentsLinkToThis))
			linkToThis.setContent(null);
		
		for (Property intelligentProperty: new ArrayList<>(intelligentProperties))
			intelligentProperty.setContent(null);
	}
	
	public List<Property> getParentsLinksToThis() {
		return Collections.unmodifiableList(parentsLinkToThis);
	}
	
	public List<Property> getProperties() {
		List<Property> ret = new ArrayList<>();
		for(Field field: getClass().getFields()) {
			if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers()))
				ret.add(new Property(this, field.getName()));
		}
		ret.addAll(instanceProperties);
		return Collections.unmodifiableList(ret);
	}
	
	public List<Property> getIntelligentProperties() {
		return Collections.unmodifiableList(intelligentProperties);
	}
	
	public List<Property> getBoundProperties() {
		List<Property> ret = new ArrayList<>();
		
		recursivelyFindBoundProperties(this, new Property(this, ""), ret, HashTreePSet.<Property>empty());
		
		return ret;
	}
	
	private void recursivelyFindBoundProperties(IntelligentObject original, Property parentPath, List<Property> list, PSet<Property> seen) {
		for(ChangeListener l: parentPath.getRoot().listeners)
			list.addAll(l.getBoundProperties(parentPath, original));
		
		for(Property linkToThis: parentsLinkToThis) {
			if (seen.contains(linkToThis))
				continue;
			recursivelyFindBoundProperties(original, prependParent(linkToThis, parentPath), list, seen.plus(linkToThis));
		}
	}
	
	private Property prependParent(Property parent, Property path) {
		if (path.getPath().isEmpty())
			return parent;
		else
			return new Property(parent.getRoot(), parent.getPath() + "." + path.getPath());
	}
	
	protected List<Property> getInstanceProperties() {
		return instanceProperties;
	}
	
	public <T extends IntelligentObject> T copy() {
		IntelligentObject copy = kryo.copy(this);
		
		for(Property linkToThis: new ArrayList<>(copy.parentsLinkToThis))
			linkToThis.setContent(null);
		
		IdentityHashMap<IntelligentObject, Void> descendents = new IdentityHashMap<>();
		IdentityHashMap<IntelligentObject, Void> nonDescendents = new IdentityHashMap<>();
		for(Property childProperty: copy.intelligentProperties) {
			childProperty.getContent(IntelligentObject.class).removeInvalidLinks(copy, descendents, nonDescendents);
		}
		
		return (T)copy;
	}
	
	private boolean descendFrom(IntelligentObject ancestor) {
		if (this == ancestor)
			return true;
		for(Property linkToThis: parentsLinkToThis) {
			if (linkToThis.getRoot().descendFrom(ancestor))
				return true;
		}
		return false;
	}
	
	private void removeInvalidLinks(IntelligentObject ancestor,
			IdentityHashMap<IntelligentObject, Void> descendents,
			IdentityHashMap<IntelligentObject, Void> nonDescendents) {
		for(Property linkToThis: new ArrayList<>(parentsLinkToThis)) {
			IntelligentObject parent = linkToThis.getRoot();
			
			if (descendents.containsKey(parent)) {
				continue;
			}
			
			if (nonDescendents.containsKey(parent)) {
				linkToThis.setContent(null);
				continue;
			}
			
			if (parent.descendFrom(ancestor)) {
				descendents.put(parent, null);
			} else {
				nonDescendents.put(parent, null);
				linkToThis.setContent(null);
			}
			
		}
	}
	
	public void write(OutputStream out) {
		Output output = new Output(out);
		kryo.writeClassAndObject(output, this.copy());
		output.close();
	}
	
	public static <T extends IntelligentObject> T load(InputStream in) {
		Input input = new Input(in);
		T ret = (T)kryo.readClassAndObject(input);
		input.close();
		return ret;
	}

}
