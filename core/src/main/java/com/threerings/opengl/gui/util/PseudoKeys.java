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

package com.threerings.opengl.gui.util;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.ListMultimap;

import org.lwjgl.glfw.GLFW;

import com.samskivert.util.HashIntSet;

import com.google.common.collect.Maps;

import com.samskivert.util.IntTuple;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.opengl.gui.event.ControllerEvent;
import com.threerings.opengl.gui.event.ControllerListener;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.event.KeyListener;
import com.threerings.opengl.gui.event.MouseAdapter;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.event.MouseWheelListener;

import static com.threerings.opengl.gui.Log.log;

/**
 * Provides a unified system for handling keys, mouse buttons, and other key-like features.  The
 * pseudo-keys share the integer identifier space with GLFW's {@code GLFW_KEY_*} constants: real
 * keyboard keys use their GLFW codes (0..{@link GLFW#GLFW_KEY_LAST}), and pseudo-keys start at
 * {@link #KEY_BASE}, safely above the GLFW range.
 */
public class PseudoKeys
{
  /**
   * Get the singleton PseudoKeys instance.
   */
  public static PseudoKeys singleton ()
  {
    return _singleton;
  }

  /**
   * An interface for objects listening for key press and release events.
   */
  public interface Observer
  {
    /**
     * Called when a "key" (either a real key or a pseudo-key) is pressed.
     */
    public void keyPressed (long when, int key, float amount);

    /**
     * Called when a "key" is released.
     */
    public void keyReleased (long when, int key);
  }

  /**
   * For convenience, provides no-op implementations of the observer interface.
   */
  public static class Adapter
    implements Observer
  {
    // documentation inherited from interface Observer
    public void keyPressed (long when, int key, float amount)
    {
      // no-op
    }

    // documentation inherited from interface Observer
    public void keyReleased (long when, int key)
    {
      // no-op
    }
  }

