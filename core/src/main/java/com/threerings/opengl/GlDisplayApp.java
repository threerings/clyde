//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl;

import java.awt.Dimension;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryUtil;

import com.samskivert.util.RunAnywhere;
import com.samskivert.util.RunQueue;

import com.threerings.opengl.gui.DisplayRoot;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.lwjgl2.DisplayMode;

import static com.threerings.opengl.Log.log;

/**
 * A base class for applications that use a GLFW window for display.
 */
public abstract class GlDisplayApp extends GlApp
{
  public GlDisplayApp ()
  {
    _vsync = !Boolean.getBoolean("no_vsync");
  }

  /**
   * Returns the GLFW window handle.
   */
  public long getWindow ()
  {
    return _window;
  }

  /**
   * Returns the current window width in pixels (framebuffer size).
   */
  public int getWindowWidth ()
  {
    return _fbWidth;
  }

  /**
   * Returns the current window height in pixels (framebuffer size).
   */
  public int getWindowHeight ()
  {
    return _fbHeight;
  }

  /**
   * Returns whether the window is currently fullscreen.
   */
  public boolean isFullscreen ()
  {
    return _window != MemoryUtil.NULL &&
      GLFW.glfwGetWindowMonitor(_window) != MemoryUtil.NULL;
  }

  /**
   * Returns whether the window is currently active (focused).
   */
  public boolean isActive ()
  {
    return _window != MemoryUtil.NULL &&
      GLFW.glfwGetWindowAttrib(_window, GLFW.GLFW_FOCUSED) != 0;
  }

  /**
   * Returns whether the display/window has been created.
   */
  public boolean isCreated ()
  {
    return _window != MemoryUtil.NULL;
  }

  /**
   * Returns whether the window was resized since the last call.
   */
  public boolean wasResized ()
  {
    boolean resized = _wasResized;
    _wasResized = false;
    return resized;
  }

