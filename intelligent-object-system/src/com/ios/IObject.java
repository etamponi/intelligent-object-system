package com.ios;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.objenesis.strategy.StdInstantiatorStrategy;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.reflectasm.FieldAccess;

public class IObject {

	public static final int MAXIMUM_CHANGE_PROPAGATION = 5;

	private static final Kryo kryo = new Kryo();

	static {
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
	}
	
	protected static Kryo getKryo() {
		return kryo;
	}

	@SuppressWarnings("unchecked")
	public static <T extends IObject> T load(InputStream in) {
		Input input = new Input(in);
		T ret = (T) kryo.readClassAndObject(input);
		input.close();
		return ret;
	}

	private final List<Property> parentsLinkToThis = new ArrayList<>();

	private final List<ChangeListener> listeners = new ArrayList<>();

	public String name = String.format("%s-%3d", getClass().getSimpleName(), hashCode() % 1000);

	protected void addListener(ChangeListener listener) {
		this.listeners.add(listener);
	}

	@SuppressWarnings("unchecked")
	public <T extends IObject> T copy() {
		IObject copy = kryo.copy(this);

		IdentityHashMap<IObject, Void> descendents = new IdentityHashMap<>();
		IdentityHashMap<IObject, Void> nonDescendents = new IdentityHashMap<>();
		copy.removeInvalidLinks(copy, descendents, nonDescendents);
		
		for (Property childProperty: copy.getIntelligentProperties()) {
			childProperty.getContent(IObject.class).removeInvalidLinks(copy, descendents, nonDescendents);
		}

		return (T) copy;
	}

	private boolean descendFrom(IObject ancestor) {
		if (this == ancestor)
			return true;
		for (Property linkToThis : parentsLinkToThis) {
			if (linkToThis.getRoot().descendFrom(ancestor))
				return true;
		}
		return false;
	}

	public void detach() {
		for (Property linkToThis : new ArrayList<>(parentsLinkToThis))
			linkToThis.setContent(null);

		for (Property intelligentProperty : getIntelligentProperties())
			intelligentProperty.setContent(null);
	}