  /**
   * Processes unconsumed key, mouse, and controller events, converting them into unified "key"
   * events.  Users may either subclass this adapter or pass an observer to the constructor.
   */
  public static class Unifier extends MouseAdapter
    implements KeyListener, MouseWheelListener, ControllerListener
  {
    /**
     * Constructor for observers.
     */
    public Unifier (Observer observer)
    {
      _observer = observer;
    }

    /**
     * Constructor for subclasses.
     */
    public Unifier (boolean consume)
    {
      _consume = consume;
    }

    /**
     * Constructor for subclasses.
     */
    public Unifier ()
    {
      this(false);
    }

    /**
     * Called when a "key" (either a real key or a pseudo-key) is pressed.  Default
     * implementation forwards the event to the observer, if any.
     */
    public void keyPressed (long when, int key, float amount)
    {
      if (_observer == null) {
        return;
      }

      int modKey = getModifierKey(key);
      if (modKey == 0 && !_modifierUsers.contains(key)) {
        // Not a modifier, and we don't care about modifiers.
        _observer.keyPressed(when, key, amount);
        return;
      }

      if (modKey == 0) {
        key = applyModifierKeys(key);
        _pressedKeys.add(key);
        _observer.keyPressed(when, key, amount);
      } else {
        holdModifierKey(modKey, true);
        for (int m : currentPressedKeys()) {
          _observer.keyReleased(when, m);
          _pressedKeys.remove(m);
          m = applyModifierKeys(m);
          _pressedKeys.add(m);
          _observer.keyPressed(when, m, amount);
        }
      }
    }

    /**
     * Called when a "key" is released.  Default implementation forwards the event to the
     * observer, if any.
     */
    public void keyReleased (long when, int key)
    {
      if (_observer == null) {
        return;
      }

      int modKey = getModifierKey(key);
      if (modKey == 0 && !_modifierUsers.contains(key)) {
        // Not a modifier, and we don't care about modifiers.
        _observer.keyReleased(when, key);
        return;
      }

      if (modKey == 0) {
        key = applyModifierKeys(key);
        _observer.keyReleased(when, key);
        _pressedKeys.remove(key);
      } else {
        holdModifierKey(modKey, false);
        for (int m : currentPressedKeys()) {
          if (singleton().hasModifierKey(m, modKey)) {
            _observer.keyReleased(when, m);
            _pressedKeys.remove(m);
            m = applyModifierKeys(singleton().getBaseKey(m));
            _pressedKeys.add(m);
            _observer.keyPressed(when, m, 1);
          }
        }
      }
    }

    // documentation inherited from interface KeyListener
    public void keyPressed (KeyEvent event)
    {
      if (!(event.isConsumed() || event.isRepeat())) {
        keyPressed(event.getWhen(), event.getKeyCode(), 1f);
        if (_consume) {
          event.consume();
        }
      }
    }

    // documentation inherited from interface KeyListener
    public void keyReleased (KeyEvent event)
    {
      if (!event.isConsumed()) {
        keyReleased(event.getWhen(), event.getKeyCode());
        if (_consume) {
          event.consume();
        }
      }
    }

    // documentation inherited from interface MouseWheelListener
    public void mouseWheeled (MouseEvent event)
    {
      if (event.isConsumed()) {
        return;
      }
      long when = event.getWhen();
      int delta = event.getDelta();
      if (delta > 0) {
        keyPressed(when, KEY_WHEEL_UP, delta);
        keyReleased(when, KEY_WHEEL_UP);
      } else {
        keyPressed(when, KEY_WHEEL_DOWN, -delta);
        keyReleased(when, KEY_WHEEL_DOWN);
      }
      if (_consume) {
        event.consume();
      }
    }

    // documentation inherited from interface ControllerListener
    public void controllerPressed (ControllerEvent event)
    {
      if (!event.isConsumed()) {
        keyPressed(event.getWhen(), getControllerKey(KEY_CONTROLLER_BUTTON,
          0, event.getControlIndex()), 1f);
        if (_consume) {
          event.consume();
        }
      }
    }

    // documentation inherited from interface ControllerListener
    public void controllerReleased (ControllerEvent event)
    {
      if (!event.isConsumed()) {
        keyReleased(event.getWhen(), getControllerKey(KEY_CONTROLLER_BUTTON,
          0, event.getControlIndex()));
        if (_consume) {
          event.consume();
        }
      }
    }

    // documentation inherited from interface ControllerListener
    public void controllerMoved (ControllerEvent event)
    {
      if (event.isConsumed()) {
        return;
      }
      Object controller = event.getController();
      int controllerIndex = 0;
      int axisIndex = event.getControlIndex();
      float value = event.getValue();
      float dead = 0.2f;
      if (value > dead) {
        Integer okey = _axes.put(new IntTuple(controllerIndex, axisIndex),
          KEY_CONTROLLER_AXIS_POSITIVE);
        if (okey != null && okey == KEY_CONTROLLER_AXIS_NEGATIVE) {
          keyReleased(event.getWhen(), getControllerKey(
            okey, controllerIndex, axisIndex));
        }
        keyPressed(event.getWhen(), getControllerKey(
          KEY_CONTROLLER_AXIS_POSITIVE, controllerIndex, axisIndex), value);

      } else if (value < -dead) {
        Integer okey = _axes.put(new IntTuple(controllerIndex, axisIndex),
          KEY_CONTROLLER_AXIS_NEGATIVE);
        if (okey != null && okey == KEY_CONTROLLER_AXIS_POSITIVE) {
          keyReleased(event.getWhen(), getControllerKey(
            okey, controllerIndex, axisIndex));
        }
        keyPressed(event.getWhen(), getControllerKey(
          KEY_CONTROLLER_AXIS_NEGATIVE, controllerIndex, axisIndex), -value);

      } else {
        Integer okey = _axes.remove(new IntTuple(controllerIndex, axisIndex));
        if (okey != null) {
          keyReleased(event.getWhen(), getControllerKey(
            okey, controllerIndex, axisIndex));
        }
      }
      if (_consume) {
        event.consume();
      }
    }

    // documentation inherited from interface ControllerListener
    public void controllerPovXMoved (ControllerEvent event)
    {
      if (event.isConsumed()) {
        return;
      }
      Object controller = event.getController();
      int controllerIndex = 0;
      float value = event.getValue();
      if (value > 0f) {
        Integer okey = _povx.put(controllerIndex, KEY_CONTROLLER_POV_X_POSITIVE);
        if (okey != null && okey == KEY_CONTROLLER_POV_X_NEGATIVE) {
          keyReleased(event.getWhen(), getControllerKey(okey, controllerIndex, 0));
        }
        keyPressed(event.getWhen(), getControllerKey(
          KEY_CONTROLLER_POV_X_POSITIVE, controllerIndex, 0), value);

      } else if (value < 0f) {
        Integer okey = _povx.put(controllerIndex, KEY_CONTROLLER_POV_X_NEGATIVE);
        if (okey != null && okey == KEY_CONTROLLER_POV_X_POSITIVE) {
          keyReleased(event.getWhen(), getControllerKey(okey, controllerIndex, 0));
        }
        keyPressed(event.getWhen(), getControllerKey(
          KEY_CONTROLLER_POV_X_NEGATIVE, controllerIndex, 0), -value);

      } else { // value == 0f
        Integer okey = _povx.remove(controllerIndex);
        if (okey != null) {
          keyReleased(event.getWhen(), getControllerKey(okey, controllerIndex, 0));
        }
      }
      if (_consume) {
        event.consume();
      }
    }

    // documentation inherited from interface ControllerListener
    public void controllerPovYMoved (ControllerEvent event)
    {
      if (event.isConsumed()) {
        return;
      }
      Object controller = event.getController();
      int controllerIndex = 0;
      float value = event.getValue();
      if (value > 0f) {
        Integer okey = _povy.put(controllerIndex, KEY_CONTROLLER_POV_Y_POSITIVE);
        if (okey != null && okey == KEY_CONTROLLER_POV_Y_NEGATIVE) {
          keyReleased(event.getWhen(), getControllerKey(okey, controllerIndex, 0));
        }
        keyPressed(event.getWhen(), getControllerKey(
          KEY_CONTROLLER_POV_Y_POSITIVE, controllerIndex, 0), value);

      } else if (value < 0f) {
        Integer okey = _povy.put(controllerIndex, KEY_CONTROLLER_POV_Y_NEGATIVE);
        if (okey != null && okey == KEY_CONTROLLER_POV_Y_POSITIVE) {
          keyReleased(event.getWhen(), getControllerKey(okey, controllerIndex, 0));
        }
        keyPressed(event.getWhen(), getControllerKey(
          KEY_CONTROLLER_POV_Y_NEGATIVE, controllerIndex, 0), -value);

      } else { // value == 0f
        Integer okey = _povy.remove(controllerIndex);
        if (okey != null) {
          keyReleased(event.getWhen(), getControllerKey(okey, controllerIndex, 0));
        }
      }
      if (_consume) {
        event.consume();
      }
    }

    @Override
    public void mousePressed (MouseEvent event)
    {
      if (!event.isConsumed()) {
        keyPressed(event.getWhen(), getMouseKey(event.getButton()), 1f);
        if (_consume) {
          event.consume();
        }
      }
    }

    @Override
    public void mouseReleased (MouseEvent event)
    {
      if (!event.isConsumed()) {
        keyReleased(event.getWhen(), getMouseKey(event.getButton()));
        if (_consume) {
          event.consume();
        }
      }
    }

    /**
     * Adds a key to the list of keys which care about modifiers.
     */
    public void addModifierUser (int key)
    {
      if (!_modifierUsers.contains(key)) {
        _modifierUsers.add(key);
      }
    }

    /**
     * Adds a modifier key to the map.
     */
    public void addModifierKey (int key, int modKey)
    {
      _modifierMap.put(key, modKey);
    }

    /**
     * Clears any existing modifier users.
     */
    public void clearModifierUsers ()
    {
      clearHeldKeys();
      _modifierUsers.clear();
    }

    /**
     * Clears any existing modifier keys.
     */
    public void clearModifierKeys ()
    {
      clearHeldKeys();
      _modifierMap.clear();
    }

    /**
     * Clears any held keys.
     */
    protected void clearHeldKeys ()
    {
      _pressedModifiers = 0;
      _pressedKeys.clear();
    }

    /**
     * Returns the current set of pressed keys.
     */
    protected HashIntSet currentPressedKeys ()
    {
      return new HashIntSet(_pressedKeys);
    }

    /**
     * Returns any associated modifier key bit (one of {@link #KEY_MODIFIER1}..{@link
     * #KEY_MODIFIER4}) for the given key, or 0 if this key is not configured as a modifier.
     * Note: this sentinel is 0, not {@link #KEY_NONE} — the callers check {@code modKey == 0}.
     */
    protected int getModifierKey (int key)
    {
      Integer retVal = _modifierMap.get(key);
      return (retVal == null) ? 0 : retVal;
    }

    /**
     * Adds the modifier key flags into the current key.
     */
    protected int applyModifierKeys (int key)
    {
      return _pressedModifiers | key;
    }

    /**
     * Holds/releases a modifier key.
     */
    protected void holdModifierKey (int modKey, boolean held)
    {
      if (held) {
        _pressedModifiers |= modKey;
      } else {
        _pressedModifiers &= ~modKey;
      }
    }

    /** The observer that we notify, if any. */
    protected Observer _observer;

    /** Stores the "keys" pressed for controller/axis combinations. */
    protected Map<IntTuple, Integer> _axes = Maps.newHashMap();

    /** Stores the "keys" pressed for pov x. */
    protected Map<Integer, Integer> _povx = Maps.newHashMap();

    /** Stores the "keys" pressed for pov y. */
    protected Map<Integer, Integer> _povy = Maps.newHashMap();

    /** Stores any pressed keys. */
    protected HashIntSet _pressedKeys = new HashIntSet();

    /** Stores any pressed modifiers. */
    protected int _pressedModifiers;

    /** Stores mappings of modifiers to keys. */
    protected Map<Integer, Integer> _modifierMap = Maps.newHashMap();

    /** Stores a list of keys that actually care about modifier keys. */
    protected HashIntSet _modifierUsers = new HashIntSet();

    /** If we consume all events. */
    protected boolean _consume;
  }