  /**
   * Sets whether the window is resizable.
   */
  public void setResizable (boolean resizable)
  {
    _resizable = resizable;
    if (_window != MemoryUtil.NULL) {
      GLFW.glfwSetWindowAttrib(_window, GLFW.GLFW_RESIZABLE,
        resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    }
  }

  /**
   * Sets the window title.
   */
  public void setTitle (String title)
  {
    _title = title;
    if (_window != MemoryUtil.NULL) {
      GLFW.glfwSetWindowTitle(_window, title);
    }
  }

  /**
   * Sets whether vsync is enabled.
   */
  public void setVSyncEnabled (boolean enabled)
  {
    _vsync = enabled;
    if (_window != MemoryUtil.NULL) {
      GLFW.glfwMakeContextCurrent(_window);
      GLFW.glfwSwapInterval(enabled ? 1 : 0);
    }
  }

  /**
   * Synchronizes the frame rate to the given FPS. Negative values are no-ops.
   */
  public void sync (int fps)
  {
    if (fps <= 0) return;
    long targetNanos = 1_000_000_000L / fps;
    long elapsed = System.nanoTime() - _lastFrameTime;
    long sleepNanos = targetNanos - elapsed;
    if (sleepNanos > 0) {
      try {
        Thread.sleep(sleepNanos / 1_000_000, (int)(sleepNanos % 1_000_000));
      } catch (InterruptedException e) {
        // ignore
      }
    }
    _lastFrameTime = System.nanoTime();
  }

  protected void initSharedManagers ()
  {
    _mainQueue = new ConcurrentLinkedQueue<>();
    _mainRunQueue = new RunQueue() {
      @Override public void postRunnable (Runnable r) {
        _mainQueue.add(r);
      }
      @Override public boolean isDispatchThread () {
        return Thread.currentThread() == _mainThread;
      }
      @Override public boolean isRunning () {
        // Always return true: the queue can always accept tasks (they'll be
        // processed once mainLoop starts). Returning false even once causes
        // samskivert Interval to permanently cancel itself.
        return true;
      }
    };

    super.initSharedManagers();
  }

  /**
   * Ensures GLFW is initialized (safe to call multiple times).
   */
  protected void ensureGlfwInit ()
  {
    if (_glfwInited) return;
    // On macOS, use the async GLFW build that allows AWT/Swing to coexist
    // with GLFW. This avoids glfwPollEvents hanging when AWT is initialized.
    if (RunAnywhere.isMacOS()) {
      java.awt.Toolkit.getDefaultToolkit(); // tickle the awt prior to init
      Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
    }
    // On macOS, tell GLFW not to change the working directory to the
    // .app bundle Resources dir (we manage resources ourselves).
    GLFW.glfwInitHint(GLFW.GLFW_COCOA_CHDIR_RESOURCES, GLFW.GLFW_FALSE);
    if (!GLFW.glfwInit()) {
      log.warning("Failed to initialize GLFW.");
      return;
    }
    _glfwInited = true;
    GLFW.glfwPollEvents();
  }

  @Override
  public float getPixelScaleFactor ()
  {
    if (_scale > 0) return _scale;
    ensureGlfwInit();
    long monitor = GLFW.glfwGetPrimaryMonitor();
    if (monitor == MemoryUtil.NULL) return 1f;
    float[] sx = new float[1], sy = new float[1];
    GLFW.glfwGetMonitorContentScale(monitor, sx, sy);
    return _scale = sx[0];
  }

  @Override
  public float getWindowScaleFactor ()
  {
    if (_window != MemoryUtil.NULL) {
      int[] fw = { 0 }, ww = { 0 };
      GLFW.glfwGetFramebufferSize(_window, fw, null);
      GLFW.glfwGetWindowSize(_window, ww, null);
      if (ww[0] > 0) return (float)fw[0] / ww[0];
    }
    return getPixelScaleFactor();
  }

  /**
   * Gets the desktop display mode via GLFW.
   * Dimensions are in pixels (physical resolution).
   */
  public DisplayMode getDesktopDisplayMode ()
  {
    ensureGlfwInit();
    long monitor = GLFW.glfwGetPrimaryMonitor();
    if (monitor != MemoryUtil.NULL) {
      GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
      if (vidMode != null) {
        float scale = getWindowScaleFactor();
        return new DisplayMode(Math.round(vidMode.width() * scale),
          Math.round(vidMode.height() * scale), vidMode.refreshRate(), true);
      }
    }
    return new DisplayMode(1024, 768); // make something up... ouch!
  }

  /**
   * Gets all available display modes.
   * Dimensions are in pixels (physical resolution).
   */
  public DisplayMode[] getAvailableDisplayModes ()
  {
    ensureGlfwInit();
    long monitor = GLFW.glfwGetPrimaryMonitor();
    if (monitor == MemoryUtil.NULL) return new DisplayMode[0];
    GLFWVidMode.Buffer vidModes = GLFW.glfwGetVideoModes(monitor);
    if (vidModes == null) return new DisplayMode[0];
    DisplayMode[] modes = new DisplayMode[vidModes.limit()];
    for (int ii = 0; ii < vidModes.limit(); ii++) {
      vidModes.position(ii);
      // But, for some reason, these modes are full pixel? I am confused.
      modes[ii] = new DisplayMode(
        vidModes.width(), vidModes.height(), vidModes.refreshRate(), true);
    }
    return modes;
  }

  /**
   * Sets the display mode and fullscreen state.
   */
  public void setDisplayModeAndFullscreen (DisplayMode mode)
  {
    if (_window == MemoryUtil.NULL) {
      _pendingMode = mode;
      return;
    }
    boolean wasFullscreen = GLFW.glfwGetWindowMonitor(_window) != MemoryUtil.NULL;
    if (mode.fullscreen) {
      // Fullscreen: pixel dimensions match monitor video mode directly
      long monitor = GLFW.glfwGetPrimaryMonitor();
      GLFW.glfwSetWindowMonitor(_window, monitor, 0, 0,
        mode.width, mode.height,
        mode.frequency > 0 ? mode.frequency : GLFW.GLFW_DONT_CARE);
    } else {
      // Windowed: glfwSetWindowMonitor's size args are screen coords, so we divide pixel
      // dims by the fb/win ratio of the target (windowed) state.
      //
      // Wrinkle: on macOS, glfwGetMonitorContentScale reflects the *current* video mode,
      // not a static monitor property. While fullscreen at a non-native mode (e.g. 1920x1080
      // on a Retina display) it reports 1.0, since the mode is already at pixel resolution.
      // If we query it mid-transition, we'd use 1.0 and end up with a 2x-too-big window once
      // macOS restores the native mode.
      //
      // So: exit fullscreen first with a placeholder size, pump events so macOS restores the
      // native video mode and the real content scale, drop the cached scale, then do the
      // actual resize using a freshly-queried value.
      if (wasFullscreen) {
        GLFW.glfwSetWindowMonitor(_window, MemoryUtil.NULL, 0, 0,
          100, 100, GLFW.GLFW_DONT_CARE);
        GLFW.glfwPollEvents();
        _scale = 0; // invalidate stale cache now that the mode is restored
      }
      float scale = getPixelScaleFactor();
      int winW = Math.round(mode.width / scale);
      int winH = Math.round(mode.height / scale);

      // Where to position the window: if we're resizing a windowed window, keep its
      // current top-left. If we're coming out of fullscreen, center on the primary monitor.
      int xpos, ypos;
      if (wasFullscreen) {
        int[] wx = { 0 }, wy = { 0 }, ww = { 0 }, wh = { 0 };
        GLFW.glfwGetMonitorWorkarea(GLFW.glfwGetPrimaryMonitor(), wx, wy, ww, wh);
        xpos = wx[0] + (ww[0] - winW) / 2;
        ypos = wy[0] + (wh[0] - winH) / 2;
      } else {
        int[] x = { 0 }, y = { 0 };
        GLFW.glfwGetWindowPos(_window, x, y);
        xpos = x[0];
        ypos = y[0];
      }
      GLFW.glfwSetWindowMonitor(_window, MemoryUtil.NULL,
        xpos, ypos, winW, winH, GLFW.GLFW_DONT_CARE);
    }
    GLFW.glfwSwapInterval(_vsync ? 1 : 0);
    updateRendererSize();
  }

  /**
   * Returns the OpenGL adapter/renderer string (replaces Display.getAdapter()).
   */
  public String getAdapter ()
  {
    return org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
  }

  /**
   * Sets the display icon.
   *
   * @param paths the resource paths of the icons to set.
   */
  public void setIcon (String... paths)
  {
    // TODO: GLFW icon support (differs from LWJGL 2)
  }

  // documentation inherited from interface GlContext
  public void makeCurrent ()
  {
    GLFW.glfwMakeContextCurrent(_window);
  }

  @Override
  public RunQueue getRunQueue ()
  {
    return _mainRunQueue;
  }

  @Override
  public Root createRoot ()
  {
    if (_displayRoot == null) {
      _displayRoot = new DisplayRoot(this);
    }
    return _displayRoot;
  }

  @Override
  public void startup ()
  {
    // On macOS with -XstartOnFirstThread, main() IS the AppKit main thread.
    // GLFW requires all window/event operations on this thread.
    _mainThread = Thread.currentThread();
    _running = true;
    init();
    if (_window == MemoryUtil.NULL) {
      _running = false;
      return;
    }
    GLFW.glfwPostEmptyEvent();
    mainLoop(); // holds this thread!
    performShutdown();
  }

  @Override
  public void shutdown ()
  {
    _running = false;
  }

  /**
   * Performs the actual shutdown cleanup. Called from the main loop thread.
   */
  protected void performShutdown ()
  {
    willShutdown();
    if (_window != MemoryUtil.NULL) {
      GLFW.glfwDestroyWindow(_window);
      _window = MemoryUtil.NULL;
      _scale = 0;
    }
    GLFW.glfwTerminate();
    System.exit(0);
  }

  @Override
  protected void init ()
  {
    if (!createDisplay()) {
      log.warning("Failed to create display.");
      return;
    }
    super.init();
  }

  @Override
  protected void didInit ()
  {
    // Nothing to do here — the main loop is started by startup() after init().
  }

  /**
   * The main render/event loop. Runs on the main thread.
   */
  protected void mainLoop ()
  {
    // Pump events before showing the window so macOS NSApplication is ready,
    // then show the window. Showing before the first poll blocks on macOS.
    GLFW.glfwPollEvents();
    GLFW.glfwShowWindow(_window);

    while (_running) {
      if (GLFW.glfwWindowShouldClose(_window)) {
        _running = false;
        break;
      }

      GLFW.glfwPollEvents();

      // Process any queued tasks
      Runnable task;
      while ((task = _mainQueue.poll()) != null) {
        try {
          task.run();
        } catch (Exception e) {
          log.warning("Error in queued task.", e);
        }
      }

      updateFrame();
    }
  }

  @Override
  protected void willShutdown ()
  {
    if (_displayRoot != null) {
      _displayRoot.dispose();
      _displayRoot = null;
    }
    super.willShutdown();
  }

  @Override
  protected void initRenderer ()
  {
    int[] w = new int[1], h = new int[1];
    GLFW.glfwGetFramebufferSize(_window, w, h);
    _fbWidth = w[0];
    _fbHeight = h[0];
    _renderer.init(_fbWidth, _fbHeight);
  }

  /**
   * Update the renderer size after a mode change.
   */
  protected void updateRendererSize ()
  {
    if (_window != MemoryUtil.NULL) {
      _renderer.setSize(_fbWidth, _fbHeight);
    }
  }

  /**
   * Return the size to use for rendering.
   */
  protected Dimension calcRendererSize ()
  {
    return new Dimension(_fbWidth, _fbHeight);
  }

  /**
   * Creates the GLFW window with one of the supported pixel formats.
   *
   * @return true if successful.
   */
  protected boolean createDisplay ()
  {
    ensureGlfwInit();
    if (!_glfwInited) return false;

    GLFW.glfwDefaultWindowHints();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, _resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);

    // _pendingMode dimensions are in pixels. For fullscreen, glfwCreateWindow wants
    // pixel dimensions matching a monitor video mode, with a non-null monitor handle.
    // For windowed, it wants screen coordinates (pixels / content scale) and a null
    // monitor handle.
    boolean fs = _pendingMode != null && _pendingMode.fullscreen;
    long monitor = fs ? GLFW.glfwGetPrimaryMonitor() : MemoryUtil.NULL;
    int initWidth, initHeight;
    if (_pendingMode != null && fs) {
      initWidth = _pendingMode.width;
      initHeight = _pendingMode.height;
      if (_pendingMode.frequency > 0) {
        GLFW.glfwWindowHint(GLFW.GLFW_REFRESH_RATE, _pendingMode.frequency);
      }
    } else if (_pendingMode != null) {
      float scale = getPixelScaleFactor();
      initWidth = Math.round(_pendingMode.width / scale);
      initHeight = Math.round(_pendingMode.height / scale);
    } else {
      // guess a default, bigger for retina screens
      float scale = getPixelScaleFactor();
      initWidth = Math.round(1024 * scale);
      initHeight = Math.round(768 * scale);
    }
    _window = GLFW.glfwCreateWindow(initWidth, initHeight,
      _title != null ? _title : "Clyde", monitor, MemoryUtil.NULL);
    _scale = 0;
    if (_window == MemoryUtil.NULL) {
      log.warning("Couldn't create window!");
      return false;
    }
    _pendingMode = null;
    GLFW.glfwMakeContextCurrent(_window);
    GL.createCapabilities();
    GLFW.glfwSwapInterval(_vsync ? 1 : 0);
    GLFW.glfwSetFramebufferSizeCallback(_window, (win, w, h) -> {
      _fbWidth = w;
      _fbHeight = h;
      _wasResized = true;
    });
    GLFW.glfwPollEvents();
    return true;
  }

