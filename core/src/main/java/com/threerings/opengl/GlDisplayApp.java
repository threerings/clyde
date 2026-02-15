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
import java.awt.EventQueue;
import java.awt.image.BufferedImage;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import com.samskivert.util.RunAnywhere;
import com.samskivert.util.RunQueue;

import com.threerings.opengl.gui.DisplayRoot;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.lwjgl2.DisplayMode;
import com.threerings.opengl.lwjgl2.PixelFormat;

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
   * Returns the current window width.
   */
  public int getWindowWidth ()
  {
    int[] w = new int[1], h = new int[1];
    GLFW.glfwGetWindowSize(_window, w, h);
    return w[0];
  }

  /**
   * Returns the current window height.
   */
  public int getWindowHeight ()
  {
    int[] w = new int[1], h = new int[1];
    GLFW.glfwGetWindowSize(_window, w, h);
    return h[0];
  }

  /**
   * Sets the window size.
   */
  public void setWindowSize (int width, int height)
  {
    GLFW.glfwSetWindowSize(_window, width, height);
    updateRendererSize();
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
   * Sets the fullscreen mode.
   */
  public void setFullscreen (boolean fullscreen)
  {
    if (_window == MemoryUtil.NULL) return;
    long monitor = GLFW.glfwGetPrimaryMonitor();
    if (fullscreen) {
      GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
      GLFW.glfwSetWindowMonitor(_window, monitor, 0, 0,
        vidMode.width(), vidMode.height(), vidMode.refreshRate());
    } else {
      GLFW.glfwSetWindowMonitor(_window, MemoryUtil.NULL,
        100, 100, 800, 600, GLFW.GLFW_DONT_CARE);
    }
    GLFW.glfwSwapInterval(_vsync ? 1 : 0);
    updateRendererSize();
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

  /**
   * Ensures GLFW is initialized (safe to call multiple times).
   */
  protected void ensureGlfwInit ()
  {
    if (!_glfwInited) {
      if (!GLFW.glfwInit()) {
        log.warning("Failed to initialize GLFW.");
        return;
      }
      _glfwInited = true;
    }
  }

  /**
   * Gets the desktop display mode via GLFW.
   */
  public DisplayMode getDesktopDisplayMode ()
  {
    ensureGlfwInit();
    long monitor = GLFW.glfwGetPrimaryMonitor();
    if (monitor == MemoryUtil.NULL) {
      return new DisplayMode(1024, 768);
    }
    GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
    if (vidMode == null) {
      return new DisplayMode(1024, 768);
    }
    return new DisplayMode(vidMode.width(), vidMode.height(),
      vidMode.redBits() + vidMode.greenBits() + vidMode.blueBits(),
      vidMode.refreshRate(), true);
  }

  /**
   * Gets all available display modes.
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
      int bpp = vidModes.redBits() + vidModes.greenBits() + vidModes.blueBits();
      modes[ii] = new DisplayMode(
        vidModes.width(), vidModes.height(), bpp, vidModes.refreshRate(), true);
    }
    return modes;
  }

  /**
   * Sets the display mode and fullscreen state.
   */
  public void setDisplayModeAndFullscreen (DisplayMode mode)
  {
    if (_window == MemoryUtil.NULL) return;
    if (mode.isFullscreenCapable()) {
      long monitor = GLFW.glfwGetPrimaryMonitor();
      GLFW.glfwSetWindowMonitor(_window, monitor, 0, 0,
        mode.getWidth(), mode.getHeight(),
        mode.getFrequency() > 0 ? mode.getFrequency() : GLFW.GLFW_DONT_CARE);
    } else {
      GLFW.glfwSetWindowMonitor(_window, MemoryUtil.NULL,
        100, 100, mode.getWidth(), mode.getHeight(), GLFW.GLFW_DONT_CARE);
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
    // GLFW window icon setting would go here
    // For now, this is a no-op; GLFW icon support differs from LWJGL 2
    log.info("setIcon: GLFW icon support pending implementation.");
  }

  // documentation inherited from interface GlContext
  public void makeCurrent ()
  {
    GLFW.glfwMakeContextCurrent(_window);
  }

  @Override
  public RunQueue getRunQueue ()
  {
    if (_mainRunQueue == null) {
      // Can be called during superclass constructor before field initializers run
      _mainQueue = new ConcurrentLinkedQueue<>();
      _mainRunQueue = createMainRunQueue();
    }
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
    // On macOS, GLFW requires all window/event operations on the main thread.
    // With -XstartOnFirstThread, main() IS the main thread, so we run the
    // GLFW loop there and use a queue for deferred tasks.
    _mainThread = Thread.currentThread();
    _running = true;
    init();
    if (_window == MemoryUtil.NULL) {
      _running = false;
      return;
    }
    // run the main loop on this thread (the main thread)
    mainLoop();
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
    }
    GLFW.glfwTerminate();
    System.exit(0);
  }

  @Override
  protected void init ()
  {
    if (!createDisplay()) {
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
    _mainThread = Thread.currentThread();
    _running = true;
    while (_running) {
      if (GLFW.glfwWindowShouldClose(_window)) {
        _running = false;
        break;
      }

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
    performShutdown();
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
    _renderer.init(_window, w[0], h[0]);
  }

  /**
   * Update the renderer size after a mode change.
   */
  protected void updateRendererSize ()
  {
    if (_window != MemoryUtil.NULL) {
      int[] w = new int[1], h = new int[1];
      GLFW.glfwGetFramebufferSize(_window, w, h);
      _renderer.setSize(w[0], h[0]);
    }
  }

  /**
   * Return the size to use for rendering.
   */
  protected Dimension calcRendererSize ()
  {
    int[] w = new int[1], h = new int[1];
    GLFW.glfwGetFramebufferSize(_window, w, h);
    return new Dimension(w[0], h[0]);
  }

  /**
   * Creates the GLFW window with one of the supported pixel formats.
   *
   * @return true if successful.
   */
  protected boolean createDisplay ()
  {
    ensureGlfwInit();
    if (!_glfwInited) {
      return false;
    }

    // Try pixel formats in order of preference
    for (PixelFormat format : getPixelFormats()) {
      GLFW.glfwDefaultWindowHints();
      GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
      GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, _resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
      GLFW.glfwWindowHint(GLFW.GLFW_ALPHA_BITS, format.getAlphaBits());
      GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, format.getDepthBits());
      GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, format.getStencilBits());
      if (format.getSamples() > 0) {
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, format.getSamples());
      }

      _window = GLFW.glfwCreateWindow(800, 600, "Clyde", MemoryUtil.NULL, MemoryUtil.NULL);
      if (_window != MemoryUtil.NULL) {
        GLFW.glfwMakeContextCurrent(_window);
        GL.createCapabilities();
        GLFW.glfwSwapInterval(_vsync ? 1 : 0);
        GLFW.glfwSetWindowSizeCallback(_window, (win, w, h) -> _wasResized = true);
        GLFW.glfwShowWindow(_window);
        return true;
      }
    }

    log.warning("Couldn't find valid pixel format.");
    return false;
  }

  /**
   * Updates and renders a single frame.
   */
  protected void updateFrame ()
  {
    try {
      GLFW.glfwPollEvents();
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

  /** Whether vsync is enabled. */
  protected boolean _vsync;

  /** Whether the window should be resizable. */
  protected boolean _resizable = true;

  /** Whether the window was resized. */
  protected boolean _wasResized;

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

  private RunQueue createMainRunQueue ()
  {
    return new RunQueue() {
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
  }

  /** The main thread reference for RunQueue.isDispatchThread(). */
  protected Thread _mainThread = Thread.currentThread();
}