  /** Sentinel for "no key assigned"; matches GLFW's UNKNOWN value (-1). */
  public static final int KEY_NONE = GLFW.GLFW_KEY_UNKNOWN;

  /** First identifier used for pseudo-keys. Chosen well above {@link GLFW#GLFW_KEY_LAST}
   *  (348) to leave headroom if GLFW ever adds more real keys. */
  public static final int KEY_BASE = 512;

  /** A special "key" mapping for the left mouse button. */
  public static final int KEY_BUTTON1 = KEY_BASE;

  /** A special "key" mapping for the right mouse button. */
  public static final int KEY_BUTTON2 = KEY_BASE + 1;

  /** A special "key" mapping for the middle mouse button. */
  public static final int KEY_BUTTON3 = KEY_BASE + 2;

  /** A special "key" mapping for scrolling the mouse wheel up. */
  public static final int KEY_WHEEL_UP = KEY_BASE + 3;

  /** A special "key" mapping for scrolling the mouse wheel down. */
  public static final int KEY_WHEEL_DOWN = KEY_BASE + 4;

  /** A special "key" mapping for a controller button. */
  public static final int KEY_CONTROLLER_BUTTON = KEY_BASE + 5;

  /** A special "key" mapping for positive movement on a controller axis. */
  public static final int KEY_CONTROLLER_AXIS_POSITIVE = KEY_BASE + 6;

