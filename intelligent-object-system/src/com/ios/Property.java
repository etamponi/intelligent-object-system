package com.ios;



public class Property {
	
	public static final String ANY = "*";

	private final IntelligentObject root;
	
	private final String path;
	
	@SuppressWarnings("unused")
	private Property() {
		root = null;
		path = null;
	}
	
	public Property(IntelligentObject root, String path) {
		this.root = root;
		this.path = path;
	}
	
	public Object getContent() {
		return root.get(path);
	}
	
	public <T> T getContent(Class<T> contentType) {
		return root.get(path, contentType);
	}
	
	public void setContent(Object content) {
		root.set(path, content);
	}
	
	public IntelligentObject getRoot() {
		return root;
	}
	
	public String getPath() {
		return path;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Property) {
			Property other = (Property)o;
			return this.root == other.root && this.path.equals(other.path);
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return "<"+root+">" + (path.isEmpty() ? "" : "." + path) + " = " + getContent();
	}
	
	public boolean isParent(Property complete) {
		return isPrefix(complete, true);
	}
	
	public boolean isPrefix(Property complete, boolean parentOnly) {
		if (this.root != complete.root)
			return false;

		String[] prefixTokens = this.path.split("\\.");
		String[] completeTokens = complete.path.split("\\.");
		if (!this.path.isEmpty()) {
			
			if (prefixTokens.length > completeTokens.length)
				return false;
			
			for(int i = 0; i < prefixTokens.length; i++) {
				if (completeTokens[i].equals(ANY))
					continue;
				if (!prefixTokens[i].equals(completeTokens[i]))
					return false;
			}
			
			return parentOnly ? prefixTokens.length == completeTokens.length-1 : true;
		} else {
			return parentOnly ? completeTokens.length == 1 : true;
		}
	}
	
	public String getLastPart() {
		if (!path.contains("."))
			return path;
		
		return path.substring(path.lastIndexOf('.'));
	}
	
}
