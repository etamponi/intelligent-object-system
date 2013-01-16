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
package com.ios;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.reflectasm.FieldAccess;

public class IObject {

	public static final int MAXIMUM_CHANGE_PROPAGATION = 4;

	private static final Kryo kryo = new Kryo() {
		InstantiatorStrategy s = new StdInstantiatorStrategy();
		@Override
		protected ObjectInstantiator newInstantiator (final Class type) {
			if (IObject.class.isAssignableFrom(type))
				return s.newInstantiatorOf(type);
			else
				return super.newInstantiator(type);
		}
	};

	static {
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		kryo.addDefaultSerializer(IList.class, new FieldSerializer(kryo, IList.class));
		kryo.addDefaultSerializer(IMap.class, new FieldSerializer(kryo, IMap.class));
	}
	
	public static Kryo getKryo() {
		return kryo;
	}
	
	public static <T extends IObject> T load(File inFile) {
		try {
			FileInputStream in = new FileInputStream(inFile);
			T ret = (T)load(in);
			in.close();
			return ret;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static <T extends IObject> T load(InputStream in) {
		Input input = new Input(in);
		T ret = (T) kryo.readClassAndObject(input);
		input.close();
		return ret;
	}

	final LinkList parentsLinkToThis = new LinkList();

	private final List<Trigger> triggers = new ArrayList<>();
	
	private final Map<Property, List<ErrorCheck>> errorChecks = new HashMap<>();
	
	private final Map<Property, List<Constraint>> constraints = new HashMap<>();

	public String name = String.format("%s-%02d", getClass().getSimpleName(), hashCode() % 100);
	
	private List<Property> omittedFromErrorCheck = new ArrayList<>();
	
	private List<String[]> omittedFromPropagation = new ArrayList<>();
	
	private Property linkFromEditor = null;
	
	public Property getLinkFromEditor() {
		return linkFromEditor;
	}
	
	public void startEdit(Property linkFromEditor) {
		this.linkFromEditor = linkFromEditor;
	}
	
	protected void addConstraint(String propertyName, Constraint constraint) {
		Property property = new Property(this, propertyName);
		if (!constraints.containsKey(property))
			constraints.put(property, new ArrayList<Constraint>());
		constraints.get(property).add(constraint);
	}
	
	protected void omitFromErrorCheck(String... paths) {
		for (String path: paths)
			omittedFromErrorCheck.add(new Property(this, path));
	}
	
	protected void omitFromPropagation(String... paths) {
		for (String path: paths)
			omittedFromPropagation.add(path.split("\\."));
	}

	protected void addErrorCheck(String propertyName, ErrorCheck check) {
		Property property = new Property(this, propertyName);
		if (!errorChecks.containsKey(property))
			errorChecks.put(property, new ArrayList<ErrorCheck>());
		errorChecks.get(property).add(check);
	}

	protected void addTrigger(Trigger trigger) {
		this.triggers.add(trigger);
	}
	/*
	private Property appendChild(Property path, Property child) {
		if (path.getPath().isEmpty())
			return child;
		else
			return new Property(path.getRoot(), path.getPath() + "." + child.getPath());
	}
	
	static int errorcount = 0;
	private List<String> checkErrors(Property property) {
		System.out.println(String.format("%4d checking errors %s", errorcount++, property));
		List<String> ret = new ArrayList<>();
		Object content = property.getContent();
		if (content == null) {
			ret.add("is null");
		} else {
			if (errorChecks.containsKey(property)) {
				for (ErrorCheck check: errorChecks.get(property)) {
					String error = check.getError(content);
					if (error != null)
						ret.add(error);
				}
			}
		}
		
		return ret;
	}
	*/
	private List<String> checkErrors() {
		List<String> ret = new ArrayList<>();
		for(Property p: errorChecks.keySet()) {
			if (p.getContent() == null) {
				ret.add(p.getPath() + ": is null");
			} else {
				Object content = p.getContent();
				for (ErrorCheck check: errorChecks.get(p)) {
					String error = check.getError(content);
					if (error != null) ret.add(p.getPath() + ": " + error);
				}
			}
		}
		return ret;
	}

	static int triggercount = 0;
	private void checkTriggers(Property changedPath) {
//		System.out.println(String.format("%4d checking triggers for %s", triggercount++, changedPath.getPath()));
		for (Trigger t: triggers)
			t.check(changedPath);
	}

	public <T extends IObject> T copy() {
		kryo.getContext().put("root", this);
		kryo.getContext().put("descendents", new HashSet<IObject>());
		kryo.getContext().put("nondescendents", new HashSet<IObject>());

		Property linkFromEditor = this.linkFromEditor;
		this.linkFromEditor = null;
		
		IObject copy = kryo.copy(this);
		
		this.linkFromEditor = linkFromEditor;

		kryo.getContext().remove("root");
		kryo.getContext().remove("descendents");
		kryo.getContext().remove("nondescendents");
		return (T) copy;
	}

	public void detach() {
		for (Property linkToThis : new ArrayList<>(parentsLinkToThis))
			linkToThis.setContent(null);

		for (Property intelligentProperty : getIntelligentProperties())
			intelligentProperty.setContent(null);
	}
	
	public void editingFinished() {
		linkFromEditor = null;
		propagateChange(new Property(this, ""));
	}
	
	public List<Property> getBoundProperties() {
		List<Property> ret = new ArrayList<>();
		recursivelyFindBoundProperties(new Property(this, ""), ret, HashTreePSet.<Property> empty());
		return ret;
	}
	
	public <T> T getContent(String propertyPath) {
		if (propertyPath.isEmpty())
			return (T)this;

		int firstSplit = propertyPath.indexOf('.');
		if (firstSplit < 0) {
			return (T)getLocal(propertyPath);
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
	
	public <T> T getContent(String propertyPath, Class<T> contentType) {
		return (T) getContent(propertyPath);
	}
	
	public Class<?> getContentType(String propertyName, boolean runtime) {
		if (runtime) {
			Object content = getLocal(propertyName);
			return content == null ? getContentType(propertyName, false) : content.getClass();
		} else {
			try {
				return getClass().getField(propertyName).getType();
			} catch (NoSuchFieldException | SecurityException e) {
				return null;
			}
		}
	}

	public List<String> getErrors() {
		List<String> ret = new ArrayList<>();

//		ret.put(new Property(this, ""), checkErrors(new Property(this, "")));
		
		recursivelyFindErrors(ret, new HashSet<IObject>());
		
		return ret;
	}

	public List<String> getFieldPropertyNames() {
		List<String> ret = new ArrayList<>();
		
		Stack<Class<?>> types = new Stack<>();
		types.add(getClass());
		while (!types.peek().equals(IObject.class))
			types.push(types.peek().getSuperclass());

		while(!types.isEmpty()) {
			Class<?> type = types.pop();
			for (Field field : type.getDeclaredFields()) {
				int mod = field.getModifiers();
				if (Modifier.isPublic(mod) && !Modifier.isStatic(mod)
						&& !Modifier.isFinal(mod))
					ret.add(field.getName());
			}
		}
		
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
		FieldAccess fieldAccess = FieldAccess.get(getClass());
		return fieldAccess.get(this, propertyName);
	}

	protected List<Property> getParentsLinksToThis() {
		return parentsLinkToThis;
	}

	public List<Property> getProperties() {
		List<Property> ret = new ArrayList<>();

		for(String name: getFieldPropertyNames())
			ret.add(new Property(this, name));
		
		ret.addAll(getInstanceProperties());
		return ret;
	}

	public Property getProperty(String propertyName) {
		return new Property(this, propertyName); // TODO: add some check to getProperty
	}

	public List<Property> getUnboundProperties() {
		List<Property> ret = getProperties();
		ret.removeAll(getBoundProperties());
		return ret;
	}

	public Set<Class> getValidContentTypes(String propertyName) {
		Property path = new Property(this, propertyName);
		
		List<Constraint> list = new ArrayList<>();
		recursivelyFindConstraints(path, list, HashTreePSet.<Property> empty());
		
		return PluginManager.getValidImplementationsOf(getContentType(propertyName, false), list);
	}

	private void innerSetLocal(String propertyName, Object content) {
		Property property = new Property(this, propertyName);

		Object oldContent = getLocal(propertyName);

		if (oldContent == content || (oldContent != null && oldContent.equals(content)))
			return;

		if (oldContent instanceof IObject) {
			((IObject) oldContent).parentsLinkToThis.remove(property);
		}

		setLocal(propertyName, content);

		if (content instanceof IObject) {
			((IObject) content).parentsLinkToThis.add(property);
		}

		propagateChange(property);
	}
	
	protected void notifyObservers() {
		for (Property linkToThis: new ArrayList<>(parentsLinkToThis)) {
			IObject root = linkToThis.getRoot();
			if (root instanceof Observer) {
				root.checkTriggers(linkToThis);
			}
		}
	}
	
	private Property prependParent(Property parent, Property path) {
		if (path.getPath().isEmpty())
			return parent;
		else
			return new Property(parent.getRoot(), parent.getPath() + "." + path.getPath());
	}
	
	public String printErrors() {
		List<String> errors = getErrors();
		StringBuilder builder = new StringBuilder();
		
		for(String error: errors)
			builder.append(error).append("\n");
		
		return builder.toString();
	}
	
	protected void propagateChange(Property property) {
		propagateChange(property, HashTreePSet.<Property> empty(), 0);
	}
	
	private void propagateChange(Property path, PSet<Property> seen, int level) {
		path.getRoot().checkTriggers(path);

		if (level == MAXIMUM_CHANGE_PROPAGATION)
			return;

		List<Property> linksToThis = new ArrayList<>();
		if (linkFromEditor == null)
			linksToThis.addAll(parentsLinkToThis);
		else {
			linksToThis.add(linkFromEditor);
			linksToThis.addAll(getLinkFromSubEditors());
		}
		
		for (Property linkToThis : linksToThis) {
			if (seen.contains(linkToThis))
				continue;
			Property fullPath = prependParent(linkToThis, path);
			if (!omitFromPropagation(fullPath))
				linkToThis.getRoot().propagateChange(fullPath, seen.plus(linkToThis), level + 1);
		}
	}
	
	private List<Property> getLinkFromSubEditors() {
		List<Property> ret = new ArrayList<>();
		for(Property link: parentsLinkToThis) {
			if (link.getRoot() instanceof Editor)
				ret.add(link);
		}
		return ret;
	}
	
	private boolean includes(String[] omitted, String[] tokens) {
		if (omitted.length > tokens.length)
			return false;
		for(int i = 0; i < omitted.length; i++) {
			if (omitted[i].equals(Property.ANY))
				continue;
			if (!omitted[i].equals(tokens[i]))
				return false;
		}
		return true;
	}
	
	private boolean omitFromPropagation(Property fullPath) {
		String[] tokens = fullPath.getPathTokens();
		for(String[] omitted: omittedFromPropagation) {
			if (includes(omitted, tokens))
				return true;
		}
		return false;
	}

	private void recursivelyFindBoundProperties(Property path, List<Property> list, PSet<Property> seen) {
		for (Trigger t: triggers)
			list.addAll(t.getLocalBoundProperties(path));

		for (Property linkToThis: parentsLinkToThis) {
			if (seen.contains(linkToThis))
				continue;
			Property fullPath = prependParent(linkToThis, path);
			if (!omitFromPropagation(fullPath))
				linkToThis.getRoot().recursivelyFindBoundProperties(fullPath, list, seen.plus(linkToThis));
		}
	}

	private void recursivelyFindConstraints(Property path, List<Constraint> list, PSet<Property> seen) {
		for(Property constrained: constraints.keySet()) {
			if (constrained.includes(path))
				list.addAll(constraints.get(constrained));
		}

		for (Property linkToThis: parentsLinkToThis) {
			if (seen.contains(linkToThis))
				continue;
			Property fullPath = prependParent(linkToThis, path);
			if (!omitFromPropagation(fullPath))
				linkToThis.getRoot().recursivelyFindConstraints(fullPath, list, seen.plus(linkToThis));
		}
	}

	private void recursivelyFindErrors(List<String> errors, Set<IObject> seen) {
		if (seen.contains(this))
			return;
		else
			seen.add(this);
		
		errors.addAll(checkErrors());
		for(Property p: getIntelligentProperties()) {
			if (omittedFromErrorCheck.contains(p))
				continue;
			List<String> subErrors = new ArrayList<>();
			p.getContent(IObject.class).recursivelyFindErrors(subErrors, seen);
			for(String error: subErrors)
				errors.add(p.getPath() + "." + error);
		}
		
		/*
		for(Property property: getUnboundProperties()) {
			Property complete = appendChild(basePath, property);
			
			List<String> list = checkErrors(property);
			if (!list.isEmpty())
				errors.put(complete, list);
			if (property.getContent() instanceof IObject && !omittedFromErrorCheck.contains(property)) {
				property.getContent(IObject.class).recursivelyFindErrors(complete, errors, seen);
			}
		}
		*/
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
		FieldAccess fieldAccess = FieldAccess.get(getClass());
		fieldAccess.set(this, propertyName, content);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public void write(File outFile) {
		OutputStream out;
		try {
			out = new FileOutputStream(outFile);
			write(out);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void write(OutputStream out) {
		Output output = new Output(out);
		IObject copy = this.copy();
		kryo.writeClassAndObject(output, copy);
		output.close();
	}
	
	public void replace(IObject other) {
		if (!other.getClass().equals(this.getClass()))
			return;
		for(Property linkToOther: new ArrayList<>(other.parentsLinkToThis)) {
			linkToOther.setContent(this);
		}
		other.detach();
	}

}