  /** A special "key" mapping for negative movement on a controller axis. */
  public static final int KEY_CONTROLLER_AXIS_NEGATIVE = KEY_BASE + 7;

  /** A special "key" mapping for positive movement on a controller pov x axis. */
  public static final int KEY_CONTROLLER_POV_X_POSITIVE = KEY_BASE + 8;

  /** A special "key" mapping for negative movement on a controller pov x axis. */
  public static final int KEY_CONTROLLER_POV_X_NEGATIVE = KEY_BASE + 9;

  /** A special "key" mapping for positive movement on a controller pov y axis. */
  public static final int KEY_CONTROLLER_POV_Y_POSITIVE = KEY_BASE + 10;

  /** A special "key" mapping for negative movement on a controller pov y axis. */
  public static final int KEY_CONTROLLER_POV_Y_NEGATIVE = KEY_BASE + 11;

  /** A special "key" mapping for the 4th mouse button. */
  public static final int KEY_BUTTON4 = KEY_BASE + 12;

  /** A special "key" mapping for the 5th mouse button. */
  public static final int KEY_BUTTON5 = KEY_BASE + 13;
  public static final int KEY_BUTTON6 = KEY_BASE + 14;
  public static final int KEY_BUTTON7 = KEY_BASE + 15;
  public static final int KEY_BUTTON8 = KEY_BASE + 16;
  public static final int KEY_BUTTON9 = KEY_BASE + 17;
  public static final int KEY_BUTTON10 = KEY_BASE + 18;
  public static final int KEY_BUTTON11 = KEY_BASE + 19;
  public static final int KEY_BUTTON12 = KEY_BASE + 20;
  public static final int KEY_BUTTON13 = KEY_BASE + 21;
  public static final int KEY_BUTTON14 = KEY_BASE + 22;
  public static final int KEY_BUTTON15 = KEY_BASE + 23;
  public static final int KEY_BUTTON16 = KEY_BASE + 24;

  public static final int LAST_KEY = KEY_BUTTON16;

  /** A special "key" mapping for modifier key 1. */
  public static final int KEY_MODIFIER1 = 1 << 28;

  /** A special "key" mapping for modifier key 2. */
  public static final int KEY_MODIFIER2 = 1 << 29;

  /** A special "key" mapping for modifier key 3. */
  public static final int KEY_MODIFIER3 = 1 << 30;

  /** A special "key" mapping for modifier key 4. */
  public static final int KEY_MODIFIER4 = 1 << 31;

