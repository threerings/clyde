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

import java.awt.Toolkit;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;

import com.threerings.math.FloatMath;

import com.threerings.opengl.GlDisplayApp;
import com.threerings.opengl.lwjgl2.Keyboard;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.InputEvent;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.text.IMEComponent;
import static com.threerings.opengl.gui.Log.log;


/**
 * A root for GLFW-window-based apps.
 */
public class DisplayRoot extends Root
{
  public DisplayRoot (GlContext ctx)
  {
    super(ctx);
    _clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    // Set up GLFW callbacks if we have a window
    if (ctx.getApp() instanceof GlDisplayApp) {
      long window = ((GlDisplayApp)ctx.getApp()).getWindow();
      if (window != 0) {
        setupCallbacks(window);
      }
    }
  }

  /**
   * Polls the input system for events and dispatches them.
   */
  public void poll ()
  {
    // Process buffered events from GLFW callbacks
    List<Runnable> events;
    synchronized (_eventQueue) {
      events = new ArrayList<>(_eventQueue);
      _eventQueue.clear();
    }
    for (Runnable event : events) {
      event.run();
    }

    // make sure the pressed keys are really pressed
    if (_isActive && !_pressedKeys.isEmpty()) {
      long window = getWindow();
      if (window != 0) {
        for (Iterator<KeyRecord> it = _pressedKeys.values().iterator(); it.hasNext(); ) {
          KeyRecord record = it.next();
          KeyEvent press = record.getPress();
          int key = press.getKeyCode();
          int glfwKey = Keyboard.lwjgl2ToGlfw(key);
          if (glfwKey != GLFW.GLFW_KEY_UNKNOWN &&
              GLFW.glfwGetKey(window, glfwKey) != GLFW.GLFW_PRESS) {
            dispatchEvent(getFocus(), new KeyEvent(
              this, _tickStamp, _modifiers, KeyEvent.KEY_RELEASED,
              press.getKeyChar(), key, false));
            updateKeyModifier(key, false);
            it.remove();
          }
        }
      }
    }

    if (!_isActive) {
      // clear all modifiers and release all keys
      if (!_pressedKeys.isEmpty()) {
        for (KeyRecord record : _pressedKeys.values()) {
          KeyEvent press = record.getPress();
          dispatchEvent(getFocus(), new KeyEvent(
            this, _tickStamp, _modifiers, KeyEvent.KEY_RELEASED,
            press.getKeyChar(), press.getKeyCode(), false));
        }
        _pressedKeys.clear();
      }
      _modifiers = 0;
    }
  }

  /**
   * Sets if we perform native IME composition.
   */
  public void setIMEComposingEnabled (boolean enabled)
  {
    // IME not currently supported in LWJGL 3 migration
  }

  @Override
  public int getDisplayWidth ()
  {
    return FloatMath.round(_ctx.getRenderer().getWidth() / _scale);
  }

  @Override
  public int getDisplayHeight ()
  {
    return FloatMath.round(_ctx.getRenderer().getHeight() / _scale);
  }

  @Override
  public void setMousePosition (int x, int y)
  {
    long window = getWindow();
    if (window != 0) {
      GLFW.glfwSetCursorPos(window, x * _scale, (_ctx.getRenderer().getHeight() - y - 1) * _scale);
    }
    super.setMousePosition(x, y);
  }

  @Override
  protected void updateCursor (Cursor cursor)
  {
    // GLFW cursor management would go here
    // For now, use default cursor
  }

