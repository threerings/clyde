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

package com.threerings.opengl.gui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;

import com.threerings.opengl.util.GlUtil;

import static com.threerings.opengl.gui.Log.log;

/**
 * Contains a cursor.
 */
public class Cursor
{
  /**
   * Creates a new cursor.
   */
  public Cursor (BufferedImage image, int hx, int hy)
  {
    _image = image;
    _hx = hx;
    _hy = hy;
  }

  /**
   * Retrieve the lazily-initialized GLFW cursor handle.
   */
  public long getGLFWCursor ()
  {
    if (_glfwCursor == 0) {
      _glfwCursor = createGLFWCursor();
    }
    return _glfwCursor;
  }

  /**
   * Retrieves the lazily-initialized AWT cursor using the supplied toolkit.
   */
  public java.awt.Cursor getAWTCursor (Toolkit toolkit)
  {
    if (_awtCursor == null) {
      _awtCursor = createAWTCursor(toolkit);
    }
    return _awtCursor;
  }

  /**
   * Display the GLFW cursor on the given window.
   */
  public void show (long window)
  {
    if (window == 0) {
      return;
    }
    long cursor = getGLFWCursor();
    if (cursor != 0) {
      GLFW.glfwSetCursor(window, cursor);
    }
  }

  /**
   * Display the cursor (legacy method).
   */
  public void show ()
  {
    // No-op without a window reference; use show(long window) instead
  }

  /**
   * Destroys the GLFW cursor resources.
   */
  public void destroy ()
  {
    if (_glfwCursor != 0) {
      GLFW.glfwDestroyCursor(_glfwCursor);
      _glfwCursor = 0;
    }
  }

  /**
   * Creates a GLFW cursor from the configured image and hotspot.
   */
  protected long createGLFWCursor ()
  {
    GLFWImage glfwImage = GLFWImage.malloc();
    glfwImage.set(_image.getWidth(), _image.getHeight(), GlUtil.getRgbaPixels(_image));
    long cursor = GLFW.glfwCreateCursor(glfwImage, _hx, _hy);
    glfwImage.free();

    return cursor;
  }

  /**
   * Creates an AWT cursor from the configured image and hotspot.
   */
  protected java.awt.Cursor createAWTCursor (Toolkit toolkit)
  {
    int width = _image.getWidth(), height = _image.getHeight();
    Dimension size = toolkit.getBestCursorSize(width, height);

    // Per Toolkit.getBestCursorSize, a (0,0) result means custom cursors aren't supported.
    if (size.width <= 0 || size.height <= 0) return java.awt.Cursor.getDefaultCursor();

    BufferedImage image = _image;
    int hx = _hx, hy = _hy;
    if (size.width != width || size.height != height) {
      // Render into a fresh ARGB BufferedImage rather than using Image.getScaledInstance.
      // getScaledInstance returns a non-BufferedImage Image, which on Windows causes
      // Toolkit.createCustomCursor to drop the alpha channel and render fully-transparent
      // pixels as opaque black -- visible most obviously as a black box for our empty
      // (transparent) cursor on systems where best cursor size != source size (e.g. 16->32
      // upscaling on Windows).
      image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      try {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(_image, 0, 0, size.width, size.height, null);
      } finally {
        g.dispose();
      }
      hx = (_hx * size.width) / width;
      hy = (_hy * size.height) / height;
    }
    return toolkit.createCustomCursor(image, new Point(hx, hy), "cursor");
  }

  protected BufferedImage _image;
  protected int _hx, _hy;

  protected long _glfwCursor;
  protected java.awt.Cursor _awtCursor;
}