  /** A mask for the modifier keys. */
  public static final int KEY_MODIFIER_MASK =
    KEY_MODIFIER1 | KEY_MODIFIER2 | KEY_MODIFIER3 | KEY_MODIFIER4;

  /** An array for modifiers. */
  public static final int[] KEY_MODIFIERS = {
    KEY_MODIFIER1, KEY_MODIFIER2, KEY_MODIFIER3, KEY_MODIFIER4 };

  /**
   * Returns the "key" mapping for the identified mouse button.
   */
  public static int getMouseKey (int button)
  {
    switch (button) {
      case MouseEvent.BUTTON1: return KEY_BUTTON1;
      case MouseEvent.BUTTON2: return KEY_BUTTON2;
      case MouseEvent.BUTTON3: return KEY_BUTTON3;
      case MouseEvent.BUTTON4: return KEY_BUTTON4;
      case MouseEvent.BUTTON5: return KEY_BUTTON5;
      case MouseEvent.BUTTON6: return KEY_BUTTON6;
      case MouseEvent.BUTTON7: return KEY_BUTTON7;
      case MouseEvent.BUTTON8: return KEY_BUTTON8;
      case MouseEvent.BUTTON9: return KEY_BUTTON9;
      case MouseEvent.BUTTON10: return KEY_BUTTON10;
      case MouseEvent.BUTTON11: return KEY_BUTTON11;
      case MouseEvent.BUTTON12: return KEY_BUTTON12;
      case MouseEvent.BUTTON13: return KEY_BUTTON13;
      case MouseEvent.BUTTON14: return KEY_BUTTON14;
      case MouseEvent.BUTTON15: return KEY_BUTTON15;
      case MouseEvent.BUTTON16: return KEY_BUTTON15;
      default: return KEY_NONE;
    }
  }

  /**
   * Returns the "key" mapping for the identified controller parameters.
   */
  public static int getControllerKey (int type, int controllerIndex, int controlIndex)
  {
    return 0xFFFFFFF & ((controllerIndex << 24) | (controlIndex << 16) | type);
  }

  /**
   * Checks whether the supplied key is valid. Controller keys are invalid if they refer to
   * controllers or controls that don't exist.
   */
  public boolean isValid (int key)
  {
    return true;
//    switch (getType(key)) {
//      case KEY_CONTROLLER_BUTTON:
//      case KEY_CONTROLLER_AXIS_POSITIVE:
//      case KEY_CONTROLLER_AXIS_NEGATIVE:
//        return getControllerIndex(key) < 0 && getControlIndex(key) < 0;
//      case KEY_CONTROLLER_POV_X_POSITIVE:
//      case KEY_CONTROLLER_POV_X_NEGATIVE:
//      case KEY_CONTROLLER_POV_Y_POSITIVE:
//      case KEY_CONTROLLER_POV_Y_NEGATIVE:
//        return getControllerIndex(key) < 0;
//      default:
//        return true;
//    }
  }

  /**
   * Returns a string describing the specified key.
   */
  public String getDesc (MessageBundle msgs, int key)
  {
    int baseKey = getBaseKey(key);

    List<String> modifiers = Lists.newArrayList();

    if (key != KEY_NONE) {
      OUTER:
      for (int ii = 0; ii < KEY_MODIFIERS.length; ii++) {
        if (hasModifierKey(key, KEY_MODIFIERS[ii])) {
          List<Integer> keys = _modifierToKeys.get(KEY_MODIFIERS[ii]);
          if (keys.isEmpty()) {
            // Modified key with no modifier association. Return default.
            modifiers.add("k.modifier_" + (ii + 1));
            continue;
          }
          boolean isController = isControllerKey(baseKey);
          boolean isMouse = isMouseKey(baseKey);
          boolean isKeyboard = isKeyboardKey(baseKey);
          for (int k : keys) {
            if ((isController && isControllerKey(k)) ||
                (isMouse && isMouseKey(k)) ||
                (isKeyboard && isKeyboardKey(k))) {
              // If we have a modifier that matches the button type, use it.
              modifiers.add(MessageBundle.taint(getDesc(msgs, k)));
              continue OUTER;
            }
          }
          // Otherwise just use the first one.
          modifiers.add(MessageBundle.taint(getDesc(msgs, keys.iterator().next())));
        }
      }
    }

    String result = getBaseDescKey(msgs, baseKey);
    if (!modifiers.isEmpty()) {
      modifiers.add(result);
      result = MessageBundle.compose(
        "m.control-list." + modifiers.size(), modifiers.toArray());
    }
    return msgs.xlate(result);
  }