  /**
   * Updates and renders a single frame.
   */
  protected void updateFrame ()
  {
    try {
      // glfwPollEvents() is called at the top of the main loop, before task processing.
      updateView();
      // Check if window is visible/not iconified
      if (GLFW.glfwGetWindowAttrib(_window, GLFW.GLFW_VISIBLE) != 0) {
        renderView();
      }
      GLFW.glfwSwapBuffers(_window);

    } catch (Exception e) {
      log.warning("Caught exception in frame loop.", e);
    }
  }


  /** Whether GLFW has been initialized. */
  protected boolean _glfwInited;

  /** The GLFW window handle. */
  protected long _window = MemoryUtil.NULL;

  /** Our cached screen scale, probably 1 or 2, or 0 if not yet set. */
  protected float _scale;

  /** The window title (stored so it can be set before window creation). */
  protected String _title;

  /** Display mode requested before window creation (applied at create time). */
  protected DisplayMode _pendingMode;

  /** Whether vsync is enabled. */
  protected boolean _vsync;

  /** Whether the window should be resizable. */
  protected boolean _resizable = true;

  /** Whether the window was resized. */
  protected volatile boolean _wasResized;

  /** Cached framebuffer dimensions, updated from the framebuffer size callback. */
  protected volatile int _fbWidth, _fbHeight;

  /** Whether the main loop is running. */
  protected volatile boolean _running;

  /** Last frame time for sync(). */
  protected long _lastFrameTime = System.nanoTime();

  /** Our root. */
  protected Root _displayRoot;

  /** Queue of tasks to run on the main/GL thread. */
  protected ConcurrentLinkedQueue<Runnable> _mainQueue;

  /** A RunQueue that posts to the main/GL thread. */
  protected RunQueue _mainRunQueue;

  /** The main thread reference for RunQueue.isDispatchThread(). */
  protected Thread _mainThread = Thread.currentThread();
}