  /**
   * Sets up GLFW input callbacks on the given window.
   */
  protected void setupCallbacks (long window)
  {
    GLFW.glfwSetKeyCallback(window, new GLFWKeyCallback() {
      @Override
      public void invoke (long window, int glfwKey, int scancode, int action, int mods) {
        int key = Keyboard.glfwToLwjgl2(glfwKey);
        boolean pressed = (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT);
        synchronized (_eventQueue) {
          _eventQueue.add(() -> {
            if (pressed) {
              keyPressed(_tickStamp, (char)0, key, false);
            } else {
              keyReleased(_tickStamp, (char)0, key, false);
            }
            updateKeyModifier(key, pressed);
          });
        }
      }
    });

    GLFW.glfwSetCharCallback(window, new GLFWCharCallback() {
      @Override
      public void invoke (long window, int codepoint) {
        // Character events are handled via keyPressed char parameter
      }
    });

    GLFW.glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
      @Override
      public void invoke (long window, int button, int action, int mods) {
        boolean pressed = (action == GLFW.GLFW_PRESS);
        synchronized (_eventQueue) {
          _eventQueue.add(() -> {
            double[] xpos = new double[1], ypos = new double[1];
            GLFW.glfwGetCursorPos(window, xpos, ypos);
            int x = (int)xpos[0];
            int[] ww = new int[1], wh = new int[1];
            GLFW.glfwGetWindowSize(window, ww, wh);
            int y = wh[0] - (int)ypos[0] - 1;
            if (pressed) {
              mousePressed(_tickStamp, button, x, y, false);
              updateButtonModifier(button, true);
            } else {
              mouseReleased(_tickStamp, button, x, y, false);
              updateButtonModifier(button, false);
            }
          });
        }
      }
    });

    GLFW.glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
      @Override
      public void invoke (long window, double xpos, double ypos) {
        synchronized (_eventQueue) {
          _eventQueue.add(() -> {
            int x = (int)xpos;
            int[] ww = new int[1], wh = new int[1];
            GLFW.glfwGetWindowSize(window, ww, wh);
            int y = wh[0] - (int)ypos - 1;
            mouseMoved(_tickStamp, x, y, false);
          });
        }
      }
    });

    GLFW.glfwSetScrollCallback(window, new GLFWScrollCallback() {
      @Override
      public void invoke (long window, double xoffset, double yoffset) {
        synchronized (_eventQueue) {
          _eventQueue.add(() -> {
            double[] xpos = new double[1], ypos2 = new double[1];
            GLFW.glfwGetCursorPos(window, xpos, ypos2);
            int x = (int)xpos[0];
            int[] ww = new int[1], wh = new int[1];
            GLFW.glfwGetWindowSize(window, ww, wh);
            int y = wh[0] - (int)ypos2[0] - 1;
            mouseWheeled(_tickStamp, x, y, (yoffset > 0) ? +1 : -1, false);
          });
        }
      }
    });

    GLFW.glfwSetWindowFocusCallback(window, new GLFWWindowFocusCallback() {
      @Override
      public void invoke (long window, boolean focused) {
        _isActive = focused;
      }
    });
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
        mask = InputEvent.BUTTON2_DOWN_MASK;
        break;
      case 2:
        mask = InputEvent.BUTTON3_DOWN_MASK;
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
   * Updates the modifier for the specified key, if any.
   */
  protected void updateKeyModifier (int key, boolean pressed)
  {
    int mask, okey;
    switch (key) {
      case Keyboard.KEY_LSHIFT:
      case Keyboard.KEY_RSHIFT:
        mask = InputEvent.SHIFT_DOWN_MASK;
        okey = (key == Keyboard.KEY_LSHIFT) ? Keyboard.KEY_RSHIFT : Keyboard.KEY_LSHIFT;
        break;
      case Keyboard.KEY_LCONTROL:
      case Keyboard.KEY_RCONTROL:
        mask = InputEvent.CTRL_DOWN_MASK;
        okey = (key == Keyboard.KEY_LCONTROL) ?
          Keyboard.KEY_RCONTROL : Keyboard.KEY_LCONTROL;
        break;
      case Keyboard.KEY_LMENU:
      case Keyboard.KEY_RMENU:
        mask = InputEvent.ALT_DOWN_MASK;
        okey = (key == Keyboard.KEY_LMENU) ? Keyboard.KEY_RMENU : Keyboard.KEY_LMENU;
        break;
      case Keyboard.KEY_LMETA:
      case Keyboard.KEY_RMETA:
        mask = InputEvent.META_DOWN_MASK;
        okey = (key == Keyboard.KEY_LMETA) ? Keyboard.KEY_RMETA : Keyboard.KEY_LMETA;
        break;
      default:
        return;
    }
    if (pressed || _pressedKeys.containsKey(okey)) {
      _modifiers |= mask;
    } else {
      _modifiers &= ~mask;
    }
  }

  /**
   * Returns the GLFW window handle.
   */
  protected long getWindow ()
  {
    if (_ctx.getApp() instanceof GlDisplayApp) {
      return ((GlDisplayApp)_ctx.getApp()).getWindow();
    }
    return 0;
  }

  /** Whether the window is currently active/focused. */
  protected boolean _isActive = true;

  /** Queue of input events from GLFW callbacks. */
  protected final List<Runnable> _eventQueue = new ArrayList<>();
}
