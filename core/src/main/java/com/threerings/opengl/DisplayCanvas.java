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
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

import com.samskivert.util.HashIntSet;

import com.threerings.opengl.lwjgl2.Keyboard;

import static com.threerings.opengl.Log.log;

/**
 * A canvas that uses lwjgl3-awt to embed an OpenGL context in a Swing panel.
 * This replaces the old LWJGL 2 Display.setParent() approach.
 */
public class DisplayCanvas extends JPanel
  implements GlCanvas, KeyEventDispatcher
{
  /**
   * Creates a new canvas.
   */
  public DisplayCanvas (final int antialiasingLevel)
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
    _glCanvas = new LockableGLCanvas(data);

    add(_glCanvas, BorderLayout.CENTER);

    JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    _glCanvas.setFocusable(false);

    // add a listener to record states
    MouseInputAdapter listener = new MouseInputAdapter() {
      @Override public void mouseEntered (MouseEvent event) {
        _entered = true;
        _lx = event.getX();
        _ly = event.getY();
      }
      @Override public void mouseExited (MouseEvent event) {
        _entered = false;
      }
      @Override public void mousePressed (MouseEvent event) {
        requestFocusInWindow();
        int button = getLWJGLButton(event.getButton());
        if (button >= 0 && button < _buttons.length) {
          _buttons[button].wasPressed(event);
          updateButtonModifier(button, true);
        }
      }
      @Override public void mouseReleased (MouseEvent event) {
        int button = getLWJGLButton(event.getButton());
        if (button >= 0 && button < _buttons.length) {
          _buttons[button].wasReleased(event);
          updateButtonModifier(button, false);
        }
      }
      @Override public void mouseMoved (MouseEvent event) {
        _lx = event.getX();
        _ly = event.getY();
      }
      @Override public void mouseDragged (MouseEvent event) {
        _lx = event.getX();
        _ly = event.getY();
      }
    };
    // Attach listeners to _glCanvas (heavyweight) since it sits on top and
    // intercepts mouse events that would otherwise go to this JPanel.
    _glCanvas.addMouseListener(listener);
    _glCanvas.addMouseMotionListener(listener);
    _glCanvas.addMouseWheelListener(new MouseWheelListener() {
      public void mouseWheelMoved (MouseWheelEvent event) {
        _lclicks--;
      }
    });
  }

  /**
   * Returns the framebuffer width in pixels (may differ from component width on HiDPI/Retina).
   */
  public int getFramebufferWidth ()
  {
    return _glCanvas.getFramebufferWidth();
  }

  /**
   * Returns the framebuffer height in pixels (may differ from component height on HiDPI/Retina).
   */
  public int getFramebufferHeight ()
  {
    return _glCanvas.getFramebufferHeight();
  }

  // documentation inherited from interface GlCanvas
  public long getWindowHandle ()
  {
    return 0; // no GLFW window; context is managed by lwjgl3-awt
  }

  // documentation inherited from interface GlCanvas
  public void setVSyncEnabled (boolean vsync)
  {
    // TODO: implement vsync for lwjgl3-awt (requires platform-specific CGL/WGL/GLX calls).
    // The GlCanvasTool frame limiter (Thread.sleep) provides adequate rate control for tools.
  }

  // documentation inherited from interface GlCanvas
  public void makeCurrent ()
  {
    // Lock the GL context so that GL calls can be made outside of the
    // render callback (e.g., during scene/resource loading).
    if (!_contextLocked) {
      _glCanvas.lockContext();
      _contextLocked = true;
    }
  }

  /**
   * Releases the GL context after a manual {@link #makeCurrent()} call.
   */
  public void releaseCurrent ()
  {
    if (_contextLocked) {
      _glCanvas.unlockContext();
      _contextLocked = false;
    }
  }

  // documentation inherited from interface GlCanvas
  public void shutdown ()
  {
    stopUpdating();
    _glCanvas.disposeCanvas();
  }

  // documentation inherited from interface KeyEventDispatcher
  public boolean dispatchKeyEvent (KeyEvent event)
  {
    boolean pressed;
    int id = event.getID();
    if (id == KeyEvent.KEY_PRESSED) {
      pressed = true;
    } else if (id == KeyEvent.KEY_RELEASED) {
      pressed = false;
    } else {
      return false;
    }
    int mask, okey;
    boolean left = (event.getKeyLocation() == KeyEvent.KEY_LOCATION_LEFT);
    switch (event.getKeyCode()) {
      case KeyEvent.VK_SHIFT:
        mask = InputEvent.SHIFT_DOWN_MASK;
        okey = left ? Keyboard.KEY_RSHIFT : Keyboard.KEY_LSHIFT;
        break;
      case KeyEvent.VK_CONTROL:
        mask = InputEvent.CTRL_DOWN_MASK;
        okey = left ? Keyboard.KEY_RCONTROL : Keyboard.KEY_LCONTROL;
        break;
      case KeyEvent.VK_ALT:
        mask = InputEvent.ALT_DOWN_MASK;
        okey = left ? Keyboard.KEY_RMENU : Keyboard.KEY_LMENU;
        break;
      case KeyEvent.VK_META:
        mask = InputEvent.META_DOWN_MASK;
        okey = left ? Keyboard.KEY_RMETA : Keyboard.KEY_LMETA;
        break;
      default:
        return false;
    }
    if (pressed || _pressedKeys.contains(okey)) {
      _modifiers |= mask;
    } else {
      _modifiers &= ~mask;
    }
    return false;
  }

  // Forward external mouse listener registration to the heavyweight GL canvas,
  // which sits on top and receives all mouse events.
  @Override
  public synchronized void addMouseListener (MouseListener l)
  {
    _glCanvas.addMouseListener(l);
  }

  @Override
  public synchronized void addMouseMotionListener (MouseMotionListener l)
  {
    _glCanvas.addMouseMotionListener(l);
  }

  @Override
  public synchronized void addMouseWheelListener (MouseWheelListener l)
  {
    _glCanvas.addMouseWheelListener(l);
  }

  @Override
  public synchronized void removeMouseListener (MouseListener l)
  {
    _glCanvas.removeMouseListener(l);
  }

  @Override
  public synchronized void removeMouseMotionListener (MouseMotionListener l)
  {
    _glCanvas.removeMouseMotionListener(l);
  }

  @Override
  public synchronized void removeMouseWheelListener (MouseWheelListener l)
  {
    _glCanvas.removeMouseWheelListener(l);
  }

  @Override
  public void addNotify ()
  {
    super.addNotify();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    // Start the render loop once the canvas has a native peer
    startUpdating();
  }

  @Override
  public void removeNotify ()
  {
    super.removeNotify();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
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
      _glCanvas.render();
    } catch (Exception e) {
      log.warning("Caught exception in frame loop.", e);
    }
    // Re-lock the context so it stays current on the AWT thread between frames.
    // This matches LWJGL 2 behavior where the GL context was always current,
    // allowing resource creation (buffer uploads, texture loads) at any time.
    makeCurrent();
  }

  /**
   * Updates the modifier for the specified button.
   */
  protected void updateButtonModifier (int button, boolean pressed)
  {
    int mask;
    switch (button) {
      case 0:
        mask = InputEvent.BUTTON1_DOWN_MASK;
        break;
      case 1:
        mask = InputEvent.BUTTON3_DOWN_MASK;
        break;
      case 2:
        mask = InputEvent.BUTTON2_DOWN_MASK;
        break;
      default:
        return;
    }
    if (pressed) {
      _modifiers |= mask;
    } else {
      _modifiers &= ~mask;
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
   * Determines whether the current set of modifiers contains any pressed buttons.
   */
  protected boolean anyButtonsDown ()
  {
    return (_modifiers & ANY_BUTTONS_DOWN_MASK) != 0;
  }

  /**
   * Returns the AWT button corresponding to the specified LWJGL button.
   */
  protected static int getAWTButton (int button)
  {
    switch (button) {
      case 0: return MouseEvent.BUTTON1;
      case 1: return MouseEvent.BUTTON3;
      case 2: return MouseEvent.BUTTON2;
      default: return MouseEvent.NOBUTTON;
    }
  }

  /**
   * Returns the LWJGL button corresponding to the specified AWT button.
   */
  protected static int getLWJGLButton (int button)
  {
    switch (button) {
      case MouseEvent.BUTTON1: return 0;
      case MouseEvent.BUTTON2: return 2;
      case MouseEvent.BUTTON3: return 1;
      default: return -1;
    }
  }

  /**
   * Contains the state of a single mouse button.
   */
  protected class ButtonRecord
  {
    public boolean isPressed ()
    {
      return _pressed;
    }

    public void wasPressed (MouseEvent press)
    {
      _pressed = true;
      long when = press.getWhen();
      _clickTime = when + CLICK_INTERVAL;
      _count = (when < _chainTime) ? (_count + 1) : 1;
    }

    public void wasReleased (MouseEvent release)
    {
      _pressed = false;
      long when = release.getWhen();
      if (when < _clickTime) {
        dispatchEvent(new MouseEvent(
          DisplayCanvas.this, MouseEvent.MOUSE_CLICKED, when, _modifiers,
          release.getX(), release.getY(), _count, false, release.getButton()));
        _chainTime = when + CLICK_CHAIN_INTERVAL;
      }
    }

    protected boolean _pressed;
    protected long _clickTime, _chainTime;
    protected int _count;
  }

  /**
   * AWTGLCanvas subclass that exposes context lock/unlock for use outside the render callback.
   */
  protected class LockableGLCanvas extends AWTGLCanvas
  {
    public LockableGLCanvas (GLData data) { super(data); }
    @Override public void initGL () {
      GL.createCapabilities();
      DisplayCanvas.this.didInit();
    }
    @Override public void paintGL () {
      DisplayCanvas.this.updateView();
      if (isShowing()) {
        DisplayCanvas.this.renderView();
      }
      swapBuffers();
    }
    public void lockContext () { beforeRender(); }
    public void unlockContext () { afterRender(); }
  }

  /** The lwjgl3-awt GL canvas that provides the actual OpenGL surface. */
  protected LockableGLCanvas _glCanvas;

  /** Whether the GL context is manually locked via makeCurrent(). */
  protected boolean _contextLocked;

  /** Whether or not the mouse is over the component. */
  protected boolean _entered;

  /** The last position we reported. */
  protected int _lx, _ly;

  /** The states of the mouse buttons. */
  protected ButtonRecord[] _buttons = new ButtonRecord[] {
    new ButtonRecord(), new ButtonRecord(), new ButtonRecord() };

  /** The number of wheel clicks recorded. */
  protected int _lclicks;

  /** The current set of modifiers. */
  protected int _modifiers;

  /** The set of currently pressed keys. */
  protected HashIntSet _pressedKeys = new HashIntSet();

  /** The runnable that updates the frame. */
  protected Runnable _updater;

  /** A mask for checking whether any mouse buttons are down. */
  protected static final int ANY_BUTTONS_DOWN_MASK =
    InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;

  /** Mouse buttons released within this interval after being pressed are counted as clicks. */
  protected static final long CLICK_INTERVAL = 250L;

  /** Clicks this close to one another are chained together for counting purposes. */
  protected static final long CLICK_CHAIN_INTERVAL = 250L;
}
