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

	/**
	 * Infer the type for a piece of property data, for building documents to write.
	 */
	public static FBXDataType typeFor (Object data) {
		return switch (data) {
			case Short _ -> SHORT;
			case Boolean _ -> BOOLEAN;
			case Integer _ -> INT;
			case Float _ -> FLOAT;
			case Double _ -> DOUBLE;
			case Long _ -> LONG;
			case float[] _ -> FLOAT_ARRAY;
			case double[] _ -> DOUBLE_ARRAY;
			case long[] _ -> LONG_ARRAY;
			case int[] _ -> INT_ARRAY;
			case boolean[] _ -> BOOLEAN_ARRAY;
			case byte[] _ -> RAW;
			case String _ -> STRING;
			case null -> throw new IllegalArgumentException("Property data may not be null");
			default -> throw new IllegalArgumentException(
					"No FBX data type for: " + data.getClass().getName());
		};
	}

	public boolean isArray() {
		return category == FBXDataCategory.ARRAY;
	}

}