	public Object getContent(String propertyPath) {
		if (propertyPath.isEmpty())
			return null;

		int firstSplit = propertyPath.indexOf('.');
		if (firstSplit < 0) {
			return getLocal(propertyPath);
		} else {
			String localProperty = propertyPath.substring(0, firstSplit);
			String remainingPath = propertyPath.substring(firstSplit + 1);
			IObject local = (IObject) getLocal(localProperty);
			if (local != null)
				return local.getContent(remainingPath);
			else
				return null;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getContent(String propertyPath, Class<T> contentType) {
		return (T) getContent(propertyPath);
	}

	public List<Property> getBoundProperties() {
		List<Property> ret = new ArrayList<>();
		recursivelyFindBoundProperties(this, new Property(this, ""), ret,
				HashTreePSet.<Property> empty());
		return ret;
	}

	public Map<Property, List<String>> getErrors() {
		Map<Property, List<String>> ret = new LinkedHashMap<>();

		// FIXME getErrors() implementation
		
		return ret;
	}

	protected List<Property> getInstanceProperties() {
		return new ArrayList<>();
	}

	public List<Property> getIntelligentProperties() {
		List<Property> ret = new ArrayList<>();
		for (Property property: getProperties()) {
			if (property.getContent() instanceof IObject)
				ret.add(property);
		}
		return ret;
	}

	protected Object getLocal(String propertyName) {
		// TODO Add access through getter
		FieldAccess fieldAccess = FieldAccess.get(getClass());
		return fieldAccess.get(this, propertyName);
	}

	public List<Property> getParentsLinksToThis() {
		return Collections.unmodifiableList(parentsLinkToThis);
	}
	
	protected List<Property> getParentsLinksToThis(boolean editable) {
		return parentsLinkToThis;
	}
	
	public List<String> getFieldPropertyNames() {
		List<String> ret = new ArrayList<>();
		
		Stack<Class<?>> types = new Stack<>();
		types.add(getClass());
		while (!types.peek().equals(IObject.class))
			types.push(types.peek().getSuperclass());

		for (Class<?> type : types) {
			for (Field field : type.getDeclaredFields()) {
				int mod = field.getModifiers();
				if (Modifier.isPublic(mod) && !Modifier.isStatic(mod)
						&& !Modifier.isFinal(mod))
					ret.add(field.getName());
			}
		}
		
		return ret;
	}

	public List<Property> getProperties() {
		List<Property> ret = new ArrayList<>();

		for(String name: getFieldPropertyNames())
			ret.add(new Property(this, name));
		
		ret.addAll(getInstanceProperties());
		return ret;
	}

	public Class<?> getType(String propertyName, boolean runtime) {
		if (runtime) {
			Object content = getLocal(propertyName);
			return content == null ? null : content.getClass();
		} else {
			try {
				return getClass().getField(propertyName).getType();
			} catch (NoSuchFieldException | SecurityException e) {
				return null;
			}
		}
	}

	public List<Property> getUnboundProperties() {
		List<Property> ret = getProperties();
		ret.removeAll(getBoundProperties());
		return ret;
	}

	private void innerSetLocal(String propertyName, Object content) {
		Property property = new Property(this, propertyName);

		Object oldContent = getLocal(propertyName);

		if (oldContent == content)
			return;

		if (oldContent instanceof IObject) {
			((IObject) oldContent).parentsLinkToThis.remove(property);
		}

		setLocal(propertyName, content);

		if (content instanceof IObject) {
			((IObject) content).parentsLinkToThis.add(property);
		}

		propagateChange(property, HashTreePSet.<Property> empty(), 0);
	}

	private void notifyChange(Property changedPath) {
		for (ChangeListener listener : listeners) {
			if (listener.isListening(changedPath))
				listener.onChange(changedPath);
		}
	}

	private Property prependParent(Property parent, Property path) {
		if (path.getPath().isEmpty())
			return parent;
		else
			return new Property(parent.getRoot(), parent.getPath() + "." + path.getPath());
	}

	protected void propagateChange(Property property, PSet<Property> seen, int level) {
		property.getRoot().notifyChange(property);

		if (level == MAXIMUM_CHANGE_PROPAGATION)
			return;

		for (Property linkToThis : parentsLinkToThis) {
			if (seen.contains(linkToThis))
				continue;
			linkToThis.getRoot().propagateChange(prependParent(linkToThis, property), seen.plus(linkToThis), level + 1);
		}
	}

	private void recursivelyFindBoundProperties(IObject original,
			Property parentPath, List<Property> list, PSet<Property> seen) {
		for (ChangeListener l : parentPath.getRoot().listeners)
			list.addAll(l.getBoundProperties(parentPath));

		for (Property linkToThis : parentsLinkToThis) {
			if (seen.contains(linkToThis))
				continue;
			recursivelyFindBoundProperties(original, prependParent(linkToThis, parentPath), list, seen.plus(linkToThis));
		}
	}

	private void removeInvalidLinks(IObject ancestor, IdentityHashMap<IObject, Void> descendents, IdentityHashMap<IObject, Void> nonDescendents) {
		
		for (Property linkToThis : new ArrayList<>(parentsLinkToThis)) {
			IObject parent = linkToThis.getRoot();

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

	public void setContent(String propertyPath, Object content) {
		if (propertyPath.isEmpty())
			return;

		int firstSplit = propertyPath.indexOf('.');
		if (firstSplit < 0) {
			innerSetLocal(propertyPath, content);
		} else {
			String localProperty = propertyPath.substring(0, firstSplit);
			String remainingPath = propertyPath.substring(firstSplit + 1);
			IObject local = (IObject) getLocal(localProperty);
			if (local != null)
				local.setContent(remainingPath, content);
		}
	}

	protected void setLocal(String propertyName, Object content) {
		// TODO Access through setter
		FieldAccess fieldAccess = FieldAccess.get(getClass());
		fieldAccess.set(this, propertyName, content);
	}
	
	public void write(OutputStream out) {
		Output output = new Output(out);
		kryo.writeClassAndObject(output, this.copy());
		output.close();
	}

}
