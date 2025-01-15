Java FBX Loader
=============

**Java FBX Loader** is a simple Java library to load binary FBX files and extract their data.

##### Example:

```java
// Example: extracting vertices from a FBX file
FBXFile file = null;
try {
    file = FBXLoader.loadFBXFile("res/Model.fbx");
} catch (IOException e) {
    e.printStackTrace();
}

FBXNode root = file.getRootNode();
FBXNode verticesNode = root.getNodeFromPath("Objects/Geometry/Vertices");
FBXProperty property = verticesNode.getProperty(0);

if (property.getDataType() != FBXDataType.DOUBLE_ARRAY) {
    System.err.println("Unexpected data type!");
    System.exit(-1);
}

double[] vertices = (double[]) property.getData();
```

**Note that this is just a very basic example. FBX files can contain multiple *Geometry* nodes and be a lot more complex in structure.**

Download library as jar file [here](https://raw.githubusercontent.com/lukaseichberg/Java-FBX-Loader/main/jar/java-fbx-loader-v1.0.jar).


Documentation
=============

Contents
---------------------

- [Class FBXLoader](#class-fbxloader)
  - [Functions](#functions)
    - [loadFBXFile(String filePath)](#loadfbxfilestring-filepath)
- [Class FBXFile](#class-fbxfile)
  - [Functions](#functions-1)
    - [getFilePath()](#getfilepath)
    - [getRootNode()](#getrootnode)
    - [getVersion()](#getversion)
- [Class FBXNode](#class-fbxnode)
  - [Functions](#functions-2)
    - [getChild(int index)](#getchildint-index)
    - [getChildByName(String name)](#getchildbynamestring-name)
    - [getChildrenByName(String name)](#getchildrenbynamestring-name)
    - [getName()](#getname)
    - [getNodeFromPath(String path)](#getnodefrompathstring-path)
    - [getNumChildren()](#getnumchildren)
    - [getNumProperties()](#getnumproperties)
    - [getParent()](#getparent)
    - [getProperty(int index)](#getpropertyint-index)
- [Class FBXProperty](#class-fbxproperty)
  - [Functions](#functions-3)
    - [getData()](#getdata)
    - [getDataType()](#getdatatype)
    - [getParent()](#getparent-1)
- [Enum FBXDataType](#enum-fbxdatatype)
  - [Members](#members)
  - [Functions](#functions-4)
    - [isArray()](#isarray)
    - [isCategory(FBXDataCategory category)](#iscategoryfbxdatacategory-category)
- [Enum FBXDataCategory](#enum-fbxdatacategory)
  - [Membes](#members-1)



## Class FBXLoader

### Functions

| Type   | Function                     | Return Type | Description      |
| ------ | ---------------------------- | ----------- | ---------------- |
| Static | loadFBXFile(String filePath) | FBXFile     | Loads a FBX File |



#### loadFBXFile(String filePath)

Loads and processes a FBX file and returns **FBXFile**.

##### Example:

```java
FBXFile file = null;
try {
    file = FBXLoader.loadFBXFile("your_path/your_fbx_file.fbx");
} catch (IOException e) {
    e.printStackTrace();
}
// code using file here
```



## Class FBXFile

### Functions

| Type     | Function      | Return Type | Description                    |
| -------- | ------------- | ----------- | ------------------------------ |
| Instance | getFilePath() | String      | returns the absolute file path |
| Instance | getRootNode() | FBXNode     | returns the root node          |
| Instance | getVersion()  | int         | returns the FBX version        |



#### getFilePath()

Returns the absolute file path as a **String**.

##### Example:

```java
String filePath = file.getFilePath();
```



#### getRootNode()

Returns the root node as a **FBXNode**.

##### Example:

```java
FBXNode rootNode = file.getRootNode();
```



#### getVersion()

Returns the FBX version as a **int**.

##### Example:

```java
// Example: Check if FBX version used in file is 7400 (v 7.4)
int version = file.getVersion();

if (version != 7400) {
    System.err.println("Not the version I want!");
    System.exit(-1);
}
```





Class FBXNode
---------

**A node can have multiple children with the same name!**

### Functions

| Type     | Function                       | Return Type     | Description                                                  |
| -------- | ------------------------------ | --------------- | ------------------------------------------------------------ |
| Instance | getChild(int index)            | FBXNode         | returns the node at the specified index                      |
| Instance | getChildByName(String name)    | FBXNode \| null | if exists, returns the first found node with the specified name |
| Instance | getChildrenByName(String name) | List\<FBXNode\> | returns a list of nodes with the specified name              |
| Instance | getName()                      | String          | returns the name of the node                                 |
| Instance | getNodeFromPath(String path)   | FBXNode \| null | if found, returns the node at the specified path             |
| Instance | getNumChildren()               | int             | returns the number of child nodes                            |
| Instance | getNumProperties()             | int             | returns the number of properties                             |
| Instance | getParent()                    | FBXNode         | returns the parent node                                      |
| Instance | getProperty(int index)         | FBXProperty     | returns the property at the specified index                  |



#### getChild(int index)

Returns a child node at the specified index as **FBXNode**.

##### Example:

```java
// Example: looping though all direct children of a fbx file
int count = rootNode.getNumChildren();

for (int i = 0; i < count; i++) {
    FBXNode child = rootNode.getChild(i);	// get a child node at index i
}
```



#### getChildByName(String name)

Returns the first found child node with the specified name. If found, returns **FBXNode**. If not found, returns **null**.

##### Example:

```java
// get the "Objects" node from the root node
FBXNode objectsNode = rootNode.getChildByName("Objects");
```



#### getChildrenByName(String name)

Returns a list of child nodes with the specified name as **List\<FBXNode\>**.

##### Example:

```java
// get all "Geometry" nodes from the "Objects" node
List<FBXNode> geometryNodes = objectNode.getChildrenByName("Geometry");

for (FBXNode node:geometryNodes) {
    // process "Geometry" nodes one by one
}
```



#### getName()

Returns the name of the node as **String**.

##### Example:

```java
// Example: process nodes based on their name
int count = rootNode.getNumChildren();

for (int i = 0; i < count; i++) {
    FBXNode node = rootNode.getChild(i);
    String name = node.getName();			// get the name of the node
    
    switch (name) {
        case "Document":
            // ...process documents node...
            break;
            
        case "Definitions":
            // ...process definitions node...
            break;
            
        case "Objects":
            // ...process objects node...
            break;
    }
}
```



#### getNodeFromPath(String path)

Returns the node at the specified path. If found, returns **FBXNode**. If not found returns **null**.
Path is made up of node names separated by "/".

##### Example:

```java
FBXNode verticesNode = root.getNodeFromPath("Objects/Geometry/Vertices");
// is the same as
FBXNode verticesNode = root
    .getChildByName("Objects")
    .getChildByName("Geometry")
    .getChildByName("Vertices");
```



#### getNumChildren()

Returns the number of child nodes as **int**.

##### Example:

```java
// Example: looping though all direct children of a fbx file
int count = rootNode.getNumChildren();	// get number of all direct children

for (int i = 0; i < count; i++) {
    FBXNode child = rootNode.getChild(i);
}
```



#### getNumProperties()

Returns the number of properties as **int**.

##### Example:

```java
// Example: looping through all properties of a node
int count = node.getNumPorperties();	// get number of properties of a node

for (int i = 0; i < count; i++) {
    FBXProperty property = node.getProperty(i);
}
```



#### getParent()

Returns the parent node as **FBXNode**. Root always returns **null**.

##### Example:

```java
FBXNode parent = node.getParent();
```



#### getProperty(int index)

Returns the property at the specified index as **FBXProperty**.

##### Example:

```java
// Example: looping through all properties of a node
int count = node.getNumPorperties();

for (int i = 0; i < count; i++) {
    FBXProperty property = node.getProperty(i);		// get property of a node at index i
}
```





Class FBXProperty
--------------

### Functions

| Type     | Function      | Return Type | Description             |
| -------- | ------------- | ----------- | ----------------------- |
| Instance | getData()     | Object      | returns the value/data  |
| Instance | getDataType() | FBXDataType | returns the data type   |
| Instance | getParent()   | FBXNode     | returns the parent node |



#### getData()

Returns the data as **Object**. The data has to be casted to it's proper data type for use.
Data type should be checked before casting.

##### Example:

```java
FBXProperty p0 = node.getProperty(0);
FBXProperty p1 = node.getProperty(1);

//	...check data types here...

int value = (int) p0.getData();                 // get data of property 0 as 'int'
float[] floatArray = (float[]) p1.getData();    // get data of property 1 as 'float[]'
```



#### getDataType()

Returns the data type as **FBXDataType** Enum. This can be used to check if FBXProperty has the expected data type before casting data.

##### Example:

```java
FBXProperty property = node.getProperty(0);

// checking if property is the expected data type (FLOAT_ARRAY)
if (property.getDataType() != FBXDataType.FLOAT_ARRAY) {
    // error handling
    System.err.println("Unexpected data type!");
    System.exit(-1);
}

// checks passed. continue
```



#### getParent()

Returns the parent node as **FBXNode**.

##### Example:

```java
FBXNode parent = property.getParent();
```



Enum FBXDataType
----------------

### Members

| Member        | Byte Size (internal use) | FBXDataCategory |
| ------------- | -------------------------| --------------- |
| BOOLEAN       | 1                        | BASIC           |
| DOUBLE        | 8                        | BASIC           |
| FLOAT         | 4                        | BASIC           |
| INT           | 4                        | BASIC           |
| LONG          | 8                        | BASIC           |
| SHORT         | 2                        | BASIC           |
| BOOLEAN_ARRAY | 1                        | ARRAY           |
| DOUBLE_ARRAY  | 8                        | ARRAY           |
| FLOAT_ARRAY   | 4                        | ARRAY           |
| INT_ARRAY     | 4                        | ARRAY           |
| LONG_ARRAY    | 8                        | ARRAY           |
| RAW           | 0 (arbitrary)            | SPECIAL         |
| STRING        | 0 (arbitrary)            | SPECIAL         |



### Functions

| Function                             | Return Type | Description                                            |
| ------------------------------------ | ----------- | ------------------------------------------------------ |
| isArray()                            | boolean     | returns true if the data type is of category ARRAY     |
| isCategory(FBXDataCategory category) | boolean     | returns true if the data type is of specified category |



#### isArray()

Returns **true** if the data type is of category ARRAY.

##### Example:

```java
FBXDataType dataType = property.getDataType();
boolean isArray = dataType.isArray();
// is the same as
boolean isArray = dataType.isCategory(FBXDataCategory.ARRAY);
```


#### isCategory(FBXDataCategory category)

Returns **true** if the data type is of specified category.

##### Example:

```java
FBXDataType dataType = property.getDataType();
boolean isBasicData = dataType.isCategory(FBXDataCategory.BASIC);
```



Enum FBXDataCategory
------

### Members

|Member|
|------|
|BASIC|
|ARRAY|
|SPECIAL|