  protected String getBaseDescKey (MessageBundle msgs, int baseKey)
  {
    int idx;
    switch (getType(baseKey)) {
      case KEY_CONTROLLER_BUTTON:
        idx = getControllerIndex(baseKey);
        return MessageBundle.tcompose("m.controller_button", String.valueOf(idx),
            String.valueOf(getControlIndex(baseKey))
                .replaceFirst("[Bb]utton\\s*", ""));

      case KEY_CONTROLLER_AXIS_POSITIVE:
        idx = getControllerIndex(baseKey);
        return MessageBundle.tcompose("m.controller_axis_positive", String.valueOf(idx),
          String.valueOf(getControlIndex(baseKey)));

      case KEY_CONTROLLER_AXIS_NEGATIVE:
        idx = getControllerIndex(baseKey);
        return MessageBundle.tcompose("m.controller_axis_negative", String.valueOf(idx),
          String.valueOf(getControlIndex(baseKey)));

      case KEY_CONTROLLER_POV_X_POSITIVE:
        return MessageBundle.tcompose("m.controller_pov_x_positive",
          String.valueOf(getControllerIndex(baseKey)));

      case KEY_CONTROLLER_POV_X_NEGATIVE:
        return MessageBundle.tcompose("m.controller_pov_x_negative",
          String.valueOf(getControllerIndex(baseKey)));

      case KEY_CONTROLLER_POV_Y_POSITIVE:
        return MessageBundle.tcompose("m.controller_pov_y_positive",
          String.valueOf(getControllerIndex(baseKey)));

      case KEY_CONTROLLER_POV_Y_NEGATIVE:
        return MessageBundle.tcompose("m.controller_pov_y_negative",
          String.valueOf(getControllerIndex(baseKey)));

      default:
        String name = getName(baseKey);
        String mkey = "k." + StringUtil.toUSLowerCase(name);
        return msgs.exists(mkey) ? mkey : MessageBundle.taint(name);
    }
  }

