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

import org.lwjgl.glfw.GLFW;

import com.threerings.math.FloatMath;

import com.threerings.opengl.GlCanvas;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.InputEvent;
import com.threerings.opengl.gui.event.MouseEvent;

import static com.threerings.opengl.Log.log;

/**
 * Bridges between the AWT and the BUI input event system when we are
 * being used in an AWT canvas.
 */
public class CanvasRoot extends Root
  implements java.awt.event.MouseListener, java.awt.event.MouseMotionListener,
        java.awt.event.MouseWheelListener, java.awt.event.KeyListener
{
  public CanvasRoot (GlContext ctx, GlCanvas canvas)
  {
    super(ctx);
    _canvas = canvas;
    _clipboard = canvas.getToolkit().getSystemClipboard();

    // set our UI scale to match the display scale
    setScale(ctx.getPixelScaleFactor());

    // we want to hear about mouse movement, clicking, and keys
    canvas.addMouseListener(this);
    canvas.addMouseMotionListener(this);
    canvas.addMouseWheelListener(this);
    canvas.addKeyListener(this);
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

  // documentation inherited from interface MouseListener
  public void mouseClicked (java.awt.event.MouseEvent e) {
    // N/A
  }

  // documentation inherited from interface MouseListener
  public void mouseEntered (java.awt.event.MouseEvent e) {
    // N/A
  }

  // documentation inherited from interface MouseListener
  public void mouseExited (java.awt.event.MouseEvent e) {
    // N/A
  }

  // documentation inherited from interface MouseListener
  public void mousePressed (java.awt.event.MouseEvent e)
  {
    _modifiers = convertModifiers(e.getModifiersEx());
    float scale = _ctx.getPixelScaleFactor();
    mousePressed(e.getWhen(), convertButton(e), Math.round(e.getX() * scale),
      Math.round(((_canvas.getHeight() - e.getY()) * scale) - 1), e.isConsumed());
  }

  // documentation inherited from interface MouseListener
  public void mouseReleased (java.awt.event.MouseEvent e)
  {
    _modifiers = convertModifiers(e.getModifiersEx());
    float scale = _ctx.getPixelScaleFactor();
    mouseReleased(e.getWhen(), convertButton(e), Math.round(e.getX() * scale),
      Math.round(((_canvas.getHeight() - e.getY()) * scale) - 1), e.isConsumed());
  }

  // documentation inherited from interface MouseMotionListener
  public void mouseDragged (java.awt.event.MouseEvent e)
  {
    mouseMoved(e);
  }

  // documentation inherited from interface MouseMotionListener
  public void mouseMoved (java.awt.event.MouseEvent e)
  {
    _modifiers = convertModifiers(e.getModifiersEx());
    float scale = _ctx.getPixelScaleFactor();
    mouseMoved(e.getWhen(), Math.round(e.getX() * scale),
      Math.round(((_canvas.getHeight() - e.getY()) * scale) - 1), e.isConsumed());
  }

  // documentation inherited from interface MouseWheelListener
  public void mouseWheelMoved (java.awt.event.MouseWheelEvent e)
  {
    _modifiers = convertModifiers(e.getModifiersEx());
    float scale = _ctx.getPixelScaleFactor();
    mouseWheeled(e.getWhen(), Math.round(e.getX() * scale),
      Math.round(((_canvas.getHeight() - e.getY()) * scale) - 1),
      -e.getWheelRotation(), e.isConsumed());
  }

  // documentation inherited from interface KeyListener
  public void keyPressed (java.awt.event.KeyEvent e)
  {
    _modifiers = convertModifiers(e.getModifiersEx());
    keyPressed(e.getWhen(), e.getKeyChar(), convertKeyCode(e), e.isConsumed());
  }

  // documentation inherited from interface KeyListener
  public void keyReleased (java.awt.event.KeyEvent e)
  {
    _modifiers = convertModifiers(e.getModifiersEx());
    keyReleased(e.getWhen(), e.getKeyChar(), convertKeyCode(e), e.isConsumed());
  }

  // documentation inherited from interface KeyListener
  public void keyTyped (java.awt.event.KeyEvent e)
  {
    // N/A
  }

  @Override
  protected void updateCursor (Cursor cursor)
  {
    if (cursor == null) cursor = getDefaultCursor();
    if (cursor == null) _canvas.setCursor(null);
    else {
      _canvas.setCursor(cursor.getAWTCursor(_canvas.getToolkit()));
      cursor.show(); // hack for GlCanvas
    }
  }

  protected int convertModifiers (int modifiers)
  {
    int nmodifiers = 0;
    if ((modifiers & java.awt.event.InputEvent.BUTTON1_DOWN_MASK) != 0) {
      nmodifiers |= InputEvent.BUTTON1_DOWN_MASK;
    }
    if ((modifiers & java.awt.event.InputEvent.BUTTON3_DOWN_MASK) != 0) {
      nmodifiers |= InputEvent.BUTTON2_DOWN_MASK;
    }
    if ((modifiers & java.awt.event.InputEvent.BUTTON2_DOWN_MASK) != 0) {
      nmodifiers |= InputEvent.BUTTON3_DOWN_MASK;
    }
    if ((modifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0) {
      nmodifiers |= InputEvent.SHIFT_DOWN_MASK;
    }
    if ((modifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
      nmodifiers |= InputEvent.CTRL_DOWN_MASK;
    }
    if ((modifiers & java.awt.event.InputEvent.ALT_DOWN_MASK) != 0) {
      nmodifiers |= InputEvent.ALT_DOWN_MASK;
    }
    if ((modifiers & java.awt.event.InputEvent.META_DOWN_MASK) != 0) {
      nmodifiers |= InputEvent.META_DOWN_MASK;
    }
    return nmodifiers;
  }

  protected int convertButton (java.awt.event.MouseEvent e)
  {
    // OpenGL and the AWT disagree about mouse button numbering (AWT
    // is left=1 middle=2 right=3, OpenGL is left=0 middle=2 right=1)
    switch (e.getButton()) {
    case java.awt.event.MouseEvent.BUTTON1: return MouseEvent.BUTTON1;
    case java.awt.event.MouseEvent.BUTTON3: return MouseEvent.BUTTON2;
    case java.awt.event.MouseEvent.BUTTON2: return MouseEvent.BUTTON3;
    case 0: return -1; // this is generated when we wheel
    default:
      Log.log.warning("Requested to map unknown button '" +
              e.getButton() + "'.");
      return e.getButton();
    }
  }

  protected int convertKeyCode (java.awt.event.KeyEvent e)
  {
    boolean numpad = (e.getKeyLocation() == java.awt.event.KeyEvent.KEY_LOCATION_NUMPAD);
    boolean left = (e.getKeyLocation() == java.awt.event.KeyEvent.KEY_LOCATION_LEFT);
    switch (e.getKeyCode()) {
    case java.awt.event.KeyEvent.VK_ESCAPE: return GLFW.GLFW_KEY_ESCAPE;
    case java.awt.event.KeyEvent.VK_1: return GLFW.GLFW_KEY_1;
    case java.awt.event.KeyEvent.VK_2: return GLFW.GLFW_KEY_2;
    case java.awt.event.KeyEvent.VK_3: return GLFW.GLFW_KEY_3;
    case java.awt.event.KeyEvent.VK_4: return GLFW.GLFW_KEY_4;
    case java.awt.event.KeyEvent.VK_5: return GLFW.GLFW_KEY_5;
    case java.awt.event.KeyEvent.VK_6: return GLFW.GLFW_KEY_6;
    case java.awt.event.KeyEvent.VK_7: return GLFW.GLFW_KEY_7;
    case java.awt.event.KeyEvent.VK_8: return GLFW.GLFW_KEY_8;
    case java.awt.event.KeyEvent.VK_9: return GLFW.GLFW_KEY_9;
    case java.awt.event.KeyEvent.VK_0: return GLFW.GLFW_KEY_0;
    case java.awt.event.KeyEvent.VK_MINUS: return GLFW.GLFW_KEY_MINUS;
    case java.awt.event.KeyEvent.VK_EQUALS:
      return numpad ? GLFW.GLFW_KEY_KP_EQUAL : GLFW.GLFW_KEY_EQUAL;
    case java.awt.event.KeyEvent.VK_BACK_SPACE: return GLFW.GLFW_KEY_BACKSPACE;
    case java.awt.event.KeyEvent.VK_TAB: return GLFW.GLFW_KEY_TAB;
    case java.awt.event.KeyEvent.VK_Q: return GLFW.GLFW_KEY_Q;
    case java.awt.event.KeyEvent.VK_W: return GLFW.GLFW_KEY_W;
    case java.awt.event.KeyEvent.VK_E: return GLFW.GLFW_KEY_E;
    case java.awt.event.KeyEvent.VK_R: return GLFW.GLFW_KEY_R;
    case java.awt.event.KeyEvent.VK_T: return GLFW.GLFW_KEY_T;
    case java.awt.event.KeyEvent.VK_Y: return GLFW.GLFW_KEY_Y;
    case java.awt.event.KeyEvent.VK_U: return GLFW.GLFW_KEY_U;
    case java.awt.event.KeyEvent.VK_I: return GLFW.GLFW_KEY_I;
    case java.awt.event.KeyEvent.VK_O: return GLFW.GLFW_KEY_O;
    case java.awt.event.KeyEvent.VK_P: return GLFW.GLFW_KEY_P;
    case java.awt.event.KeyEvent.VK_OPEN_BRACKET: return GLFW.GLFW_KEY_LEFT_BRACKET;
    case java.awt.event.KeyEvent.VK_CLOSE_BRACKET: return GLFW.GLFW_KEY_RIGHT_BRACKET;
    case java.awt.event.KeyEvent.VK_ENTER:
      return numpad ? GLFW.GLFW_KEY_KP_ENTER : GLFW.GLFW_KEY_ENTER;
    case java.awt.event.KeyEvent.VK_CONTROL:
      return left ? GLFW.GLFW_KEY_LEFT_CONTROL : GLFW.GLFW_KEY_RIGHT_CONTROL;
    case java.awt.event.KeyEvent.VK_A: return GLFW.GLFW_KEY_A;
    case java.awt.event.KeyEvent.VK_S: return GLFW.GLFW_KEY_S;
    case java.awt.event.KeyEvent.VK_D: return GLFW.GLFW_KEY_D;
    case java.awt.event.KeyEvent.VK_F: return GLFW.GLFW_KEY_F;
    case java.awt.event.KeyEvent.VK_G: return GLFW.GLFW_KEY_G;
    case java.awt.event.KeyEvent.VK_H: return GLFW.GLFW_KEY_H;
    case java.awt.event.KeyEvent.VK_J: return GLFW.GLFW_KEY_J;
    case java.awt.event.KeyEvent.VK_K: return GLFW.GLFW_KEY_K;
    case java.awt.event.KeyEvent.VK_L: return GLFW.GLFW_KEY_L;
    case java.awt.event.KeyEvent.VK_SEMICOLON: return GLFW.GLFW_KEY_SEMICOLON;
    case java.awt.event.KeyEvent.VK_QUOTE: return GLFW.GLFW_KEY_APOSTROPHE;
    case java.awt.event.KeyEvent.VK_BACK_QUOTE: return GLFW.GLFW_KEY_GRAVE_ACCENT;
    case java.awt.event.KeyEvent.VK_SHIFT:
      return left ? GLFW.GLFW_KEY_LEFT_SHIFT : GLFW.GLFW_KEY_RIGHT_SHIFT;
    case java.awt.event.KeyEvent.VK_BACK_SLASH: return GLFW.GLFW_KEY_BACKSLASH;
    case java.awt.event.KeyEvent.VK_Z: return GLFW.GLFW_KEY_Z;
    case java.awt.event.KeyEvent.VK_X: return GLFW.GLFW_KEY_X;
    case java.awt.event.KeyEvent.VK_C: return GLFW.GLFW_KEY_C;
    case java.awt.event.KeyEvent.VK_V: return GLFW.GLFW_KEY_V;
    case java.awt.event.KeyEvent.VK_B: return GLFW.GLFW_KEY_B;
    case java.awt.event.KeyEvent.VK_N: return GLFW.GLFW_KEY_N;
    case java.awt.event.KeyEvent.VK_M: return GLFW.GLFW_KEY_M;
    case java.awt.event.KeyEvent.VK_COMMA:
      // GLFW has no separate numpad-comma; map both to the plain comma key.
      return GLFW.GLFW_KEY_COMMA;
    case java.awt.event.KeyEvent.VK_PERIOD: return GLFW.GLFW_KEY_PERIOD;
    case java.awt.event.KeyEvent.VK_SLASH: return GLFW.GLFW_KEY_SLASH;
    case java.awt.event.KeyEvent.VK_MULTIPLY: return GLFW.GLFW_KEY_KP_MULTIPLY;
    case java.awt.event.KeyEvent.VK_SPACE: return GLFW.GLFW_KEY_SPACE;
    case java.awt.event.KeyEvent.VK_CAPS_LOCK: return GLFW.GLFW_KEY_CAPS_LOCK;
    case java.awt.event.KeyEvent.VK_F1: return GLFW.GLFW_KEY_F1;
    case java.awt.event.KeyEvent.VK_F2: return GLFW.GLFW_KEY_F2;
    case java.awt.event.KeyEvent.VK_F3: return GLFW.GLFW_KEY_F3;
    case java.awt.event.KeyEvent.VK_F4: return GLFW.GLFW_KEY_F4;
    case java.awt.event.KeyEvent.VK_F5: return GLFW.GLFW_KEY_F5;
    case java.awt.event.KeyEvent.VK_F6: return GLFW.GLFW_KEY_F6;
    case java.awt.event.KeyEvent.VK_F7: return GLFW.GLFW_KEY_F7;
    case java.awt.event.KeyEvent.VK_F8: return GLFW.GLFW_KEY_F8;
    case java.awt.event.KeyEvent.VK_F9: return GLFW.GLFW_KEY_F9;
    case java.awt.event.KeyEvent.VK_F10: return GLFW.GLFW_KEY_F10;
    case java.awt.event.KeyEvent.VK_F11: return GLFW.GLFW_KEY_F11;
    case java.awt.event.KeyEvent.VK_F12: return GLFW.GLFW_KEY_F12;
    case java.awt.event.KeyEvent.VK_F13: return GLFW.GLFW_KEY_F13;
    case java.awt.event.KeyEvent.VK_F14: return GLFW.GLFW_KEY_F14;
    case java.awt.event.KeyEvent.VK_F15: return GLFW.GLFW_KEY_F15;
    case java.awt.event.KeyEvent.VK_NUM_LOCK: return GLFW.GLFW_KEY_NUM_LOCK;
    case java.awt.event.KeyEvent.VK_SCROLL_LOCK: return GLFW.GLFW_KEY_SCROLL_LOCK;
    case java.awt.event.KeyEvent.VK_NUMPAD0: return GLFW.GLFW_KEY_KP_0;
    case java.awt.event.KeyEvent.VK_NUMPAD1: return GLFW.GLFW_KEY_KP_1;
    case java.awt.event.KeyEvent.VK_NUMPAD2: return GLFW.GLFW_KEY_KP_2;
    case java.awt.event.KeyEvent.VK_NUMPAD3: return GLFW.GLFW_KEY_KP_3;
    case java.awt.event.KeyEvent.VK_NUMPAD4: return GLFW.GLFW_KEY_KP_4;
    case java.awt.event.KeyEvent.VK_NUMPAD5: return GLFW.GLFW_KEY_KP_5;
    case java.awt.event.KeyEvent.VK_NUMPAD6: return GLFW.GLFW_KEY_KP_6;
    case java.awt.event.KeyEvent.VK_NUMPAD7: return GLFW.GLFW_KEY_KP_7;
    case java.awt.event.KeyEvent.VK_NUMPAD8: return GLFW.GLFW_KEY_KP_8;
    case java.awt.event.KeyEvent.VK_NUMPAD9: return GLFW.GLFW_KEY_KP_9;
    case java.awt.event.KeyEvent.VK_DECIMAL: return GLFW.GLFW_KEY_KP_DECIMAL;
    case java.awt.event.KeyEvent.VK_DIVIDE: return GLFW.GLFW_KEY_KP_DIVIDE;
    case java.awt.event.KeyEvent.VK_SUBTRACT: return GLFW.GLFW_KEY_KP_SUBTRACT;
    case java.awt.event.KeyEvent.VK_ADD: return GLFW.GLFW_KEY_KP_ADD;
    case java.awt.event.KeyEvent.VK_PRINTSCREEN: return GLFW.GLFW_KEY_PRINT_SCREEN;
    case java.awt.event.KeyEvent.VK_PAUSE: return GLFW.GLFW_KEY_PAUSE;
    case java.awt.event.KeyEvent.VK_HOME: return GLFW.GLFW_KEY_HOME;
    case java.awt.event.KeyEvent.VK_UP: return GLFW.GLFW_KEY_UP;
    case java.awt.event.KeyEvent.VK_PAGE_UP: return GLFW.GLFW_KEY_PAGE_UP;
    case java.awt.event.KeyEvent.VK_LEFT: return GLFW.GLFW_KEY_LEFT;
    case java.awt.event.KeyEvent.VK_RIGHT: return GLFW.GLFW_KEY_RIGHT;
    case java.awt.event.KeyEvent.VK_END: return GLFW.GLFW_KEY_END;
    case java.awt.event.KeyEvent.VK_DOWN: return GLFW.GLFW_KEY_DOWN;
    case java.awt.event.KeyEvent.VK_PAGE_DOWN: return GLFW.GLFW_KEY_PAGE_DOWN;
    case java.awt.event.KeyEvent.VK_INSERT: return GLFW.GLFW_KEY_INSERT;
    case java.awt.event.KeyEvent.VK_DELETE: return GLFW.GLFW_KEY_DELETE;
    // VK_KANA, VK_CONVERT, VK_NONCONVERT, VK_CIRCUMFLEX, VK_AT, VK_COLON,
    // VK_UNDERSCORE, VK_KANJI, VK_STOP, VK_UNDEFINED — no GLFW equivalent.
    default: return GLFW.GLFW_KEY_UNKNOWN;
    }
  }

  protected GlCanvas _canvas;
}
