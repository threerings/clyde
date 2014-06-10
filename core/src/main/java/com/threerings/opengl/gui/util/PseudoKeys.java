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

import com.samskivert.util.HashIntSet;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Keyboard;

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
 * pseudo-keys are in the same identifier space as the key codes defined in {@link Keyboard}.
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
                    event.getController().getIndex(), event.getControlIndex()), 1f);
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
                    event.getController().getIndex(), event.getControlIndex()));
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
            Controller controller = event.getController();
            int controllerIndex = controller.getIndex();
            int axisIndex = event.getControlIndex();
            float value = controller.getAxisValue(axisIndex);
            float dead = controller.getDeadZone(axisIndex);
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
            Controller controller = event.getController();
            int controllerIndex = controller.getIndex();
            float value = controller.getPovX();
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
            Controller controller = event.getController();
            int controllerIndex = controller.getIndex();
            float value = controller.getPovY();
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
         * Returns any associated modifier key with this key.
         */
        protected int getModifierKey (int key)
        {
            Integer retVal = _modifierMap.get(key);
            return (retVal == null) ? Keyboard.KEY_NONE : retVal;
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

    /** A special "key" mapping for the left mouse button. */
    public static final int KEY_BUTTON1 = Keyboard.KEYBOARD_SIZE;

    /** A special "key" mapping for the right mouse button. */
    public static final int KEY_BUTTON2 = Keyboard.KEYBOARD_SIZE + 1;

    /** A special "key" mapping for the middle mouse button. */
    public static final int KEY_BUTTON3 = Keyboard.KEYBOARD_SIZE + 2;

    /** A special "key" mapping for scrolling the mouse wheel up. */
    public static final int KEY_WHEEL_UP = Keyboard.KEYBOARD_SIZE + 3;

    /** A special "key" mapping for scrolling the mouse wheel down. */
    public static final int KEY_WHEEL_DOWN = Keyboard.KEYBOARD_SIZE + 4;

    /** A special "key" mapping for a controller button. */
    public static final int KEY_CONTROLLER_BUTTON = Keyboard.KEYBOARD_SIZE + 5;

    /** A special "key" mapping for positive movement on a controller axis. */
    public static final int KEY_CONTROLLER_AXIS_POSITIVE = Keyboard.KEYBOARD_SIZE + 6;

    /** A special "key" mapping for negative movement on a controller axis. */
    public static final int KEY_CONTROLLER_AXIS_NEGATIVE = Keyboard.KEYBOARD_SIZE + 7;

    /** A special "key" mapping for positive movement on a controller pov x axis. */
    public static final int KEY_CONTROLLER_POV_X_POSITIVE = Keyboard.KEYBOARD_SIZE + 8;

    /** A special "key" mapping for negative movement on a controller pov x axis. */
    public static final int KEY_CONTROLLER_POV_X_NEGATIVE = Keyboard.KEYBOARD_SIZE + 9;

    /** A special "key" mapping for positive movement on a controller pov y axis. */
    public static final int KEY_CONTROLLER_POV_Y_POSITIVE = Keyboard.KEYBOARD_SIZE + 10;

    /** A special "key" mapping for negative movement on a controller pov y axis. */
    public static final int KEY_CONTROLLER_POV_Y_NEGATIVE = Keyboard.KEYBOARD_SIZE + 11;

    /** A special "key" mapping for the 4th mouse button. */
    public static final int KEY_BUTTON4 = Keyboard.KEYBOARD_SIZE + 12;

    /** A special "key" mapping for the 5th mouse button. */
    public static final int KEY_BUTTON5 = Keyboard.KEYBOARD_SIZE + 13;
    public static final int KEY_BUTTON6 = Keyboard.KEYBOARD_SIZE + 14;
    public static final int KEY_BUTTON7 = Keyboard.KEYBOARD_SIZE + 15;
    public static final int KEY_BUTTON8 = Keyboard.KEYBOARD_SIZE + 16;
    public static final int KEY_BUTTON9 = Keyboard.KEYBOARD_SIZE + 17;
    public static final int KEY_BUTTON10 = Keyboard.KEYBOARD_SIZE + 18;
    public static final int KEY_BUTTON11 = Keyboard.KEYBOARD_SIZE + 19;
    public static final int KEY_BUTTON12 = Keyboard.KEYBOARD_SIZE + 20;
    public static final int KEY_BUTTON13 = Keyboard.KEYBOARD_SIZE + 21;
    public static final int KEY_BUTTON14 = Keyboard.KEYBOARD_SIZE + 22;
    public static final int KEY_BUTTON15 = Keyboard.KEYBOARD_SIZE + 23;
    public static final int KEY_BUTTON16 = Keyboard.KEYBOARD_SIZE + 24;

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
            default: return Keyboard.KEY_NONE;
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
     * Checks whether the supplied key is valid.  Controller keys are invalid if they refer to
     * controllers or controls that don't exist.
     */
    public boolean isValid (int key)
    {
        int controllerIndex;

        if (!Controllers.isCreated()) {
            throw new RuntimeException("Controllers has not been created.");
        }

        switch (getType(key)) {
            case KEY_CONTROLLER_BUTTON:
                controllerIndex = getControllerIndex(key);
                return controllerIndex < Controllers.getControllerCount() &&
                    getControlIndex(key) <
                        Controllers.getController(controllerIndex).getButtonCount();
            case KEY_CONTROLLER_AXIS_POSITIVE:
            case KEY_CONTROLLER_AXIS_NEGATIVE:
                controllerIndex = getControllerIndex(key);
                return controllerIndex < Controllers.getControllerCount() &&
                    getControlIndex(key) <
                        Controllers.getController(controllerIndex).getAxisCount();
            case KEY_CONTROLLER_POV_X_POSITIVE:
            case KEY_CONTROLLER_POV_X_NEGATIVE:
            case KEY_CONTROLLER_POV_Y_POSITIVE:
            case KEY_CONTROLLER_POV_Y_NEGATIVE:
                return getControllerIndex(key) < Controllers.getControllerCount();
            default:
                return true;
        }
    }

    /**
     * Returns a string describing the specified key.
     */
    public String getDesc (MessageBundle msgs, int key)
    {
        int baseKey = getBaseKey(key);

        List<String> modifiers = Lists.newArrayList();

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
                        Controllers.getController(idx).getButtonName(getControlIndex(baseKey))
                                .replaceFirst("[Bb]utton\\s*", ""));

            case KEY_CONTROLLER_AXIS_POSITIVE:
                idx = getControllerIndex(baseKey);
                return MessageBundle.tcompose("m.controller_axis_positive", String.valueOf(idx),
                    Controllers.getController(idx).getAxisName(getControlIndex(baseKey)));

            case KEY_CONTROLLER_AXIS_NEGATIVE:
                idx = getControllerIndex(baseKey);
                return MessageBundle.tcompose("m.controller_axis_negative", String.valueOf(idx),
                    Controllers.getController(idx).getAxisName(getControlIndex(baseKey)));

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
            default: return Keyboard.getKeyName(key);
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
        return (key < Keyboard.KEYBOARD_SIZE && key != Keyboard.KEY_NONE);
    }

    /**
     * Is the specified key a controller key?
     */
    public boolean isControllerKey (int key)
    {
        return (isAnalogKey(key) || (getType(key) == KEY_CONTROLLER_BUTTON));
    }

    /**
     * Returns the base key for a given int.
     */
    public int getBaseKey (int key)
    {
        return key & ~KEY_MODIFIER_MASK;
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
