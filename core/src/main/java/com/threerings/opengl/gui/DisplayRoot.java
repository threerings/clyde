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

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import java.util.Iterator;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.IME;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.samskivert.util.RunAnywhere;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.ControllerEvent;
import com.threerings.opengl.gui.event.IMEEvent;
import com.threerings.opengl.gui.event.InputEvent;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.text.IMEComponent;
import static com.threerings.opengl.gui.Log.log;


/**
 * A root for {@link Display}-based apps.
 */
public class DisplayRoot extends Root
{
    public DisplayRoot (GlContext ctx)
    {
        super(ctx);
        _clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    /**
     * Polls the input system for events and dispatches them.
     */
    public void poll ()
    {
        boolean isActive = Display.isActive();
        boolean newActive = !_wasActive && isActive;
        _wasActive = isActive;

        // process ime events
        while (IME.next()) {
            dispatchEvent(getFocus(),
                    new IMEEvent(this, _tickStamp,
                        IME.getState(), IME.getString(), IME.getCursorPosition()));
        }

        // Work around a Mac issue: when focus is regained, the mouse coordinate is not
        // updated until an actual mouse moved event is generated...
        if (newActive && RunAnywhere.isMacOS()) {
            Point p = MouseInfo.getPointerInfo().getLocation();
            // We can modify and use that point directly. Translate it into window coords...
            p.x -= Display.getX();
            p.y = MAC_OS_MENUBAR_HEIGHT + Display.getHeight() - (p.y - Display.getY());
            mouseMoved(_tickStamp, p.x, p.y, false); // hover the right coordinate immediately
            _forcedMouse = p; // save this point until we get a geniune mouseMoved
        }

        // process mouse events
        while (Mouse.next()) {
            int button = Mouse.getEventButton();
            int delta = Mouse.getEventDWheel();
            int eventX = Mouse.getEventX();
            int eventY = Mouse.getEventY();
            // clicks and wheels
            if ((button != -1) || (delta != 0)) {
                if (_forcedMouse != null) {
                    eventX = _forcedMouse.x;
                    eventY = _forcedMouse.y;
                }
                if (button != -1) {
                    boolean pressed = Mouse.getEventButtonState();
                    if (pressed && (NAIVE_FOCUS || !newActive)) {
                        mousePressed(_tickStamp, button, eventX, eventY, false);
                    } else {
                        mouseReleased(_tickStamp, button, eventX, eventY, false);
                    }
                    updateButtonModifier(button, pressed);
                }
                if (delta != 0) {
                    mouseWheeled(_tickStamp, eventX, eventY, (delta > 0) ? +1 : -1, false);
                }

            // movement
            } else if (NAIVE_FOCUS || isActive) {
                mouseMoved(_tickStamp, eventX, eventY, false);
                _forcedMouse = null; // clear the forcedMouse value, if any
            }
        }

        // process keyboard events
        while (Keyboard.next()) {
            int key = Keyboard.getEventKey();
            boolean pressed = Keyboard.getEventKeyState();
            if (pressed) {
                keyPressed(_tickStamp, Keyboard.getEventCharacter(), key, false);
            } else {
                keyReleased(_tickStamp, Keyboard.getEventCharacter(), key, false);
            }
            updateKeyModifier(key, pressed);
        }

        if (isActive) {
            // make sure the pressed keys are really pressed
            if (!_pressedKeys.isEmpty()) {
                for (Iterator<KeyRecord> it = _pressedKeys.values().iterator(); it.hasNext(); ) {
                    KeyRecord record = it.next();
                    KeyEvent press = record.getPress();
                    int key = press.getKeyCode();
                    if (!Keyboard.isKeyDown(key)) {
                        dispatchEvent(getFocus(), new KeyEvent(
                            this, _tickStamp, _modifiers, KeyEvent.KEY_RELEASED,
                            press.getKeyChar(), key, false));
                        updateKeyModifier(key, false);
                        it.remove();
                    }
                }
            }
        } else {
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

        // process controller events
        while (Controllers.next()) {
            if (!isActive) {
                continue;
            }
            Controller controller = Controllers.getEventSource();
            if (Controllers.isEventButton()) {
                int index = Controllers.getEventControlIndex();
                if (controller.isButtonPressed(index)) {
                    controllerPressed(controller, _tickStamp, index);
                } else {
                    controllerReleased(controller, _tickStamp, index);
                }
            } else if (Controllers.isEventAxis()) {
                int index = Controllers.getEventControlIndex();
                controllerMoved(
                    controller, _tickStamp, index, Controllers.isEventXAxis(),
                    Controllers.isEventYAxis(), controller.getAxisValue(index));

            } else if (Controllers.isEventPovX()) {
                controllerPovXMoved(controller, _tickStamp, controller.getPovX());

            } else if (Controllers.isEventPovY()) {
                controllerPovYMoved(controller, _tickStamp, controller.getPovY());
            }
        }
    }

    /**
     * Sets if we performe native IME composition.
     */
    public void setIMEComposingEnabled (boolean enabled)
    {
        if (enabled == _imeComposingEnabled) {
            return;
        }
        if (_focus instanceof IMEComponent) {
            IME.setComposing(enabled);
        }
        _imeComposingEnabled = enabled;
    }

    @Override
    public int getDisplayWidth ()
    {
        return Display.getDisplayMode().getWidth();
    }

    @Override
    public int getDisplayHeight ()
    {
        return Display.getDisplayMode().getHeight();
    }

    @Override
    public void setMousePosition (int x, int y)
    {
        Mouse.setCursorPosition(x, y);
        super.setMousePosition(x, y);
    }

    @Override
    protected void updateCursor (Cursor cursor)
    {
        if (cursor == null) {
            cursor = getDefaultCursor();
        }
        try {
            Mouse.setNativeCursor(cursor == null ? null : cursor.getLWJGLCursor());
        } catch (LWJGLException e) {
            log.warning("Failed to set cursor.", "cursor", cursor, e);
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
     * Notes that a controller button has been pressed.
     */
    protected void controllerPressed (Controller controller, long when, int index)
    {
        ControllerEvent event = new ControllerEvent(
            controller, when, _modifiers, ControllerEvent.CONTROLLER_PRESSED, index);
        dispatchEvent(getFocus(), event);
    }

    /**
     * Notes that a controller button has been released.
     */
    protected void controllerReleased (Controller controller, long when, int index)
    {
        ControllerEvent event = new ControllerEvent(
            controller, when, _modifiers, ControllerEvent.CONTROLLER_RELEASED, index);
        dispatchEvent(getFocus(), event);
    }

    /**
     * Notes that a controller has moved on an axis.
     */
    protected void controllerMoved (
        Controller controller, long when, int index, boolean xAxis, boolean yAxis, float value)
    {
        ControllerEvent event = new ControllerEvent(
            controller, when, _modifiers, ControllerEvent.CONTROLLER_MOVED,
            index, xAxis, yAxis, value);
        dispatchEvent(getFocus(), event);
    }

    /**
     * Notes that a controller has moved on the pov x axis.
     */
    protected void controllerPovXMoved (Controller controller, long when, float value)
    {
        ControllerEvent event = new ControllerEvent(
            controller, when, _modifiers, ControllerEvent.CONTROLLER_POV_X_MOVED, value);
        dispatchEvent(getFocus(), event);
    }

    /**
     * Notes that a controller has moved on the pov y axis.
     */
    protected void controllerPovYMoved (Controller controller, long when, float value)
    {
        ControllerEvent event = new ControllerEvent(
            controller, when, _modifiers, ControllerEvent.CONTROLLER_POV_Y_MOVED, value);
        dispatchEvent(getFocus(), event);
    }

    @Override
    protected void setIMEFocus (boolean focused)
    {
        if (!focused) {
            IME.setComposing(false);
        }
        super.setIMEFocus(focused);
        if (focused) {
            IME.setComposing(_imeComposingEnabled);
        }
    }

    /** A forced mouse location, or null. Used on Mac OS X only, after regaining focus. */
    protected Point _forcedMouse;

    /** Track whether we were active during the last event poll, so that we can consume
     * the mouse click that may arrive with focus. */
    protected boolean _wasActive;

    /** If ime composing is enabled. */
    protected boolean _imeComposingEnabled;

    /** The number of pixels used by the menubar on Mac OS X. For fudging mouse position. */
    protected static final int MAC_OS_MENUBAR_HEIGHT = 22;

    /** If true, allows hovers while the window is unfocused and processes the focusing click. */
    private static final boolean NAIVE_FOCUS = false;
}
