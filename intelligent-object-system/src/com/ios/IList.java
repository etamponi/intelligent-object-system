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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class IList<E> extends IObject implements List<E> {
	
	private final Class<E> elementType;
	
	private final List<E> internal = new ArrayList<>();
	
	private boolean propagate = true;
	
	public IList(Class<E> elementType) {
		this.elementType = elementType;
	}
	
	public IList(Class<E> elementType, List<E> other) {
		this.elementType = elementType;
		
		this.internal.addAll(other);
	}
	
	public IList(Class<E> elementType, E... elements) {
		this.elementType = elementType;
		
		for (E e: elements) {
			this.internal.add(e);
		}
	}

	@Override
	public boolean add(E e) {
		Property property = new Property(this, String.valueOf(size()));
		
		boolean ret = internal.add(e);
		
		if (ret) {
			if (e instanceof IObject) {
				((IObject) e).getParentsLinksToThis().add(property);
			}
			
			if (propagate)
				propagateChange(property);
		}
		
		return ret;
	}

	@Override
	public void add(int index, E element) {
		Property property = new Property(this, String.valueOf(index));
		
		internal.add(index, element);
		
		if (element instanceof IObject) {
			((IObject) element).getParentsLinksToThis().add(property);
		}
		
		for(index = index+1; index < size(); index++) {
			E content = get(index);
			if (content instanceof IObject) {
				Property oldProperty = new Property(this, String.valueOf(index-1));
				Property newProperty = new Property(this, String.valueOf(index));
				int linkIndex = ((IObject) content).getParentsLinksToThis().indexOf(oldProperty);
				((IObject) content).getParentsLinksToThis().set(linkIndex, newProperty);
			}
		}
		
		if (propagate)
			propagateChange(new Property(this, ""));
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		propagate = false;
		for(E e: c)
			add(e);
		propagate = true;
		propagateChange(new Property(this, ""));
		
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		propagate = false;
		for(E e: c)
			add(index, e);
		propagate = true;
		propagateChange(new Property(this, ""));
		return true;
	}

	@Override
	public void clear() {
		while(!internal.isEmpty())
			remove(0);
	}

	@Override
	public boolean contains(Object o) {
		return internal.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return internal.containsAll(c);
	}

	@Override
	public E get(int index) {
		return internal.get(index);
	}
	
	public <T extends E> T get(int index, Class<T> type) {
		return (T)internal.get(index);
	}

	@Override
	public int indexOf(Object o) {
		return internal.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return internal.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return internal.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return internal.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return internal.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return internal.listIterator(index);
	}

	@Override
	public boolean remove(Object o) {
		int index = indexOf(o);
		if (index < 0) {
			return false;
		} else {
			remove(index);
			return true;
		}
	}

	@Override
	public E remove(int index) {
		Property property = new Property(this, String.valueOf(index));
		
		E oldContent = get(index);
		
		internal.remove(index);
		
		if (oldContent instanceof IObject) {
			((IObject) oldContent).getParentsLinksToThis().remove(property);
		}
		
		for(; index < size(); index++) {
			E content = get(index);
			if (content instanceof IObject) {
				Property oldProperty = new Property(this, String.valueOf(index+1));
				Property newProperty = new Property(this, String.valueOf(index));
				int linkIndex = ((IObject) content).getParentsLinksToThis().indexOf(oldProperty);
				((IObject) content).getParentsLinksToThis().set(linkIndex, newProperty);
			}
		}
		
		if (propagate)
			propagateChange(new Property(this, ""));
		
		return oldContent;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		propagate = false;
		for(Object e: c)
			remove(e);
		propagate = true;
		propagateChange(new Property(this, ""));
		
		return true;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		propagate = false;
		for(Object e: this) {
			if (!c.contains(e))
				remove(e);
		}
		propagate = true;
		propagateChange(new Property(this, ""));
		
		return true;
	}

	@Override
	public E set(int index, E element) {
		E ret = get(index);
		setContent(String.valueOf(index), element);
		return ret;
	}

	@Override
	public int size() {
		return internal.size();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return internal.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return internal.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return internal.toArray(a);
	}

	@Override
	protected Object getLocal(String propertyName) {
		if (propertyName.matches("^\\d+$")) {
			int index = Integer.parseInt(propertyName);
			if (index >= 0 && index < size())
				return get(index);
			else
				return null;
		} else if (propertyName.equals("last")) {
			if (!internal.isEmpty())
				return get(internal.size()-1);
			else
				return null;
		} else {
			return super.getLocal(propertyName);
		}
	}

	@Override
	protected void setLocal(String propertyName, Object content) {
		if (propertyName.matches("^\\d+$")) {
			internal.set(Integer.parseInt(propertyName), (E)content);
		} else {
			super.setLocal(propertyName, content);
		}
	}

	@Override
	protected List<Property> getInstanceProperties() {
		List<Property> ret = super.getInstanceProperties();
		
		for(int i = 0; i < size(); i++)
			ret.add(new Property(this, String.valueOf(i)));
		
		return ret;
	}

	@Override
	public Class<?> getContentType(String propertyName, boolean runtime) {
		if (runtime)
			return super.getContentType(propertyName, runtime);
		else
			return getElementType();
	}
	
	public Class<E> getElementType() {
		return elementType;
	}
	
	@Override
	public String toString() {
		return internal.toString();
	}

}
