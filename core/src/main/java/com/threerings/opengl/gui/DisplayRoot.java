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

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWGamepadState;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;

import com.threerings.math.FloatMath;

import com.threerings.opengl.GlDisplayApp;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.ControllerEvent;
import com.threerings.opengl.gui.event.InputEvent;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.text.IMEComponent;
import com.threerings.opengl.gui.util.PseudoKeys;
import static com.threerings.opengl.gui.Log.log;


/**
 * A root for GLFW-window-based apps.
 */
public class DisplayRoot extends Root
{
  public DisplayRoot (GlContext ctx)
  {
    super(ctx);

    // Try to set up GLFW callbacks now; if the window doesn't exist yet
    // (created before startup()), they'll be installed lazily in poll().
    initCallbacksIfNeeded();
  }

  /**
   * Sets up GLFW callbacks if the window is available and callbacks haven't been installed yet.
   */
  protected void initCallbacksIfNeeded ()
  {
    if (!_callbacksInstalled && _ctx.getApp() instanceof GlDisplayApp) {
      _glfwWindow = ((GlDisplayApp)_ctx.getApp()).getWindow();
      if (_glfwWindow != 0) {
        setupCallbacks(_glfwWindow);
        _callbacksInstalled = true;
      }
    }
  }

  /**
   * Polls the input system for events and dispatches them.
   */
  public void poll ()
  {
    // Lazily install GLFW callbacks once the window exists
    initCallbacksIfNeeded();

    // Flush any key event that didn't get a matching char callback
    flushPendingKeyEvent();

    // Process buffered events from GLFW callbacks
    List<Runnable> events;
    synchronized (_eventQueue) {
      events = new ArrayList<>(_eventQueue);
      _eventQueue.clear();
    }
    for (Runnable event : events) {
      try {
        event.run();
      } catch (Exception e) {
        log.warning("Exception dispatching input event.", e);
      }
    }

    // make sure the pressed keys are really pressed
    if (_isActive && !_pressedKeys.isEmpty()) {
      long window = getWindow();
      if (window != 0) {
        for (Iterator<KeyRecord> it = _pressedKeys.values().iterator(); it.hasNext(); ) {
          KeyRecord record = it.next();
          KeyEvent press = record.getPress();
          int key = press.getKeyCode();
          if (key != GLFW.GLFW_KEY_UNKNOWN &&
              GLFW.glfwGetKey(window, key) != GLFW.GLFW_PRESS) {
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

    // Poll gamepads and generate controller events by diffing against previous state
    pollGamepads();
  }

  /**
   * Flushes a buffered key event that never received a matching char callback.
   * This happens for keys that glfwGetKeyName reports as printable but that don't
   * generate a char event (e.g., due to dead keys or modifier combos).
   *
   * EXPERIMENTAL: previously we dispatched these with {@code keyChar = (char)0}, which
   * meant focused text fields couldn't insert anything (their {@code insertChar(0)}
   * rejects the NUL as an ISO control character) and the unconsumed event would bubble
   * up to whatever default key handlers the host app had — surfacing as the wrong
   * actions firing while the user is typing. The Steam Deck's floating gamepad text
   * input ("on-screen keyboard") reliably triggers this path: Steam injects OS-level
   * key events but the matching char events don't always come through, especially in
   * a second OSK session opened after a prior dismissal.
   *
   * The attempted fix here: derive a printable character from the buffered keycode
   * using the layout-aware {@link GLFW#glfwGetKeyName} (which returns the unshifted
   * symbol for the key, e.g. {@code "o"} for {@code GLFW_KEY_O}) and uppercase letters
   * if the modifier mask had Shift held. Non-letter shift mappings (US 1→!, etc.) are
   * locale-dependent and we don't have that info here, so we leave them as-is —
   * acceptable since the char callback is the proper source of truth and only fails
   * to arrive in unusual situations like the OSK case above.
   */
  protected void flushPendingKeyEvent ()
  {
    if (_pendingKey >= 0) {
      int key = _pendingKey;
      boolean pressed = _pendingKeyPressed;
      int scancode = _pendingScancode;
      int mods = _pendingMods;
      _pendingKey = -1;
      String name = GLFW.glfwGetKeyName(key, scancode);
      char ch = (name != null && !name.isEmpty()) ? name.charAt(0) : (char)0;
      if ((mods & GLFW.GLFW_MOD_SHIFT) != 0 && Character.isLetter(ch)) {
        ch = Character.toUpperCase(ch);
      }
      final char keyChar = ch;
      synchronized (_eventQueue) {
        _eventQueue.add(() -> {
          if (pressed) {
            keyPressed(_tickStamp, keyChar, key, false);
          } else {
            keyReleased(_tickStamp, keyChar, key, false);
          }
          updateKeyModifier(key, pressed);
        });
      }
    }
  }

  /**
   * Polls GLFW gamepads and dispatches {@link ControllerEvent}s for state changes.
   */
  protected void pollGamepads ()
  {
    for (int jid = GLFW.GLFW_JOYSTICK_1; jid <= GLFW.GLFW_JOYSTICK_LAST; jid++) {
      if (!GLFW.glfwJoystickPresent(jid)) {
        continue;
      }
      // Ensure we have state storage for this joystick
      if (_gamepadStates[jid] == null) {
        _gamepadStates[jid] = GLFWGamepadState.calloc();
        _prevGamepadStates[jid] = GLFWGamepadState.calloc();
      }

      // Swap current → previous, read new state into current
      GLFWGamepadState tmp = _prevGamepadStates[jid];
      _prevGamepadStates[jid] = _gamepadStates[jid];
      _gamepadStates[jid] = tmp;

      if (GLFW.glfwJoystickIsGamepad(jid)) {
        GLFW.glfwGetGamepadState(jid, _gamepadStates[jid]);
      } else {
        // Fall back to raw joystick API for non-mapped controllers
        readRawJoystickState(jid, _gamepadStates[jid]);
      }

      GLFWGamepadState cur = _gamepadStates[jid];
      GLFWGamepadState prev = _prevGamepadStates[jid];

      // Diff buttons (indices 0 through GLFW_GAMEPAD_BUTTON_LAST)
      int buttonCount = GLFW.GLFW_GAMEPAD_BUTTON_LAST + 1;
      for (int bi = 0; bi < buttonCount; bi++) {
        byte wasPressed = prev.buttons(bi);
        byte isPressed = cur.buttons(bi);
        if (wasPressed != isPressed) {
          int type = (isPressed != 0)
            ? ControllerEvent.CONTROLLER_PRESSED
            : ControllerEvent.CONTROLLER_RELEASED;
          dispatchEvent(getFocus(), new ControllerEvent(
            this, _tickStamp, _modifiers, type, bi));
        }
      }

      // Diff axes (indices 0 through GLFW_GAMEPAD_AXIS_LAST)
      int axisCount = GLFW.GLFW_GAMEPAD_AXIS_LAST + 1;
      for (int ai = 0; ai < axisCount; ai++) {
        float wasValue = prev.axes(ai);
        float isValue = cur.axes(ai);
        if (wasValue != isValue) {
          boolean xAxis = (ai == GLFW.GLFW_GAMEPAD_AXIS_LEFT_X ||
                           ai == GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X);
          boolean yAxis = (ai == GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y ||
                           ai == GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y);
          dispatchEvent(getFocus(), new ControllerEvent(
            this, _tickStamp, _modifiers,
            ControllerEvent.CONTROLLER_MOVED,
            ai, xAxis, yAxis, isValue));
        }
      }
    }
  }

  /**
   * Reads raw joystick data into a GLFWGamepadState for non-mapped controllers.
   */
  protected void readRawJoystickState (int jid, GLFWGamepadState state)
  {
    java.nio.FloatBuffer axes = GLFW.glfwGetJoystickAxes(jid);
    if (axes != null) {
      for (int ii = 0; ii < Math.min(axes.remaining(), GLFW.GLFW_GAMEPAD_AXIS_LAST + 1); ii++) {
        state.axes(ii, axes.get(ii));
      }
    }
    java.nio.ByteBuffer buttons = GLFW.glfwGetJoystickButtons(jid);
    if (buttons != null) {
      for (int ii = 0; ii < Math.min(buttons.remaining(), GLFW.GLFW_GAMEPAD_BUTTON_LAST + 1); ii++) {
        state.buttons(ii, buttons.get(ii));
      }
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
  public String getClipboardText ()
  {
    return (_glfwWindow != 0) ? GLFW.glfwGetClipboardString(_glfwWindow) : null;
  }

  @Override
  public void setClipboardText (String text)
  {
    if (_glfwWindow != 0 && text != null) {
      GLFW.glfwSetClipboardString(_glfwWindow, text);
    }
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
      // x, y are in logical coords (y-up from bottom-left). Convert to framebuffer
      // pixels via _scale, then to GLFW cursor coords (y-down) via the cursor scale.
      int[] fh = { 0 };
      GLFW.glfwGetFramebufferSize(window, null, fh);
      float cs = _ctx.getWindowScaleFactor();
      GLFW.glfwSetCursorPos(window,
        (x * _scale) / cs,
        (fh[0] - y * _scale - 1) / cs);
    }
    super.setMousePosition(x, y);
  }

  @Override
  protected void updateCursor (Cursor cursor)
  {
    if (_glfwWindow == 0) return;
    if (cursor == null) cursor = getDefaultCursor();
    if (cursor == null) GLFW.glfwSetCursor(_glfwWindow, 0);
    else cursor.show(_glfwWindow);
  }

  /**
   * Sets up GLFW input callbacks on the given window.
   */
  protected void setupCallbacks (long window)
  {
    // Store strong references to all callbacks to prevent GC from collecting them.
    // GLFW only holds native references; without Java-side refs, the GC collects
    // the callback objects and input silently stops working.
    // GLFW delivers key codes and characters as separate callbacks, firing
    // synchronously during glfwPollEvents (key first, then char). We buffer
    // the key event and merge the character from the char callback before
    // enqueueing, so the BUI event carries both the key code AND the correct
    // modifier-aware character (e.g., Shift+A → 'A', Shift+hyphen → '_').
    _keyCallback = new GLFWKeyCallback() {
      @Override
      public void invoke (long window, int glfwKey, int scancode, int action, int mods) {
        // Flush any previous buffered key event that never got a char callback
        flushPendingKeyEvent();

        int key = glfwKey;
        boolean pressed = (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT);
        // glfwGetKeyName returns null for SPACE (it's whitespace with no displayable glyph),
        // but the char callback still fires for codepoint 32. Treat space as printable so it
        // goes through the buffer/merge path; otherwise we'd dispatch both a key=SPACE event
        // from here and a stray key=KEY_NONE event from the char callback.
        boolean printable = glfwKey == GLFW.GLFW_KEY_SPACE
          || GLFW.glfwGetKeyName(glfwKey, scancode) != null;

        if (printable && pressed) {
          // Buffer this key press — the char callback will provide the character.
          // We also stash the scancode and modifier mask so that, if the char callback
          // never arrives, flushPendingKeyEvent can derive a printable character from
          // the keycode rather than dispatching with (char)0. (See the long-form
          // comment on flushPendingKeyEvent for the rationale.)
          _pendingKey = key;
          _pendingKeyPressed = true;
          _pendingScancode = scancode;
          _pendingMods = mods;
        } else {
          // Non-printable key or release: enqueue immediately with no character
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
          _pendingKey = -1;
        }
      }
    };
    GLFW.glfwSetKeyCallback(window, _keyCallback);

    _charCallback = new GLFWCharCallback() {
      @Override
      public void invoke (long window, int codepoint) {
        char ch = (char)codepoint;
        if (_pendingKey >= 0) {
          // Merge the character with the buffered key event
          int key = _pendingKey;
          _pendingKey = -1;
          synchronized (_eventQueue) {
            _eventQueue.add(() -> {
              keyPressed(_tickStamp, ch, key, false);
              updateKeyModifier(key, true);
            });
          }
        } else {
          // Standalone character (IME, composed input)
          synchronized (_eventQueue) {
            _eventQueue.add(() -> {
              keyPressed(_tickStamp, ch, PseudoKeys.KEY_NONE, false);
            });
          }
        }
      }
    };
    GLFW.glfwSetCharCallback(window, _charCallback);

    _mouseButtonCallback = new GLFWMouseButtonCallback() {
      @Override
      public void invoke (long window, int button, int action, int mods) {
        boolean pressed = (action == GLFW.GLFW_PRESS);
        // Capture scaled coordinates immediately
        double[] xpos = new double[1], ypos = new double[1];
        GLFW.glfwGetCursorPos(window, xpos, ypos);
        float scale = _ctx.getWindowScaleFactor();
        int[] fh = new int[1];
        GLFW.glfwGetFramebufferSize(window, null, fh);
        int x = (int)(xpos[0] * scale);
        int y = fh[0] - (int)(ypos[0] * scale) - 1;
        synchronized (_eventQueue) {
          _eventQueue.add(() -> {
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
    };
    GLFW.glfwSetMouseButtonCallback(window, _mouseButtonCallback);

    _cursorPosCallback = new GLFWCursorPosCallback() {
      @Override
      public void invoke (long window, double xpos, double ypos) {
        float scale = _ctx.getWindowScaleFactor();
        int[] fh = new int[1];
        GLFW.glfwGetFramebufferSize(window, null, fh);
        int x = (int)(xpos * scale);
        int y = fh[0] - (int)(ypos * scale) - 1;
        synchronized (_eventQueue) {
          _eventQueue.add(() -> {
            mouseMoved(_tickStamp, x, y, false);
          });
        }
      }
    };
    GLFW.glfwSetCursorPosCallback(window, _cursorPosCallback);

    _scrollCallback = new GLFWScrollCallback() {
      @Override
      public void invoke (long window, double xoffset, double yoffset) {
        double[] xpos = new double[1], ypos2 = new double[1];
        GLFW.glfwGetCursorPos(window, xpos, ypos2);
        float scale = _ctx.getWindowScaleFactor();
        int[] fh = new int[1];
        GLFW.glfwGetFramebufferSize(window, null, fh);
        int x = (int)(xpos[0] * scale);
        int y = fh[0] - (int)(ypos2[0] * scale) - 1;
        int delta = (yoffset > 0) ? +1 : -1;
        synchronized (_eventQueue) {
          _eventQueue.add(() -> {
            mouseWheeled(_tickStamp, x, y, delta, false);
          });
        }
      }
    };
    GLFW.glfwSetScrollCallback(window, _scrollCallback);

    _focusCallback = new GLFWWindowFocusCallback() {
      @Override
      public void invoke (long window, boolean focused) {
        _isActive = focused;
      }
    };
    GLFW.glfwSetWindowFocusCallback(window, _focusCallback);
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
      case GLFW.GLFW_KEY_LEFT_SHIFT:
      case GLFW.GLFW_KEY_RIGHT_SHIFT:
        mask = InputEvent.SHIFT_DOWN_MASK;
        okey = (key == GLFW.GLFW_KEY_LEFT_SHIFT)
          ? GLFW.GLFW_KEY_RIGHT_SHIFT : GLFW.GLFW_KEY_LEFT_SHIFT;
        break;
      case GLFW.GLFW_KEY_LEFT_CONTROL:
      case GLFW.GLFW_KEY_RIGHT_CONTROL:
        mask = InputEvent.CTRL_DOWN_MASK;
        okey = (key == GLFW.GLFW_KEY_LEFT_CONTROL)
          ? GLFW.GLFW_KEY_RIGHT_CONTROL : GLFW.GLFW_KEY_LEFT_CONTROL;
        break;
      case GLFW.GLFW_KEY_LEFT_ALT:
      case GLFW.GLFW_KEY_RIGHT_ALT:
        mask = InputEvent.ALT_DOWN_MASK;
        okey = (key == GLFW.GLFW_KEY_LEFT_ALT)
          ? GLFW.GLFW_KEY_RIGHT_ALT : GLFW.GLFW_KEY_LEFT_ALT;
        break;
      case GLFW.GLFW_KEY_LEFT_SUPER:
      case GLFW.GLFW_KEY_RIGHT_SUPER:
        mask = InputEvent.META_DOWN_MASK;
        okey = (key == GLFW.GLFW_KEY_LEFT_SUPER)
          ? GLFW.GLFW_KEY_RIGHT_SUPER : GLFW.GLFW_KEY_LEFT_SUPER;
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

  /** Whether GLFW callbacks have been installed. */
  protected boolean _callbacksInstalled;

  /** Buffered key code waiting for a char callback to provide the character, or -1. */
  protected int _pendingKey = -1;

  /** Whether the pending key event is a press (vs release). */
  protected boolean _pendingKeyPressed;

  /** Scancode of the buffered key, used by {@link #flushPendingKeyEvent} to derive a
   * char via {@link GLFW#glfwGetKeyName} when the char callback never arrives. */
  protected int _pendingScancode;

  /** Modifier mask of the buffered key, used to apply shift to the derived char. */
  protected int _pendingMods;

  /** Strong references to GLFW callbacks to prevent GC collection. */
  protected GLFWKeyCallback _keyCallback;
  protected GLFWCharCallback _charCallback;
  protected GLFWMouseButtonCallback _mouseButtonCallback;
  protected GLFWCursorPosCallback _cursorPosCallback;
  protected GLFWScrollCallback _scrollCallback;
  protected GLFWWindowFocusCallback _focusCallback;

  /** The GLFW window handle (cached to avoid repeated lookups). */
  protected long _glfwWindow;

  /** Queue of input events from GLFW callbacks. */
  protected final List<Runnable> _eventQueue = new ArrayList<>();

  /** Current and previous gamepad states for diffing, indexed by joystick ID. */
  protected GLFWGamepadState[] _gamepadStates =
    new GLFWGamepadState[GLFW.GLFW_JOYSTICK_LAST + 1];
  protected GLFWGamepadState[] _prevGamepadStates =
    new GLFWGamepadState[GLFW.GLFW_JOYSTICK_LAST + 1];
}
