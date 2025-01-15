package com.lukaseichberg.fbxloader;

public enum FBXDataType {
	SHORT(2, FBXDataCategory.BASIC),
	BOOLEAN(1, FBXDataCategory.BASIC),
	INT(4, FBXDataCategory.BASIC),
	FLOAT(4, FBXDataCategory.BASIC),
	DOUBLE(8, FBXDataCategory.BASIC),
	LONG(8, FBXDataCategory.BASIC),
	FLOAT_ARRAY(4, FBXDataCategory.ARRAY),
	DOUBLE_ARRAY(8, FBXDataCategory.ARRAY),
	LONG_ARRAY(8, FBXDataCategory.ARRAY),
	INT_ARRAY(4, FBXDataCategory.ARRAY),
	BOOLEAN_ARRAY(1, FBXDataCategory.ARRAY),
	RAW(0, FBXDataCategory.SPECIAL),
	STRING(0, FBXDataCategory.SPECIAL);

	private final int size;
	private final FBXDataCategory category;
	
	private FBXDataType(int size, FBXDataCategory category) {
		this.size = size;
		this.category = category;
	}
	
	int size() {
		 return size;
	}
	
	FBXDataCategory category() {
		return category;
	}
	
	public boolean isCategory(FBXDataCategory category) {
		return this.category == category;
	}
	
	public boolean isArray() {
		return category == FBXDataCategory.ARRAY;
	}
	
}
