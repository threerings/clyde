package com.lukaseichberg.fbxloader;

import java.util.ArrayList;
import java.util.List;

public class FBXNode {
	
	private String name;
	private FBXNode parent;
	private List<FBXNode> children;
	private List<FBXProperty> properties;
	
	FBXNode(String name, FBXNode parent) {
		this.name = name;
		this.parent = parent;
		children = new ArrayList<>();
		properties = new ArrayList<>();
	}
	
	void add(FBXNode node) {
		children.add(node);
	}
	
	void add(FBXProperty property) {
		properties.add(property);
	}
	
	public FBXNode getParent() {
		return parent;
	}
	
	public FBXNode getChild(int index) {
		return children.get(index);
	}

	public FBXNode getNodeFromPath(String path) {
		String[] name = path.split("/");
		return getFromPath(name, 0);
	}
	
	FBXNode getFromPath(String[] path, int level) {
		FBXNode child = getChildByName(path[level]);
		if (child != null) {
			if (level < path.length - 1) {
				return child.getFromPath(path, level + 1);
			} else {
				return child;
			}
		}
		return null;
	}
	
	public FBXNode getChildByName(String name) {
		for (FBXNode child:children) {
			if (child.getName().equals(name)) {
				return child;
			}
		}
		return null;
	}
	
//	public List<FBXNode> getChildrenByProperty(FBXDataType dataType, Object value) {
//		List<FBXNode> nodes = new ArrayList<>();
//		for (FBXNode child:children) {
//			int properties = child.getNumProperties();
//			for (int i = 0; i < properties; i++) {
//				FBXProperty property = child.getProperty(i);
//				if (property.getDataType() == dataType) {
//					if (property.getData() == value) {
//						
//					}
//				}
//			}
//		}
//		return nodes;
//	}
	
	public List<FBXNode> getChildrenByName(String name) {
		List<FBXNode> nodes = new ArrayList<>();
		for (FBXNode child:children) {
			if (child.getName().equals(name)) {
				nodes.add(child);
			}
		}
		return nodes;
	}
	
	public int getNumChildren() {
		return children.size();
	}
	
	public FBXProperty getProperty(int index) {
		return properties.get(index);
	}
	
	public int getNumProperties() {
		return properties.size();
	}
	
	public String getName() {
		return name;
	}

}
