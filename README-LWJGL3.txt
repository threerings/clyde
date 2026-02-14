    LWJGL 2.9.3 → 3.3.6 Migration (Claude assist)

    LWJGL 2.9.3 → 3.3.6 Migration Summary

     ### Build compiles successfully ✅

     ### Changes across 58 files (1,075 insertions, 1,608 deletions)

     ### POM Changes (pom.xml, core/pom.xml)

     - Replaced org.lwjgl.lwjgl:lwjgl:2.9.3 and lwjgl_util with LWJGL 3 modular dependencies:
         - org.lwjgl:lwjgl:3.3.6 (core)
         - org.lwjgl:lwjgl-opengl:3.3.6 (OpenGL bindings)
         - org.lwjgl:lwjgl-openal:3.3.6 (OpenAL bindings)
         - org.lwjgl:lwjgl-glfw:3.3.6 (GLFW window/input)
         - org.lwjgl:lwjgl-stb:3.3.6 (image loading)
         - Plus platform-specific native classifier dependencies
     - Added platform detection profiles for Linux, macOS (ARM64 & x86), and Windows

     ### New Compatibility Classes (com.threerings.opengl.lwjgl2)

     1. Keyboard.java — Preserves all LWJGL 2 KEY_* constants with original numeric values (important
     for serialization), plus bidirectional GLFW↔LWJGL2 key code mapping
     2. PixelFormat.java — Replaces LWJGL 2's PixelFormat with a simple POJO holding
     alpha/depth/stencil/samples preferences
     3. WaveData.java — Replaces LWJGL 2's WaveData with a simple WAV file parser for OpenAL clip
     loading

     ### OpenGL API Migration (affects ~60 renderer/config files)

     - ARB extensions → Core GL: All ARB extension calls replaced with core OpenGL equivalents:
         - ARBMultitexture → GL13 (multitexture)
         - ARBBufferObject/ARBVertexBufferObject → GL15 (VBOs)
         - ARBShaderObjects/ARBVertexShader/ARBFragmentShader → GL20 (shaders)
         - ARBOcclusionQuery → GL15 (queries)
         - ARBTextureCubeMap/ARBTextureEnvCombine → GL13
         - ARBTextureRectangle → GL31
         - ARBTextureCompression → GL13
         - ARBDepthTexture/ARBShadow/ARBTextureMirroredRepeat → GL14
     - EXT extensions → Core GL:
         - EXTFramebufferObject → GL30 (FBOs)
         - EXTRescaleNormal → GL12
         - EXTTextureLODBias → GL14
         - EXTBgra → GL12
         - SGISGenerateMipmap → GL14
     - GL11 method renames for LWJGL 3: glLoadMatrix→glLoadMatrixf, glGetInteger→glGetIntegerv,
     glFog→glFogfv, glLight→glLightfv, glMaterial→glMaterialfv, glTexEnv→glTexEnvfv,
     glTexGen→glTexGenfv, glLightModel→glLightModelfv, glTexParameter→glTexParameterfv
     - FloatBuffer pointer methods: Added explicit GL_FLOAT type parameter for glVertexPointer,
     glTexCoordPointer, glColorPointer, glNormalPointer, glVertexAttribPointer
     - GLContext.getCapabilities() → GL.getCapabilities(), ContextCapabilities → GLCapabilities

     ### Shader System Migration

     - glCreateShaderObjectARB → GL20.glCreateShader, glCreateProgramObjectARB → GL20.glCreateProgram
     - glShaderSourceARB(ByteBuffer) → GL20.glShaderSource(CharSequence)
     - glGetObjectParameteriARB → glGetShaderi/glGetProgrami (properly split for shader vs program)
     - glGetInfoLogARB → glGetShaderInfoLog/glGetProgramInfoLog
     - glDeleteObjectARB → glDeleteShader/glDeleteProgram

     ### Window/Display Management (most significant structural change)
     - GlDisplayApp: Completely rewritten from LWJGL 2 Display class to GLFW window management
     (glfwCreateWindow, glfwSwapBuffers, glfwPollEvents, etc.)
     - DisplayCanvas: Rewritten to create a hidden GLFW window for GL context instead of using
     Display.setParent()
     - AWTCanvas: Rewritten to use GLFW context instead of AWTGLCanvas (which no longer exists)
     - GlCanvas: Interface changed from getDrawable() to getWindowHandle() (returns long)
     - GlCanvasApp: Simplified to always use DisplayCanvas (AWTGLCanvas path removed)
     - Renderer: Changed from init(Drawable, int, int) to init(long windowHandle, int, int)

     ### Input System

     - DisplayRoot: Rewritten from LWJGL 2 polled input (Keyboard.next(), Mouse.next(),
     Controllers.next()) to GLFW callback-based input with a thread-safe event queue
     - Cursor: Rewritten from org.lwjgl.input.Cursor to GLFW cursor API (glfwCreateCursor, GLFWImage)
     - ControllerEvent: Controller type parameter → Object (LWJGL 3 has no Controller class)
     - PseudoKeys: Controller methods adapted for missing Controller API

     ### Removed Features (replaced)

     - Pbuffer → FBOs only (in TextureRenderer and Renderer)
     - RenderTexture → removed (was part of Pbuffer system)
     - GLU.gluBuild2DMipmaps → glTexImage2D + GL30.glGenerateMipmap
     - Display.sync(60) → simple Thread.sleep(16) frame limiter

     ### Known Limitations / Future Work

     - The AWT/Swing integration approach creates a hidden GLFW window for GL context rather than true
     embedded rendering — functionally equivalent but may need tuning
     - Controller/joystick support is stubbed out (was using LWJGL 2's Controllers API)
     - GLFW icon setting is not yet implemented
     - Fullscreen toggling via GLFW requires window recreation (noted with TODO)
     - The nenya dependency still references org.lwjgl.util.WaveData in its Clip constructor — worked
     around by populating Clip fields directly
