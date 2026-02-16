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
import java.awt.Canvas;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import com.samskivert.util.HashIntSet;
import com.samskivert.util.Interator;

import com.threerings.opengl.lwjgl2.Keyboard;
import com.threerings.opengl.lwjgl2.PixelFormat;

import static com.threerings.opengl.Log.log;

/**
 * A canvas that uses a GLFW offscreen context for rendering, embedded in a Swing panel.
 * In LWJGL 3, the Display class is replaced by GLFW window management. This canvas creates
 * a hidden GLFW window for the GL context and renders into the Swing component.
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

    // create and add the contained canvas
    _canvas = new Canvas() {
      @Override public Point getMousePosition () {
        return _entered ? getRelativeMouseLocation() : null;
      }
      @Override public void paint (Graphics g) {
        if (_initialized) {
          return;
        }
        _initialized = true;
        for (PixelFormat format : GlApp.getPixelFormats(antialiasingLevel)) {
          try {
            init(format);
            return;
          } catch (Exception e) {
            log.warning("Unable to use 'PixelFormat'", "ex", e); // log msg, not trace
            // proceed to next format
          }
        }
        log.warning("Couldn't find valid pixel format.");
      }
      @Override public void update (Graphics g) {
        // no-op
      }
    };
    add(_canvas, BorderLayout.CENTER);

    JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    _canvas.setFocusable(false);

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
        _buttons[button].wasPressed(event);
        updateButtonModifier(button, true);
      }
      @Override public void mouseReleased (MouseEvent event) {
        int button = getLWJGLButton(event.getButton());
        _buttons[button].wasReleased(event);
        updateButtonModifier(button, false);
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
    addMouseListener(listener);
    addMouseMotionListener(listener);
    addMouseWheelListener(new MouseWheelListener() {
      public void mouseWheelMoved (MouseWheelEvent event) {
        _lclicks--;
      }
    });
  }

  // documentation inherited from interface GlCanvas
  public long getWindowHandle ()
  {
    return _window;
  }

  // documentation inherited from interface GlCanvas
  public void setVSyncEnabled (boolean vsync)
  {
    if (_window != MemoryUtil.NULL) {
      GLFW.glfwMakeContextCurrent(_window);
      GLFW.glfwSwapInterval(vsync ? 1 : 0);
    }
  }

  // documentation inherited from interface GlCanvas
  public void makeCurrent ()
  {
    if (_window != MemoryUtil.NULL) {
      GLFW.glfwMakeContextCurrent(_window);
    }
  }

  // documentation inherited from interface GlCanvas
  public void shutdown ()
  {
    if (_window != MemoryUtil.NULL) {
      GLFW.glfwDestroyWindow(_window);
      _window = MemoryUtil.NULL;
    }
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

  @Override
  public void addNotify ()
  {
    super.addNotify();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
  }

  @Override
  public void removeNotify ()
  {
    super.removeNotify();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
  }

  @Override
  public Point getMousePosition ()
  {
    return _canvas.getMousePosition();
  }

  /**
   * Attempts to create a GLFW window/context for this canvas.
   */
  protected void init (PixelFormat pformat)
    throws Exception
  {
    if (!GLFW.glfwInit()) {
      throw new Exception("Failed to initialize GLFW");
    }

    GLFW.glfwDefaultWindowHints();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_ALPHA_BITS, pformat.getAlphaBits());
    GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, pformat.getDepthBits());
    GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, pformat.getStencilBits());
    if (pformat.getSamples() > 0) {
      GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, pformat.getSamples());
    }

    _window = GLFW.glfwCreateWindow(
      _canvas.getWidth() > 0 ? _canvas.getWidth() : 800,
      _canvas.getHeight() > 0 ? _canvas.getHeight() : 600,
      "", MemoryUtil.NULL, MemoryUtil.NULL);
    if (_window == MemoryUtil.NULL) {
      throw new Exception("Failed to create GLFW window");
    }

    GLFW.glfwMakeContextCurrent(_window);
    GL.createCapabilities();

    // give subclasses a chance to initialize
    didInit();

    // start rendering frames
    startUpdating();
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
          makeCurrent();
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
      // Generate AWT events from the Swing/AWT input system
      // (GLFW input is not used in embedded canvas mode)
      updateView();
      if (isShowing()) {
        renderView();
      }
      if (_window != MemoryUtil.NULL) {
        GLFW.glfwSwapBuffers(_window);
      }

    } catch (Exception e) {
      log.warning("Caught exception in frame loop.", e);
    }
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

  /** The GLFW window handle. */
  protected long _window = MemoryUtil.NULL;

  /** The contained canvas. */
  protected Canvas _canvas;

  /** Set on initialization. */
  protected boolean _initialized;

  /** The runnable that updates the frame. */
  protected Runnable _updater;

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

  /** A mask for checking whether any mouse buttons are down. */
  protected static final int ANY_BUTTONS_DOWN_MASK =
    InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;

  /** Mouse buttons released within this interval after being pressed are counted as clicks. */
  protected static final long CLICK_INTERVAL = 250L;

  /** Clicks this close to one another are chained together for counting purposes. */
  protected static final long CLICK_CHAIN_INTERVAL = 250L;
}
