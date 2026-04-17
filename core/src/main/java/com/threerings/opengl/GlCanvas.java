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

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

import static com.threerings.opengl.Log.log;

/**
 * A canvas that uses lwjgl3-awt to embed an OpenGL context in a Swing panel.
 * This replaces the old LWJGL 2 Display.setParent() approach.
 */
public class GlCanvas extends JPanel
{
  /**
   * Creates a new canvas.
   */
  public GlCanvas (final int antialiasingLevel)
  {
    super(new BorderLayout());

    // configure the GL data for the context
    GLData data = new GLData();
    data.samples = antialiasingLevel;
    data.depthSize = 24;
    data.stencilSize = 8;
    data.alphaSize = 0; // no framebuffer alpha to avoid compositing issues on macOS
    data.doubleBuffer = true;
    // Request a compatibility profile for legacy OpenGL
    data.profile = GLData.Profile.COMPATIBILITY;

    // create the AWT GL canvas
    _awtCanvas = new LockableGLCanvas(data);

    add(_awtCanvas, BorderLayout.CENTER);

    JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    _awtCanvas.setFocusable(false);

    // Track whether the mouse is over the canvas (for the getMousePosition override,
    // which can't use the default JPanel behavior because the heavyweight child
    // covers the panel), and route focus to this panel on mouse press so it
    // receives subsequent key events.
    MouseInputAdapter listener = new MouseInputAdapter() {
      @Override public void mouseEntered (MouseEvent event) { _entered = true; }
      @Override public void mouseExited (MouseEvent event) { _entered = false; }
      @Override public void mousePressed (MouseEvent event) { requestFocusInWindow(); }
    };
    // Attach to _awtCanvas (heavyweight) since it sits on top and intercepts events
    // that would otherwise go to this JPanel.
    _awtCanvas.addMouseListener(listener);
  }

  /**
   * Returns the framebuffer width in pixels (may differ from component width on HiDPI/Retina).
   */
  public int getFramebufferWidth ()
  {
    return _awtCanvas.getFramebufferWidth();
  }

  /**
   * Returns the framebuffer height in pixels (may differ from component height on HiDPI/Retina).
   */
  public int getFramebufferHeight ()
  {
    return _awtCanvas.getFramebufferHeight();
  }

  /**
   * Enables or disables vsync.
   */
  public void setVSyncEnabled (boolean vsync)
  {
    // TODO: implement vsync for lwjgl3-awt (requires platform-specific CGL/WGL/GLX calls).
    // The GlCanvasTool frame limiter (Thread.sleep) provides adequate rate control for tools.
  }

  /**
   * Makes the canvas context current.
   */
  public void makeCurrent ()
  {
    // Lock the GL context so that GL calls can be made outside of the
    // render callback (e.g., during scene/resource loading).
    if (!_contextLocked) {
      _awtCanvas.lockContext();
      _contextLocked = true;
    }
  }

  /**
   * Releases the GL context after a manual {@link #makeCurrent()} call.
   */
  public void releaseCurrent ()
  {
    if (_contextLocked) {
      _awtCanvas.unlockContext();
      _contextLocked = false;
    }
  }

  /**
   * Shuts down the canvas.
   */
  public void shutdown ()
  {
    stopUpdating();
    _awtCanvas.disposeCanvas();
  }

  // Forward external mouse listener registration to the heavyweight GL canvas,
  // which sits on top and receives all mouse events.
  @Override
  public synchronized void addMouseListener (MouseListener l)
  {
    _awtCanvas.addMouseListener(l);
  }

  @Override
  public synchronized void addMouseMotionListener (MouseMotionListener l)
  {
    _awtCanvas.addMouseMotionListener(l);
  }

  @Override
  public synchronized void addMouseWheelListener (MouseWheelListener l)
  {
    _awtCanvas.addMouseWheelListener(l);
  }

  @Override
  public synchronized void removeMouseListener (MouseListener l)
  {
    _awtCanvas.removeMouseListener(l);
  }

  @Override
  public synchronized void removeMouseMotionListener (MouseMotionListener l)
  {
    _awtCanvas.removeMouseMotionListener(l);
  }

  @Override
  public synchronized void removeMouseWheelListener (MouseWheelListener l)
  {
    _awtCanvas.removeMouseWheelListener(l);
  }

  @Override
  public void addNotify ()
  {
    super.addNotify();
    // Start the render loop once the canvas has a native peer
    startUpdating();
  }

  @Override
  public void removeNotify ()
  {
    super.removeNotify();
    stopUpdating();
  }

  @Override
  public Point getMousePosition ()
  {
    return _entered ? getRelativeMouseLocation() : null;
  }

  /**
   * Override to perform custom initialization.
   * Called once when the GL context is first created.
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
    releaseCurrent();
  }

  /**
   * Updates and, if the canvas is showing, renders the scene and swaps the buffers.
   * The lwjgl3-awt render() call makes the context current, calls paintGL()
   * (which runs updateView + renderView + swapBuffers), then releases the context.
   */
  protected void updateFrame ()
  {
    try {
      // Release the persistent context lock so render()'s internal lock/unlock works
      releaseCurrent();
      _awtCanvas.render();
    } catch (Exception e) {
      log.warning("Caught exception in frame loop.", e);
    }
    // Re-lock the context so it stays current on the AWT thread between frames.
    // This matches LWJGL 2 behavior where the GL context was always current,
    // allowing resource creation (buffer uploads, texture loads) at any time.
    makeCurrent();
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

  /**
   * Returns the location of the mouse pointer relative to this component.
   */
  protected Point getRelativeMouseLocation ()
  {
    java.awt.PointerInfo info = MouseInfo.getPointerInfo();
    if (info == null) {
      return new Point(0, 0);
    }
    Point pt = info.getLocation();
    SwingUtilities.convertPointFromScreen(pt, this);
    return pt;
  }

  /**
   * AWTGLCanvas subclass that exposes context lock/unlock for use outside the render callback.
   */
  protected class LockableGLCanvas extends AWTGLCanvas
  {
    public LockableGLCanvas (GLData data) { super(data); }
    @Override public void initGL () {
      GL.createCapabilities();
      GlCanvas.this.didInit();
    }
    @Override public void paintGL () {
      GlCanvas.this.updateView();
      if (isShowing()) {
        GlCanvas.this.renderView();
      }
      swapBuffers();
    }
    public void lockContext () { beforeRender(); }
    public void unlockContext () { afterRender(); }
  }

  /** The lwjgl3-awt GL canvas that provides the actual OpenGL surface. */
  protected LockableGLCanvas _awtCanvas;

  /** Whether the GL context is manually locked via makeCurrent(). */
  protected boolean _contextLocked;

  /** Whether or not the mouse is over the component. */
  protected boolean _entered;

  /** The runnable that updates the frame. */
  protected Runnable _updater;
}
