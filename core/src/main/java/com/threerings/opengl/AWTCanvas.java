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

import java.awt.Canvas;
import java.awt.EventQueue;
import java.awt.Graphics;

import javax.swing.JPopupMenu;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import com.threerings.opengl.lwjgl2.PixelFormat;

import static com.threerings.opengl.Log.log;

/**
 * A canvas that creates a GLFW context for OpenGL rendering.
 * In LWJGL 3, AWTGLCanvas is no longer available. This class creates
 * a hidden GLFW window for the GL context.
 */
public class AWTCanvas extends Canvas
  implements GlCanvas
{
  /**
   * Creates a canvas with the supplied pixel format.
   */
  public AWTCanvas (PixelFormat pformat)
    throws Exception
  {
    if (!GLFW.glfwInit()) {
      throw new Exception("Failed to initialize GLFW");
    }

    GLFW.glfwDefaultWindowHints();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_ALPHA_BITS, pformat.alphaBits);
    GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, pformat.depthBits);
    GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, pformat.stencilBits);
    if (pformat.samples > 0) {
      GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, pformat.samples);
    }

    _window = GLFW.glfwCreateWindow(800, 600, "", MemoryUtil.NULL, MemoryUtil.NULL);
    if (_window == MemoryUtil.NULL) {
      throw new Exception("Failed to create GLFW window for AWTCanvas");
    }

    GLFW.glfwMakeContextCurrent(_window);
    GL.createCapabilities();

    // make popups heavyweight so that we can see them over the canvas
    JPopupMenu.setDefaultLightWeightPopupEnabled(false);
  }

  // documentation inherited from interface GlCanvas
  public long getWindowHandle ()
  {
    return _window;
  }

  // documentation inherited from interface GlCanvas
  public void setVSyncEnabled (boolean vsync)
  {
    GLFW.glfwMakeContextCurrent(_window);
    GLFW.glfwSwapInterval(vsync ? 1 : 0);
  }

  // documentation inherited from interface GlCanvas
  public void shutdown ()
  {
    stopUpdating();
    if (_window != MemoryUtil.NULL) {
      GLFW.glfwDestroyWindow(_window);
      _window = MemoryUtil.NULL;
    }
  }

  @Override
  public void makeCurrent ()
  {
    if (_window != MemoryUtil.NULL) {
      GLFW.glfwMakeContextCurrent(_window);
    }
  }

  public void swapBuffers ()
  {
    if (_window != MemoryUtil.NULL) {
      GLFW.glfwSwapBuffers(_window);
    }
  }

  @Override
  public void removeNotify ()
  {
    super.removeNotify();
    stopUpdating();
  }

  @Override
  public void paint (Graphics g)
  {
    if (!_glInitialized) {
      _glInitialized = true;

      // let subclasses do their own initialization
      didInit();

      // start rendering frames
      startUpdating();
    }
  }

  /**
   * Override to perform custom initialization.
   */
  protected void didInit ()
  {
  }

  /**
   * Starts calling {@link #updateFrame} regularly.
   */
  protected void startUpdating ()
  {
    _updater = new Runnable() {
      public void run () {
        if (_updater != null) {
          updateFrame();
          EventQueue.invokeLater(this);
        }
      }
    };
    EventQueue.invokeLater(_updater);
  }

  /**
   * Stops calling {@link #updateFrame}.
   */
  protected void stopUpdating ()
  {
    _updater = null;
  }

  /**
   * Updates and, if the canvas is showing, renders the scene and swaps the buffers.
   */
  protected void updateFrame ()
  {
    try {
      updateView();
      if (isShowing()) {
        renderView();
        swapBuffers();
      }
    } catch (Exception e) {
      log.warning("Caught exception in frame loop.", e);
    }
  }

  /**
   * Override to perform any updates that are required even if not rendering.
   */
  protected void updateView ()
  {
  }

  /**
   * Override to render the contents of the canvas.
   */
  protected void renderView ()
  {
  }

  /** The GLFW window handle. */
  protected long _window = MemoryUtil.NULL;

  /** Whether GL has been initialized. */
  protected boolean _glInitialized;

  /** The runnable that updates the frame. */
  protected Runnable _updater;
}
