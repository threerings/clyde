//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import java.util.Map;

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

/**
 * Provides a unified system for handling keys, mouse buttons, and other key-like features.  The
 * pseudo-keys are in the same identifier space as the key codes defined in {@link Keyboard}.
 */
public class PseudoKeys
{
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
        public Unifier ()
        {
        }

        /**
         * Called when a "key" (either a real key or a pseudo-key) is pressed.  Default
         * implementation forwards the event to the observer, if any.
         */
        public void keyPressed (long when, int key, float amount)
        {
            if (_observer != null) {
                _observer.keyPressed(when, key, amount);
            }
        }

        /**
         * Called when a "key" is released.  Default implementation forwards the event to the
         * observer, if any.
         */
        public void keyReleased (long when, int key)
        {
            if (_observer != null) {
                _observer.keyReleased(when, key);
            }
        }

        // documentation inherited from interface KeyListener
        public void keyPressed (KeyEvent event)
        {
            if (!event.isConsumed()) {
                keyPressed(event.getWhen(), event.getKeyCode(), 1f);
            }
        }

        // documentation inherited from interface KeyListener
        public void keyReleased (KeyEvent event)
        {
            if (!event.isConsumed()) {
                keyReleased(event.getWhen(), event.getKeyCode());
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
        }

        // documentation inherited from interface ControllerListener
        public void controllerPressed (ControllerEvent event)
        {
            if (!event.isConsumed()) {
                keyPressed(event.getWhen(), getControllerKey(KEY_CONTROLLER_BUTTON,
                    event.getController().getIndex(), event.getControlIndex()), 1f);
            }
        }

        // documentation inherited from interface ControllerListener
        public void controllerReleased (ControllerEvent event)
        {
            if (!event.isConsumed()) {
                keyReleased(event.getWhen(), getControllerKey(KEY_CONTROLLER_BUTTON,
                    event.getController().getIndex(), event.getControlIndex()));
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
        }

        @Override // documentation inherited
        public void mousePressed (MouseEvent event)
        {
            if (!event.isConsumed()) {
                keyPressed(event.getWhen(), getMouseKey(event.getButton()), 1f);
            }
        }

        @Override // documentation inherited
        public void mouseReleased (MouseEvent event)
        {
            if (!event.isConsumed()) {
                keyReleased(event.getWhen(), getMouseKey(event.getButton()));
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

    /**
     * Returns the "key" mapping for the identified mouse button.
     */
    public static int getMouseKey (int button)
    {
        switch (button) {
            case MouseEvent.BUTTON1: return KEY_BUTTON1;
            case MouseEvent.BUTTON2: return KEY_BUTTON2;
            case MouseEvent.BUTTON3: return KEY_BUTTON3;
            default: return Keyboard.KEY_NONE;
        }
    }

    /**
     * Returns the "key" mapping for the identified controller parameters.
     */
    public static int getControllerKey (int type, int controllerIndex, int controlIndex)
    {
        return (controllerIndex << 24) | (controlIndex << 16) | type;
    }

    /**
     * Checks whether the supplied key is valid.  Controller keys are invalid if they refer to
     * controllers or controls that don't exist.
     */
    public static boolean isValid (int key)
    {
        int controllerIndex;
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
    public static String getDesc (MessageBundle msgs, int key)
    {
        Controller controller;
        switch (getType(key)) {
            case KEY_CONTROLLER_BUTTON:
                controller = Controllers.getController(getControllerIndex(key));
                return msgs.get("m.controller_button", controller.getName(),
                    controller.getButtonName(getControlIndex(key)));
            case KEY_CONTROLLER_AXIS_POSITIVE:
                controller = Controllers.getController(getControllerIndex(key));
                return msgs.get("m.controller_axis_positive", controller.getName(),
                    controller.getAxisName(getControlIndex(key)));
            case KEY_CONTROLLER_AXIS_NEGATIVE:
                controller = Controllers.getController(getControllerIndex(key));
                return msgs.get("m.controller_axis_negative", controller.getName(),
                    controller.getAxisName(getControlIndex(key)));
            case KEY_CONTROLLER_POV_X_POSITIVE:
                return msgs.get("m.controller_pov_x_positive",
                    Controllers.getController(getControllerIndex(key)).getName());
            case KEY_CONTROLLER_POV_X_NEGATIVE:
                return msgs.get("m.controller_pov_x_negative",
                    Controllers.getController(getControllerIndex(key)).getName());
            case KEY_CONTROLLER_POV_Y_POSITIVE:
                return msgs.get("m.controller_pov_y_positive",
                    Controllers.getController(getControllerIndex(key)).getName());
            case KEY_CONTROLLER_POV_Y_NEGATIVE:
                return msgs.get("m.controller_pov_y_negative",
                    Controllers.getController(getControllerIndex(key)).getName());
            default:
                String name = getName(key);
                String mkey = "m." + StringUtil.toUSLowerCase(name);
                return msgs.exists(mkey) ? msgs.get(mkey) : name;
        }
    }

    /**
     * Returns the name for the specified key.
     */
    public static String getName (int key)
    {
        switch (getType(key)) {
            case KEY_BUTTON1: return "BUTTON1";
            case KEY_BUTTON2: return "BUTTON2";
            case KEY_BUTTON3: return "BUTTON3";
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
    public static int getType (int key)
    {
        return key & 0xFFFF;
    }

    /**
     * Returns the controller index.
     */
    public static int getControllerIndex (int key)
    {
        return key >>> 24;
    }

    /**
     * Returns the control index.
     */
    public static int getControlIndex (int key)
    {
        return (key >> 16) & 0xFF;
    }
}
