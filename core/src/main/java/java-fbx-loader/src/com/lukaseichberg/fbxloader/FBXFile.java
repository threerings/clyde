package com.lukaseichberg.fbxloader;

public class FBXFile {
	
	private String filePath;
	private int version;
	private FBXNode root;
	
	FBXFile(String filePath, int version, FBXNode root) {
		this.filePath = filePath;
		this.version = version;
		this.root = root;
	}

	public String getFilePath() {
		return filePath;
	}
	
	public FBXNode getRootNode() {
		return root;
	}
	
	public int getVersion() {
		return version;
	}

}