  /**
   * Returns the name for the specified key.
   */
  public String getName (int key)
  {
    switch (getType(key)) {
      case KEY_BUTTON1: return "BUTTON1";
      case KEY_BUTTON2: return "BUTTON2";
      case KEY_BUTTON3: return "BUTTON3";
      case KEY_BUTTON4: return "BUTTON4";
      case KEY_BUTTON5: return "BUTTON5";
      case KEY_WHEEL_UP: return "WHEEL_UP";
      case KEY_WHEEL_DOWN: return "WHEEL_DOWN";
      case KEY_CONTROLLER_BUTTON:
        return "CONTROLLER" + getControllerIndex(key) + "_BUTTON" + getControlIndex(key);
      case KEY_CONTROLLER_AXIS_POSITIVE:
        return "CONTROLLER" + getControllerIndex(key) + "_AXIS" +
          getControlIndex(key) + "_POSITIVE";
      case KEY_CONTROLLER_AXIS_NEGATIVE:
        return "CONTROLLER" + getControllerIndex(key) + "_AXIS" +
          getControlIndex(key) + "_NEGATIVE";
      case KEY_CONTROLLER_POV_X_POSITIVE:
        return "CONTROLLER" + getControllerIndex(key) + "_POV_X_POSITIVE";
      case KEY_CONTROLLER_POV_X_NEGATIVE:
        return "CONTROLLER" + getControllerIndex(key) + "_POV_X_NEGATIVE";
      case KEY_CONTROLLER_POV_Y_POSITIVE:
        return "CONTROLLER" + getControllerIndex(key) + "_POV_Y_POSITIVE";
      case KEY_CONTROLLER_POV_Y_NEGATIVE:
        return "CONTROLLER" + getControllerIndex(key) + "_POV_Y_NEGATIVE";
      default: {
        // Return old LWJGL-2-era short lowercase names where the i18n bundle
        // has existing translations keyed on them (k.lshift, k.return, etc.).
        // Anything not listed falls through to glfwGetKeyName for printable
        // characters (a, 1, ...) or "Unknown(code)" as a last resort.
        switch (key) {
          case KEY_NONE: return "none";

          // Modifiers / locks
          case GLFW.GLFW_KEY_LEFT_SHIFT: return "lshift";
          case GLFW.GLFW_KEY_RIGHT_SHIFT: return "rshift";
          case GLFW.GLFW_KEY_LEFT_CONTROL: return "lcontrol";
          case GLFW.GLFW_KEY_RIGHT_CONTROL: return "rcontrol";
          case GLFW.GLFW_KEY_LEFT_ALT: return "lmenu";
          case GLFW.GLFW_KEY_RIGHT_ALT: return "rmenu";
          case GLFW.GLFW_KEY_LEFT_SUPER: return "lmeta";
          case GLFW.GLFW_KEY_RIGHT_SUPER: return "rmeta";
          case GLFW.GLFW_KEY_CAPS_LOCK: return "capital";
          case GLFW.GLFW_KEY_NUM_LOCK: return "numlock";
          case GLFW.GLFW_KEY_SCROLL_LOCK: return "scroll";

          // Navigation / editing
          case GLFW.GLFW_KEY_LEFT: return "left";
          case GLFW.GLFW_KEY_RIGHT: return "right";
          case GLFW.GLFW_KEY_UP: return "up";
          case GLFW.GLFW_KEY_DOWN: return "down";
          case GLFW.GLFW_KEY_HOME: return "home";
          case GLFW.GLFW_KEY_END: return "end";
          case GLFW.GLFW_KEY_PAGE_UP: return "prior";
          case GLFW.GLFW_KEY_PAGE_DOWN: return "next";
          case GLFW.GLFW_KEY_INSERT: return "insert";
          case GLFW.GLFW_KEY_DELETE: return "delete";

          // Control
          case GLFW.GLFW_KEY_ENTER: return "return";
          case GLFW.GLFW_KEY_ESCAPE: return "escape";
          case GLFW.GLFW_KEY_TAB: return "tab";
          case GLFW.GLFW_KEY_SPACE: return "space";
          case GLFW.GLFW_KEY_BACKSPACE: return "back";
          case GLFW.GLFW_KEY_PAUSE: return "pause";
          case GLFW.GLFW_KEY_PRINT_SCREEN: return "sysrq";
          case GLFW.GLFW_KEY_MENU: return "menu";

          // Numpad
          case GLFW.GLFW_KEY_KP_0: return "numpad0";
          case GLFW.GLFW_KEY_KP_1: return "numpad1";
          case GLFW.GLFW_KEY_KP_2: return "numpad2";
          case GLFW.GLFW_KEY_KP_3: return "numpad3";
          case GLFW.GLFW_KEY_KP_4: return "numpad4";
          case GLFW.GLFW_KEY_KP_5: return "numpad5";
          case GLFW.GLFW_KEY_KP_6: return "numpad6";
          case GLFW.GLFW_KEY_KP_7: return "numpad7";
          case GLFW.GLFW_KEY_KP_8: return "numpad8";
          case GLFW.GLFW_KEY_KP_9: return "numpad9";
          case GLFW.GLFW_KEY_KP_DECIMAL: return "decimal";
          case GLFW.GLFW_KEY_KP_DIVIDE: return "divide";
          case GLFW.GLFW_KEY_KP_MULTIPLY: return "multiply";
          case GLFW.GLFW_KEY_KP_SUBTRACT: return "subtract";
          case GLFW.GLFW_KEY_KP_ADD: return "add";
          case GLFW.GLFW_KEY_KP_ENTER: return "numpadenter";
          case GLFW.GLFW_KEY_KP_EQUAL: return "numpadequals";

          // Punctuation whose bundle entries differ from the glfwGetKeyName result
          case GLFW.GLFW_KEY_GRAVE_ACCENT: return "grave";
          case GLFW.GLFW_KEY_APOSTROPHE: return "apostrophe";
          case GLFW.GLFW_KEY_BACKSLASH: return "backslash";
          case GLFW.GLFW_KEY_SEMICOLON: return "semicolon";
          case GLFW.GLFW_KEY_SLASH: return "slash";
          case GLFW.GLFW_KEY_COMMA: return "comma";
          case GLFW.GLFW_KEY_PERIOD: return "period";
          case GLFW.GLFW_KEY_MINUS: return "minus";
          case GLFW.GLFW_KEY_EQUAL: return "equals";
          case GLFW.GLFW_KEY_LEFT_BRACKET: return "lbracket";
          case GLFW.GLFW_KEY_RIGHT_BRACKET: return "rbracket";

          // Function keys (no bundle entries; rendered tainted as "F1".."F25")
          case GLFW.GLFW_KEY_F1: return "F1";
          case GLFW.GLFW_KEY_F2: return "F2";
          case GLFW.GLFW_KEY_F3: return "F3";
          case GLFW.GLFW_KEY_F4: return "F4";
          case GLFW.GLFW_KEY_F5: return "F5";
          case GLFW.GLFW_KEY_F6: return "F6";
          case GLFW.GLFW_KEY_F7: return "F7";
          case GLFW.GLFW_KEY_F8: return "F8";
          case GLFW.GLFW_KEY_F9: return "F9";
          case GLFW.GLFW_KEY_F10: return "F10";
          case GLFW.GLFW_KEY_F11: return "F11";
          case GLFW.GLFW_KEY_F12: return "F12";
          case GLFW.GLFW_KEY_F13: return "F13";
          case GLFW.GLFW_KEY_F14: return "F14";
          case GLFW.GLFW_KEY_F15: return "F15";
          case GLFW.GLFW_KEY_F16: return "F16";
          case GLFW.GLFW_KEY_F17: return "F17";
          case GLFW.GLFW_KEY_F18: return "F18";
          case GLFW.GLFW_KEY_F19: return "F19";
          case GLFW.GLFW_KEY_F20: return "F20";
          case GLFW.GLFW_KEY_F21: return "F21";
          case GLFW.GLFW_KEY_F22: return "F22";
          case GLFW.GLFW_KEY_F23: return "F23";
          case GLFW.GLFW_KEY_F24: return "F24";
          case GLFW.GLFW_KEY_F25: return "F25";
        }
        String name = GLFW.glfwGetKeyName(key, 0);
        return (name != null) ? StringUtil.toUSUpperCase(name) : "Unknown(" + key + ")";
      }
    }
  }

