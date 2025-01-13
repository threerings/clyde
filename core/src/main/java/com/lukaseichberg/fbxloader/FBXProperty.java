package com.lukaseichberg.fbxloader;

public class FBXProperty {
	
	private FBXDataType dataType;
	private Object data;
	private FBXNode parent;
	
	public FBXProperty(FBXDataType dataType, Object data, FBXNode parent) {
		this.dataType = dataType;
		this.data = data;
		this.parent = parent;
	}
	
	public FBXDataType getDataType() {
		return dataType;
	}
	
	public FBXNode getParent() {
		return parent;
	}
	
	public Object getData() {
		return data;
	}

}
