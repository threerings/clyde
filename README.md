The Clyde library
=================

The Clyde library provides various facilities for creating action-oriented
networked 3D games. Its packages include:

  * config - configuration management and tools for config manipulation
  * delta - adds support for delta encoding objects on top of Narya streaming
  * editor - annotation-controlled reflective object editing
  * export - version-resilient serialization to XML and binary data formats
  * expr - expression evaluation and symbol binding
  * math - basic 2D and 3D math classes
  * openal - ties Nenya's OpenAL support into OpenGL scene handling
  * opengl - base classes for canvas and Display-based OpenGL applications
  * opengl.camera - camera representation and handling
  * opengl.compositor - view sorting and compositing, render effects
  * opengl.effect - particle systems and related effects, particle editor tool
  * opengl.geometry - static and dynamic geometry classes, software skinning
  * opengl.gui - graphical user interface (derived from JME-BUI)
  * opengl.material - support for complex materials, projections
  * opengl.model - static and articulated models, explicit and procedural animation
  * opengl.renderer - core renderer and states, wrappers for OpenGL objects
  * opengl.scene - scene representation, scene influences and viewer effects
  * probs - probabilistic data types
  * tudey - engine for environments with 2D server logic and 3D graphical representation
  * tudey.shape - general 2D shape representation and computation
  * tudey.space - 2D space representation
  * tudey.tools - scene editor
  * util - various utility classes, reflective deep copying/comparison/hashing

Clyde depends on [Narya] for distributed object management and
internationalization support; [Nenya] for resource management, image
manipulation, and basic OpenAL functionality; [Vilya] for networked scene
representation; and [LWJGL] for native bindings to OpenGL, OpenAL, and input
devices.

Documentation is somewhat sparse at the moment, but inspection of the code in
the tests/ directory shows examples of use of many features of the library.

[Javadoc documentation](http://threerings.github.com/clyde/apidocs/) is provided.

Building
--------

The library is built using Ant. Invoke Ant with any of the following targets:

    all: builds the distribution files and javadoc documentation
    compile: builds only the class files (dist/classes)
    javadoc: builds only the javadoc documentation (dist/docs)
    dist: builds the distribution jar files (dist/*.jar)

Distribution
------------

The Clyde library is released under the BSD license. The most recent version of
the library is available here:

  http://github.com/threerings/clyde/

Contributions and Contact Information
-------------------------------------

Clyde is actively developed by the scurvy dogs at Three Rings Design, Inc.
Contributions (in the form of bug reports, pull requests, etc.) are welcome.

Questions, comments, and other communications can be directed to the Google
Group for Three Rings libraries:

  http://groups.google.com/group/ooo-libs

[Narya]: http://github.com/threerings/narya/
[Nenya]: http://github.com/threerings/nenya/
[Vilya]: http://github.com/threerings/vilya/
[LWJGL]: http://www.lwjgl.org/