  /**
   * Returns the key type.
   */
  public int getType (int key)
  {
    return key & 0xFFFF;
  }

  /**
   * Returns the controller index.
   */
  public int getControllerIndex (int key)
  {
    return getBaseKey(key) >>> 24;
  }

  /**
   * Returns the control index.
   */
  public int getControlIndex (int key)
  {
    return (key >> 16) & 0xFF;
  }

  /**
   * Is the specified key an analog control that can be "pressed" by values between 0f and 1f?
   */
  public boolean isAnalogKey (int key)
  {
    switch (getType(key)) {
    case KEY_CONTROLLER_AXIS_POSITIVE:
    case KEY_CONTROLLER_AXIS_NEGATIVE:
    case KEY_CONTROLLER_POV_X_POSITIVE:
    case KEY_CONTROLLER_POV_X_NEGATIVE:
    case KEY_CONTROLLER_POV_Y_POSITIVE:
    case KEY_CONTROLLER_POV_Y_NEGATIVE:
      return true;

    default:
      return false;
    }
  }

  /**
   * Is the specified key a mouse button or mouse wheel key?
   */
  public boolean isMouseKey (int key)
  {
    switch (getType(key)) {
    case KEY_BUTTON1:
    case KEY_BUTTON2:
    case KEY_BUTTON3:
    case KEY_BUTTON4:
    case KEY_BUTTON5:
    case KEY_WHEEL_UP:
    case KEY_WHEEL_DOWN:
      return true;

    default:
      return false;
    }
  }

  /**
   * Is the specified key a keyboard key?
   */
  public boolean isKeyboardKey (int key)
  {
    key = getBaseKey(key);
    return (key != KEY_NONE && key < KEY_BASE);
  }

  /**
   * Is the specified key a controller key?
   */
  public boolean isControllerKey (int key)
  {
    return (isAnalogKey(key) || (getType(key) == KEY_CONTROLLER_BUTTON));
  }

  /**
   * Returns the base key for a given int (strips the modifier-mask bits). {@link #KEY_NONE}
   * stays {@link #KEY_NONE} — its all-bits-set representation would otherwise mangle to
   * {@code ~KEY_MODIFIER_MASK}.
   */
  public int getBaseKey (int key)
  {
    return (key == KEY_NONE) ? KEY_NONE : (key & ~KEY_MODIFIER_MASK);
  }

  /**
   * Returns whether the given modifier is held in the given key.
   */
  public boolean hasModifierKey (int key, int modifier)
  {
    return (key & modifier) != 0;
  }

  /**
   * Returns whether the given key has ANY modifiers on it.
   */
  public boolean hasAnyModifierKeys (int key)
  {
    return (key & KEY_MODIFIER_MASK) != 0;
  }

  /**
   * Add a mapping for the specified key as one of the modifiers.
   */
  public void addModifierKey (int key, int modKey)
  {
    _modifierToKeys.put(modKey, key);
  }

  /**
   * Clear all modifier keys.
   */
  public void clearModifierKeys ()
  {
    _modifierToKeys.clear();
  }

  /** Maps the modifier key constant to the key value. */
  protected ListMultimap<Integer, Integer> _modifierToKeys = ArrayListMultimap.create();

  /** A singleton instance, because this class was originally all fucking static and so
   * it's a giant pain in the ass to make an instance accessible in a reasonable way. */
  protected static PseudoKeys _singleton = new PseudoKeys();
}
