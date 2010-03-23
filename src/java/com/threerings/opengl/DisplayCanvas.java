//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

import java.awt.AWTEvent;
import java.awt.Canvas;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.PixelFormat;

import static com.threerings.opengl.Log.*;

/**
 * A canvas that uses {@link Display}.
 */
public class DisplayCanvas extends Canvas
    implements GlCanvas
{
    /**
     * Creates a new canvas.
     */
    public DisplayCanvas ()
    {
        // make popups heavyweight so that we can see them over the canvas
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        // do not allow the canvas to receive focus
        // setFocusable(false);

        // add a listener to record states.  we do this here rather than in the check methods
        // because on some platforms AWT dispatches some of the mouse events that are also
        // picked up by LWJGL
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
                _lbuttons[getLWJGLButton(event.getButton())] = true;
            }
            @Override public void mouseReleased (MouseEvent event) {
                _lbuttons[getLWJGLButton(event.getButton())] = false;
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
    public Drawable getDrawable ()
    {
        return Display.getDrawable();
    }

    // documentation inherited from interface GlCanvas
    public void setVSyncEnabled (boolean vsync)
    {
        Display.setVSyncEnabled(vsync);
    }

    // documentation inherited from interface GlCanvas
    public void makeCurrent ()
    {
        try {
            Display.makeCurrent();
        } catch (LWJGLException e) {
            log.warning("Failed to make context current.", e);
        }
    }

    // documentation inherited from interface GlCanvas
    public void destroy ()
    {
        Keyboard.destroy();
        Mouse.destroy();
        Display.destroy();
    }

    @Override // documentation inherited
    public Point getMousePosition ()
    {
        return _entered ? getRelativeMouseLocation() : null;
    }

    @Override // documentation inherited
    public void paint (Graphics g)
    {
        // initialize on first paint
        if (_initialized) {
            return;
        }
        _initialized = true;

        // attempt to find a valid pixel format
        for (PixelFormat format : GlApp.PIXEL_FORMATS) {
            try {
                init(format);
                return;
            } catch (LWJGLException e) {
                // proceed to next format
            }
        }
        log.warning("Couldn't find valid pixel format.");
    }

    @Override // documentation inherited
    public void update (Graphics g)
    {
        // no-op
    }

    /**
     * Attempts to create the display with this canvas as its parent.
     */
    protected void init (PixelFormat pformat)
        throws LWJGLException
    {
        Display.setParent(this);
        Display.create(pformat);

        // create the keyboard and mouse
        try {
            Keyboard.create();
        } catch (LWJGLException e) {
            log.warning("Failed to create keyboard.", e);
        }
        try {
            Mouse.create();
        } catch (LWJGLException e) {
            log.warning("Failed to create mouse.", e);
        }

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
            generateEvents();
            updateView();
            if (Display.isVisible()) {
                renderView();
            }
            Display.update();

        } catch (Exception e) {
            log.warning("Caught exception in frame loop.", e);
        }
    }

    /**
     * Generates AWT input events from the LWJGL devices.
     */
    protected void generateEvents ()
    {
        long now = System.currentTimeMillis();

        // get the modifiers
        int modifiers = 0;
        int bcount = Mouse.getButtonCount();
        if (bcount >= 1 && Mouse.isButtonDown(0)) {
            modifiers |= InputEvent.BUTTON1_DOWN_MASK;
        }
        if (bcount >= 2 && Mouse.isButtonDown(1)) {
            modifiers |= InputEvent.BUTTON3_DOWN_MASK;
        }
        if (bcount >= 3 && Mouse.isButtonDown(2)) {
            modifiers |= InputEvent.BUTTON2_DOWN_MASK;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            modifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
                Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            modifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
            modifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA)) {
            modifiers |= InputEvent.META_DOWN_MASK;
        }

        // dispatch keyboard events
        while (Keyboard.next()) {
            int key = Keyboard.getEventKey();
            dispatchEvent(new KeyEvent(
                this, Keyboard.getEventKeyState() ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED,
                now, modifiers, getAWTCode(key), Keyboard.getEventCharacter(),
                getAWTLocation(key)));
        }

        // process mouse events
        while (Mouse.next()) {
            int x = Mouse.getEventX(), y = getHeight() - Mouse.getEventY() - 1;
            checkEntered(now, modifiers, x, y);
            checkMoved(now, modifiers, x, y);
            int button = Mouse.getEventButton();
            if (button != -1) {
                checkButtonState(now, modifiers, x, y, button, Mouse.getEventButtonState());
            }
            int delta = -Integer.signum(Mouse.getEventDWheel());
            if (delta != 0 && ++_lclicks > 0) {
                dispatchEvent(new MouseWheelEvent(
                    this, MouseEvent.MOUSE_WHEEL, now, modifiers, x, y,
                    0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, delta, delta));
            }
        }

        // handle non-event mouse business (once the pointer is outside the window, we no longer
        // receive events)
        Point pt = getRelativeMouseLocation();
        checkEntered(now, modifiers, pt.x, pt.y);
        checkExited(now, modifiers, pt.x, pt.y);
        checkMoved(now, modifiers, pt.x, pt.y);
        checkButtonState(now, modifiers, pt.x, pt.y, 0, Mouse.isButtonDown(0));
        checkButtonState(now, modifiers, pt.x, pt.y, 1, Mouse.isButtonDown(1));
        checkButtonState(now, modifiers, pt.x, pt.y, 2, Mouse.isButtonDown(2));
    }

    /**
     * Determines whether the mouse has entered the component, dispatching an event if so.
     */
    protected void checkEntered (long now, int modifiers, int x, int y)
    {
        if (!_entered && contains(x, y)) {
            dispatchEvent(new MouseEvent(
                this, MouseEvent.MOUSE_ENTERED, now, modifiers, x, y, 0, false));
        }
    }

    /**
     * Determines whether the mouse has exited the component, dispatching an event if so.
     */
    protected void checkExited (long now, int modifiers, int x, int y)
    {
        if (_entered && !anyButtonsDown(modifiers) && !contains(x, y)) {
            for (int ii = 0; ii < _lbuttons.length; ii++) {
                checkButtonState(now, modifiers, x, y, ii, false);
            }
            dispatchEvent(new MouseEvent(
                this, MouseEvent.MOUSE_EXITED, now, modifiers, x, y, 0, false));
        }
    }

    /**
     * Determines whether the mouse has moved, dispatching an event if so.
     */
    protected void checkMoved (long now, int modifiers, int x, int y)
    {
        if (_entered && (_lx != x || _ly != y)) {
            dispatchEvent(new MouseEvent(
                this,
                anyButtonsDown(modifiers) ? MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED,
                now, modifiers, x, y, 0, false));
        }
    }

    /**
     * Checks for button press/release.
     */
    protected void checkButtonState (long now, int modifiers, int x, int y, int button, boolean pressed)
    {
        if (_entered && _lbuttons[button] != pressed) {
            dispatchEvent(new MouseEvent(
                this,
                pressed ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED,
                now, modifiers, x, y, 0, false, getAWTButton(button)));
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
     * Returns the location of the mouse pointer relative to this component, regardless of whether
     * or not it's actually hovering over the component.
     */
    protected Point getRelativeMouseLocation ()
    {
        Point pt = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(pt, this);
        return pt;
    }

    /**
     * Returns the AWT key code corresponding to the specified key.
     */
    protected static int getAWTCode (int key)
    {
        switch (key) {
            case Keyboard.KEY_0: return KeyEvent.VK_0;
            case Keyboard.KEY_1: return KeyEvent.VK_1;
            case Keyboard.KEY_2: return KeyEvent.VK_2;
            case Keyboard.KEY_3: return KeyEvent.VK_3;
            case Keyboard.KEY_4: return KeyEvent.VK_4;
            case Keyboard.KEY_5: return KeyEvent.VK_5;
            case Keyboard.KEY_6: return KeyEvent.VK_6;
            case Keyboard.KEY_7: return KeyEvent.VK_7;
            case Keyboard.KEY_8: return KeyEvent.VK_8;
            case Keyboard.KEY_9: return KeyEvent.VK_9;
            case Keyboard.KEY_A: return KeyEvent.VK_A;
            case Keyboard.KEY_ADD: return KeyEvent.VK_ADD;
            case Keyboard.KEY_APOSTROPHE: return KeyEvent.VK_QUOTE;
            case Keyboard.KEY_APPS: return KeyEvent.VK_UNDEFINED;
            case Keyboard.KEY_AT: return KeyEvent.VK_AT;
            case Keyboard.KEY_AX: return KeyEvent.VK_UNDEFINED;
            case Keyboard.KEY_B: return KeyEvent.VK_B;
            case Keyboard.KEY_BACK: return KeyEvent.VK_BACK_SPACE;
            case Keyboard.KEY_BACKSLASH: return KeyEvent.VK_BACK_SLASH;
            case Keyboard.KEY_C: return KeyEvent.VK_C;
            case Keyboard.KEY_CAPITAL: return KeyEvent.VK_CAPS_LOCK;
            case Keyboard.KEY_CIRCUMFLEX: return KeyEvent.VK_CIRCUMFLEX;
            case Keyboard.KEY_COLON: return KeyEvent.VK_COLON;
            case Keyboard.KEY_COMMA: return KeyEvent.VK_COMMA;
            case Keyboard.KEY_CONVERT: return KeyEvent.VK_CONVERT;
            case Keyboard.KEY_D: return KeyEvent.VK_D;
            case Keyboard.KEY_DECIMAL: return KeyEvent.VK_DECIMAL;
            case Keyboard.KEY_DELETE: return KeyEvent.VK_DELETE;
            case Keyboard.KEY_DIVIDE: return KeyEvent.VK_DIVIDE;
            case Keyboard.KEY_DOWN: return KeyEvent.VK_DOWN;
            case Keyboard.KEY_E: return KeyEvent.VK_E;
            case Keyboard.KEY_END: return KeyEvent.VK_END;
            case Keyboard.KEY_EQUALS: return KeyEvent.VK_EQUALS;
            case Keyboard.KEY_ESCAPE: return KeyEvent.VK_ESCAPE;
            case Keyboard.KEY_F: return KeyEvent.VK_F;
            case Keyboard.KEY_F1: return KeyEvent.VK_F1;
            case Keyboard.KEY_F10: return KeyEvent.VK_F10;
            case Keyboard.KEY_F11: return KeyEvent.VK_F11;
            case Keyboard.KEY_F12: return KeyEvent.VK_F12;
            case Keyboard.KEY_F13: return KeyEvent.VK_F13;
            case Keyboard.KEY_F14: return KeyEvent.VK_F14;
            case Keyboard.KEY_F15: return KeyEvent.VK_F15;
            case Keyboard.KEY_F2: return KeyEvent.VK_F2;
            case Keyboard.KEY_F3: return KeyEvent.VK_F3;
            case Keyboard.KEY_F4: return KeyEvent.VK_F4;
            case Keyboard.KEY_F5: return KeyEvent.VK_F5;
            case Keyboard.KEY_F6: return KeyEvent.VK_F6;
            case Keyboard.KEY_F7: return KeyEvent.VK_F7;
            case Keyboard.KEY_F8: return KeyEvent.VK_F8;
            case Keyboard.KEY_F9: return KeyEvent.VK_F9;
            case Keyboard.KEY_G: return KeyEvent.VK_G;
            case Keyboard.KEY_GRAVE: return KeyEvent.VK_BACK_QUOTE;
            case Keyboard.KEY_H: return KeyEvent.VK_H;
            case Keyboard.KEY_HOME: return KeyEvent.VK_HOME;
            case Keyboard.KEY_I: return KeyEvent.VK_I;
            case Keyboard.KEY_INSERT: return KeyEvent.VK_INSERT;
            case Keyboard.KEY_J: return KeyEvent.VK_J;
            case Keyboard.KEY_K: return KeyEvent.VK_K;
            case Keyboard.KEY_KANA: return KeyEvent.VK_KATAKANA;
            case Keyboard.KEY_KANJI: return KeyEvent.VK_KANJI;
            case Keyboard.KEY_L: return KeyEvent.VK_L;
            case Keyboard.KEY_LBRACKET: return KeyEvent.VK_OPEN_BRACKET;
            case Keyboard.KEY_LCONTROL: return KeyEvent.VK_CONTROL;
            case Keyboard.KEY_LEFT: return KeyEvent.VK_LEFT;
            case Keyboard.KEY_LMENU: return KeyEvent.VK_ALT;
            case Keyboard.KEY_LMETA: return KeyEvent.VK_META;
            case Keyboard.KEY_LSHIFT: return KeyEvent.VK_SHIFT;
            case Keyboard.KEY_M: return KeyEvent.VK_M;
            case Keyboard.KEY_MINUS: return KeyEvent.VK_MINUS;
            case Keyboard.KEY_MULTIPLY: return KeyEvent.VK_MULTIPLY;
            case Keyboard.KEY_N: return KeyEvent.VK_N;
            case Keyboard.KEY_NEXT: return KeyEvent.VK_PAGE_DOWN;
            case Keyboard.KEY_NOCONVERT: return KeyEvent.VK_NONCONVERT;
            case Keyboard.KEY_NONE: return KeyEvent.VK_UNDEFINED;
            case Keyboard.KEY_NUMLOCK: return KeyEvent.VK_NUM_LOCK;
            case Keyboard.KEY_NUMPAD0: return KeyEvent.VK_NUMPAD0;
            case Keyboard.KEY_NUMPAD1: return KeyEvent.VK_NUMPAD1;
            case Keyboard.KEY_NUMPAD2: return KeyEvent.VK_NUMPAD2;
            case Keyboard.KEY_NUMPAD3: return KeyEvent.VK_NUMPAD3;
            case Keyboard.KEY_NUMPAD4: return KeyEvent.VK_NUMPAD4;
            case Keyboard.KEY_NUMPAD5: return KeyEvent.VK_NUMPAD5;
            case Keyboard.KEY_NUMPAD6: return KeyEvent.VK_NUMPAD6;
            case Keyboard.KEY_NUMPAD7: return KeyEvent.VK_NUMPAD7;
            case Keyboard.KEY_NUMPAD8: return KeyEvent.VK_NUMPAD8;
            case Keyboard.KEY_NUMPAD9: return KeyEvent.VK_NUMPAD9;
            case Keyboard.KEY_NUMPADCOMMA: return KeyEvent.VK_COMMA;
            case Keyboard.KEY_NUMPADENTER: return KeyEvent.VK_ENTER;
            case Keyboard.KEY_NUMPADEQUALS: return KeyEvent.VK_EQUALS;
            case Keyboard.KEY_O: return KeyEvent.VK_O;
            case Keyboard.KEY_P: return KeyEvent.VK_P;
            case Keyboard.KEY_PAUSE: return KeyEvent.VK_PAUSE;
            case Keyboard.KEY_PERIOD: return KeyEvent.VK_PERIOD;
            case Keyboard.KEY_POWER: return KeyEvent.VK_UNDEFINED;
            case Keyboard.KEY_PRIOR: return KeyEvent.VK_PAGE_UP;
            case Keyboard.KEY_Q: return KeyEvent.VK_Q;
            case Keyboard.KEY_R: return KeyEvent.VK_R;
            case Keyboard.KEY_RBRACKET: return KeyEvent.VK_CLOSE_BRACKET;
            case Keyboard.KEY_RCONTROL: return KeyEvent.VK_CONTROL;
            case Keyboard.KEY_RETURN: return KeyEvent.VK_ENTER;
            case Keyboard.KEY_RIGHT: return KeyEvent.VK_RIGHT;
            case Keyboard.KEY_RMENU: return KeyEvent.VK_ALT;
            case Keyboard.KEY_RMETA: return KeyEvent.VK_META;
            case Keyboard.KEY_RSHIFT: return KeyEvent.VK_SHIFT;
            case Keyboard.KEY_S: return KeyEvent.VK_S;
            case Keyboard.KEY_SCROLL: return KeyEvent.VK_SCROLL_LOCK;
            case Keyboard.KEY_SEMICOLON: return KeyEvent.VK_SEMICOLON;
            case Keyboard.KEY_SLASH: return KeyEvent.VK_SLASH;
            case Keyboard.KEY_SLEEP: return KeyEvent.VK_UNDEFINED;
            case Keyboard.KEY_SPACE: return KeyEvent.VK_SPACE;
            case Keyboard.KEY_STOP: return KeyEvent.VK_STOP;
            case Keyboard.KEY_SUBTRACT: return KeyEvent.VK_SUBTRACT;
            case Keyboard.KEY_SYSRQ: return KeyEvent.VK_PRINTSCREEN;
            case Keyboard.KEY_T: return KeyEvent.VK_T;
            case Keyboard.KEY_TAB: return KeyEvent.VK_TAB;
            case Keyboard.KEY_U: return KeyEvent.VK_U;
            case Keyboard.KEY_UNDERLINE: return KeyEvent.VK_UNDERSCORE;
            case Keyboard.KEY_UNLABELED: return KeyEvent.VK_UNDEFINED;
            case Keyboard.KEY_UP: return KeyEvent.VK_UP;
            case Keyboard.KEY_V: return KeyEvent.VK_V;
            case Keyboard.KEY_W: return KeyEvent.VK_W;
            case Keyboard.KEY_X: return KeyEvent.VK_X;
            case Keyboard.KEY_Y: return KeyEvent.VK_Y;
            case Keyboard.KEY_YEN: return KeyEvent.VK_UNDEFINED;
            case Keyboard.KEY_Z: return KeyEvent.VK_Z;
            default: return KeyEvent.VK_UNDEFINED;
        }
    }

    /**
     * Returns the AWT location corresponding to the specified key.
     */
    protected static int getAWTLocation (int key)
    {
        switch (key) {
            case Keyboard.KEY_LCONTROL:
            case Keyboard.KEY_LMENU:
            case Keyboard.KEY_LMETA:
            case Keyboard.KEY_LSHIFT:
                return KeyEvent.KEY_LOCATION_LEFT;

            case Keyboard.KEY_NUMLOCK:
            case Keyboard.KEY_NUMPAD0:
            case Keyboard.KEY_NUMPAD1:
            case Keyboard.KEY_NUMPAD2:
            case Keyboard.KEY_NUMPAD3:
            case Keyboard.KEY_NUMPAD4:
            case Keyboard.KEY_NUMPAD5:
            case Keyboard.KEY_NUMPAD6:
            case Keyboard.KEY_NUMPAD7:
            case Keyboard.KEY_NUMPAD8:
            case Keyboard.KEY_NUMPAD9:
            case Keyboard.KEY_NUMPADCOMMA:
            case Keyboard.KEY_NUMPADENTER:
            case Keyboard.KEY_NUMPADEQUALS:
                return KeyEvent.KEY_LOCATION_NUMPAD;

            case Keyboard.KEY_RCONTROL:
            case Keyboard.KEY_RMENU:
            case Keyboard.KEY_RMETA:
            case Keyboard.KEY_RSHIFT:
                return KeyEvent.KEY_LOCATION_RIGHT;

            default:
                return KeyEvent.KEY_LOCATION_STANDARD;
        }
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
     * Determines whether the current set of modifiers contains any pressed buttons.
     */
    protected static boolean anyButtonsDown (int modifiers)
    {
        return (modifiers & ANY_BUTTONS_DOWN_MASK) != 0;
    }

    /** Set on initialization. */
    protected boolean _initialized;

    /** The runnable that updates the frame. */
    protected Runnable _updater;

    /** Whether or not the mouse is over the component. */
    protected boolean _entered;

    /** The last position we reported. */
    protected int _lx, _ly;

    /** The last button states we reported. */
    protected boolean[] _lbuttons = new boolean[3];

    /** The number of wheel clicks recorded. */
    protected int _lclicks;

    /** A mask for checking whether any mouse buttons are down. */
    protected static final int ANY_BUTTONS_DOWN_MASK =
        InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;
}
