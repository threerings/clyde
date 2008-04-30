//
// $Id$

package com.threerings.opengl.gui;

import java.awt.Canvas;
import java.awt.EventQueue;

import org.lwjgl.input.Keyboard;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.InputEvent;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.event.MouseEvent;

/**
 * Bridges between the AWT and the BUI input event system when we are
 * being used in an AWT canvas.
 */
public class CanvasRoot extends Root
    implements java.awt.event.MouseListener, java.awt.event.MouseMotionListener,
               java.awt.event.MouseWheelListener, java.awt.event.KeyListener
{
    public CanvasRoot (GlContext ctx, Canvas canvas)
    {
        super(ctx);
        _canvas = canvas;
        _clipboard = canvas.getToolkit().getSystemSelection();

        // we want to hear about mouse movement, clicking, and keys
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseWheelListener(this);
        canvas.addKeyListener(this);
    }

    @Override // documentation inherited
    public int getDisplayWidth ()
    {
        return _canvas.getWidth();
    }

    @Override // documentation inherited
    public int getDisplayHeight ()
    {
        return _canvas.getHeight();
    }

    @Override // documentation inherited
    public void setCursor (Cursor cursor)
    {
        _canvas.setCursor(cursor == null ? null : cursor.getAWTCursor(_canvas.getToolkit()));
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
        updateState(e);

        setFocus(_ccomponent = getTargetComponent());
        MouseEvent event = new MouseEvent(
            this, e.getWhen(), _modifiers, MouseEvent.MOUSE_PRESSED,
            convertButton(e), _mouseX, _mouseY);
        maybeConsume(e, event);
        dispatchMouseEvent(_ccomponent, event);
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (java.awt.event.MouseEvent e)
    {
        updateState(e);

        MouseEvent event = new MouseEvent(
            this, e.getWhen(), _modifiers, MouseEvent.MOUSE_RELEASED,
            convertButton(e), _mouseX, _mouseY);
        maybeConsume(e, event);
        dispatchMouseEvent(getTargetComponent(), event);
        _ccomponent = null;
        updateHoverComponent(_mouseX, _mouseY);
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseDragged (java.awt.event.MouseEvent e)
    {
        mouseMoved(e);
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseMoved (java.awt.event.MouseEvent e)
    {
        boolean mouseMoved = updateState(e);

        // if the mouse has moved, generate a moved or dragged event
        if (mouseMoved) {
            Component tcomponent = getTargetComponent();
            int type = (tcomponent != null && tcomponent == _ccomponent) ?
                MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED;
            MouseEvent event = new MouseEvent(
                this, e.getWhen(), _modifiers, type, _mouseX, _mouseY);
            maybeConsume(e, event);
            dispatchMouseEvent(tcomponent, event);
        }
    }

    // documentation inherited from interface MouseWheelListener
    public void mouseWheelMoved (java.awt.event.MouseWheelEvent e)
    {
        updateState(e);

        MouseEvent event = new MouseEvent(
            this, e.getWhen(), _modifiers, MouseEvent.MOUSE_WHEELED,
            convertButton(e), _mouseX, _mouseY, 0, -e.getWheelRotation());
        maybeConsume(e, event);
        dispatchMouseEvent(getTargetComponent(), event);
    }

    // documentation inherited from interface KeyListener
    public void keyPressed (java.awt.event.KeyEvent e)
    {
        // update our modifiers
        _modifiers = convertModifiers(e.getModifiers());

        KeyEvent event = new KeyEvent(
            this, e.getWhen(), _modifiers, KeyEvent.KEY_PRESSED,
            e.getKeyChar(), convertKeyCode(e));
        maybeConsume(e, event);
        dispatchKeyEvent(getFocus(), event);
    }

    // documentation inherited from interface KeyListener
    public void keyReleased (java.awt.event.KeyEvent e)
    {
        // update our modifiers
        _modifiers = convertModifiers(e.getModifiers());

        KeyEvent event = new KeyEvent(
            this, e.getWhen(), _modifiers, KeyEvent.KEY_RELEASED,
            e.getKeyChar(), convertKeyCode(e));
        maybeConsume(e, event);
        dispatchKeyEvent(getFocus(), event);
    }

    // documentation inherited from interface KeyListener
    public void keyTyped (java.awt.event.KeyEvent e)
    {
        // N/A
    }

    @Override // documentation inherited
    protected void updateHoverComponent (int mx, int my)
    {
        // delay updating the hover component if we have a clicked component
        if (_ccomponent == null) {
            super.updateHoverComponent(mx, my);
        }
    }

    protected boolean updateState (java.awt.event.MouseEvent e)
    {
        // update our modifiers
        _modifiers = convertModifiers(e.getModifiers());

        // determine whether the mouse moved
        int mx = e.getX(), my = _canvas.getHeight() - e.getY();
        if (_mouseX != mx || _mouseY != my) {
            mouseDidMove(mx, my);
            return true;
        }

        return false;
    }

    protected Component getTargetComponent ()
    {
        // mouse press and mouse motion events do not necessarily go to
        // the component under the mouse. when the mouse is clicked down
        // on a component (any button), it becomes the "clicked"
        // component, the target for all subsequent click and motion
        // events (which become drag events) until all buttons are
        // released
        if (_ccomponent != null) {
            return _ccomponent;
        }
        // if there's no clicked component, use the hover component
        if (_hcomponent != null) {
            return _hcomponent;
        }
        // if there's no hover component, use the default event target
        return null;
    }

    protected int convertModifiers (int modifiers)
    {
        int nmodifiers = 0;
        if ((modifiers & java.awt.event.InputEvent.BUTTON1_MASK) != 0) {
            nmodifiers |= InputEvent.BUTTON1_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.BUTTON3_MASK) != 0) {
            nmodifiers |= InputEvent.BUTTON2_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.BUTTON2_MASK) != 0) {
            nmodifiers |= InputEvent.BUTTON3_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.SHIFT_MASK) != 0) {
            nmodifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.CTRL_MASK) != 0) {
            nmodifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.ALT_MASK) != 0) {
            nmodifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if ((modifiers & java.awt.event.InputEvent.META_MASK) != 0) {
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
        switch (e.getKeyCode()) {
        case java.awt.event.KeyEvent.VK_ESCAPE: return Keyboard.KEY_ESCAPE;
        case java.awt.event.KeyEvent.VK_1: return Keyboard.KEY_1;
        case java.awt.event.KeyEvent.VK_2: return Keyboard.KEY_2;
        case java.awt.event.KeyEvent.VK_3: return Keyboard.KEY_3;
        case java.awt.event.KeyEvent.VK_4: return Keyboard.KEY_4;
        case java.awt.event.KeyEvent.VK_5: return Keyboard.KEY_5;
        case java.awt.event.KeyEvent.VK_6: return Keyboard.KEY_6;
        case java.awt.event.KeyEvent.VK_7: return Keyboard.KEY_7;
        case java.awt.event.KeyEvent.VK_8: return Keyboard.KEY_8;
        case java.awt.event.KeyEvent.VK_9: return Keyboard.KEY_9;
        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_0;
        case java.awt.event.KeyEvent.VK_MINUS: return Keyboard.KEY_MINUS;
        case java.awt.event.KeyEvent.VK_EQUALS: return e.getKeyLocation() ==
            java.awt.event.KeyEvent.KEY_LOCATION_NUMPAD ?
                Keyboard.KEY_NUMPADEQUALS : Keyboard.KEY_EQUALS;
        case java.awt.event.KeyEvent.VK_BACK_SPACE: return Keyboard.KEY_BACK;
        case java.awt.event.KeyEvent.VK_TAB: return Keyboard.KEY_TAB;
        case java.awt.event.KeyEvent.VK_Q: return Keyboard.KEY_Q;
        case java.awt.event.KeyEvent.VK_W: return Keyboard.KEY_W;
        case java.awt.event.KeyEvent.VK_E: return Keyboard.KEY_E;
        case java.awt.event.KeyEvent.VK_R: return Keyboard.KEY_R;
        case java.awt.event.KeyEvent.VK_T: return Keyboard.KEY_T;
        case java.awt.event.KeyEvent.VK_Y: return Keyboard.KEY_Y;
        case java.awt.event.KeyEvent.VK_U: return Keyboard.KEY_U;
        case java.awt.event.KeyEvent.VK_I: return Keyboard.KEY_I;
        case java.awt.event.KeyEvent.VK_O: return Keyboard.KEY_O;
        case java.awt.event.KeyEvent.VK_P: return Keyboard.KEY_P;
        case java.awt.event.KeyEvent.VK_OPEN_BRACKET:
            return Keyboard.KEY_LBRACKET;
        case java.awt.event.KeyEvent.VK_CLOSE_BRACKET:
            return Keyboard.KEY_RBRACKET;
        case java.awt.event.KeyEvent.VK_ENTER: return e.getKeyLocation() ==
            java.awt.event.KeyEvent.KEY_LOCATION_NUMPAD ?
                Keyboard.KEY_NUMPADENTER : Keyboard.KEY_RETURN;
        case java.awt.event.KeyEvent.VK_CONTROL: return e.getKeyLocation() ==
            java.awt.event.KeyEvent.KEY_LOCATION_LEFT ?
                Keyboard.KEY_LCONTROL : Keyboard.KEY_RCONTROL;
        case java.awt.event.KeyEvent.VK_A: return Keyboard.KEY_A;
        case java.awt.event.KeyEvent.VK_S: return Keyboard.KEY_S;
        case java.awt.event.KeyEvent.VK_D: return Keyboard.KEY_D;
        case java.awt.event.KeyEvent.VK_F: return Keyboard.KEY_F;
        case java.awt.event.KeyEvent.VK_G: return Keyboard.KEY_G;
        case java.awt.event.KeyEvent.VK_H: return Keyboard.KEY_H;
        case java.awt.event.KeyEvent.VK_J: return Keyboard.KEY_J;
        case java.awt.event.KeyEvent.VK_K: return Keyboard.KEY_K;
        case java.awt.event.KeyEvent.VK_L: return Keyboard.KEY_L;
        case java.awt.event.KeyEvent.VK_SEMICOLON:
            return Keyboard.KEY_SEMICOLON;
        case java.awt.event.KeyEvent.VK_QUOTE: return Keyboard.KEY_APOSTROPHE;
        case java.awt.event.KeyEvent.VK_BACK_QUOTE: return Keyboard.KEY_GRAVE;
        case java.awt.event.KeyEvent.VK_SHIFT: return e.getKeyLocation() ==
            java.awt.event.KeyEvent.KEY_LOCATION_LEFT ?
                Keyboard.KEY_LSHIFT : Keyboard.KEY_RSHIFT;
        case java.awt.event.KeyEvent.VK_BACK_SLASH:
            return Keyboard.KEY_BACKSLASH;
        case java.awt.event.KeyEvent.VK_Z: return Keyboard.KEY_Z;
        case java.awt.event.KeyEvent.VK_X: return Keyboard.KEY_X;
        case java.awt.event.KeyEvent.VK_C: return Keyboard.KEY_C;
        case java.awt.event.KeyEvent.VK_V: return Keyboard.KEY_V;
        case java.awt.event.KeyEvent.VK_B: return Keyboard.KEY_B;
        case java.awt.event.KeyEvent.VK_N: return Keyboard.KEY_N;
        case java.awt.event.KeyEvent.VK_M: return Keyboard.KEY_M;
        case java.awt.event.KeyEvent.VK_COMMA: return e.getKeyLocation() ==
            java.awt.event.KeyEvent.KEY_LOCATION_NUMPAD ?
                Keyboard.KEY_NUMPADCOMMA : Keyboard.KEY_COMMA;
        case java.awt.event.KeyEvent.VK_PERIOD: return Keyboard.KEY_PERIOD;
        case java.awt.event.KeyEvent.VK_SLASH: return Keyboard.KEY_SLASH;
        case java.awt.event.KeyEvent.VK_MULTIPLY: return Keyboard.KEY_MULTIPLY;
//        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_LMENU;
        case java.awt.event.KeyEvent.VK_SPACE: return Keyboard.KEY_SPACE;
        case java.awt.event.KeyEvent.VK_CAPS_LOCK: return Keyboard.KEY_CAPITAL;
        case java.awt.event.KeyEvent.VK_F1: return Keyboard.KEY_F1;
        case java.awt.event.KeyEvent.VK_F2: return Keyboard.KEY_F2;
        case java.awt.event.KeyEvent.VK_F3: return Keyboard.KEY_F3;
        case java.awt.event.KeyEvent.VK_F4: return Keyboard.KEY_F4;
        case java.awt.event.KeyEvent.VK_F5: return Keyboard.KEY_F5;
        case java.awt.event.KeyEvent.VK_F6: return Keyboard.KEY_F6;
        case java.awt.event.KeyEvent.VK_F7: return Keyboard.KEY_F7;
        case java.awt.event.KeyEvent.VK_F8: return Keyboard.KEY_F8;
        case java.awt.event.KeyEvent.VK_F9: return Keyboard.KEY_F9;
        case java.awt.event.KeyEvent.VK_F10: return Keyboard.KEY_F10;
        case java.awt.event.KeyEvent.VK_NUM_LOCK: return Keyboard.KEY_NUMLOCK;
        case java.awt.event.KeyEvent.VK_SCROLL_LOCK: return Keyboard.KEY_SCROLL;
        case java.awt.event.KeyEvent.VK_NUMPAD7: return Keyboard.KEY_NUMPAD7;
        case java.awt.event.KeyEvent.VK_NUMPAD8: return Keyboard.KEY_NUMPAD8;
        case java.awt.event.KeyEvent.VK_NUMPAD9: return Keyboard.KEY_NUMPAD9;
        case java.awt.event.KeyEvent.VK_SUBTRACT: return Keyboard.KEY_SUBTRACT;
        case java.awt.event.KeyEvent.VK_NUMPAD4: return Keyboard.KEY_NUMPAD4;
        case java.awt.event.KeyEvent.VK_NUMPAD5: return Keyboard.KEY_NUMPAD5;
        case java.awt.event.KeyEvent.VK_NUMPAD6: return Keyboard.KEY_NUMPAD6;
        case java.awt.event.KeyEvent.VK_ADD: return Keyboard.KEY_ADD;
        case java.awt.event.KeyEvent.VK_NUMPAD1: return Keyboard.KEY_NUMPAD1;
        case java.awt.event.KeyEvent.VK_NUMPAD2: return Keyboard.KEY_NUMPAD2;
        case java.awt.event.KeyEvent.VK_NUMPAD3: return Keyboard.KEY_NUMPAD3;
        case java.awt.event.KeyEvent.VK_NUMPAD0: return Keyboard.KEY_NUMPAD0;
        case java.awt.event.KeyEvent.VK_DECIMAL: return Keyboard.KEY_DECIMAL;
        case java.awt.event.KeyEvent.VK_F11: return Keyboard.KEY_F11;
        case java.awt.event.KeyEvent.VK_F12: return Keyboard.KEY_F12;
        case java.awt.event.KeyEvent.VK_F13: return Keyboard.KEY_F13;
        case java.awt.event.KeyEvent.VK_F14: return Keyboard.KEY_F14;
        case java.awt.event.KeyEvent.VK_F15: return Keyboard.KEY_F15;
        case java.awt.event.KeyEvent.VK_KANA: return Keyboard.KEY_KANA;
        case java.awt.event.KeyEvent.VK_CONVERT: return Keyboard.KEY_CONVERT;
        case java.awt.event.KeyEvent.VK_NONCONVERT:
            return Keyboard.KEY_NOCONVERT;
//        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_YEN;
        case java.awt.event.KeyEvent.VK_CIRCUMFLEX:
            return Keyboard.KEY_CIRCUMFLEX;
        case java.awt.event.KeyEvent.VK_AT: return Keyboard.KEY_AT;
        case java.awt.event.KeyEvent.VK_COLON: return Keyboard.KEY_COLON;
        case java.awt.event.KeyEvent.VK_UNDERSCORE:
            return Keyboard.KEY_UNDERLINE;
        case java.awt.event.KeyEvent.VK_KANJI: return Keyboard.KEY_KANJI;
        case java.awt.event.KeyEvent.VK_STOP: return Keyboard.KEY_STOP;
//        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_AX;
        case java.awt.event.KeyEvent.VK_UNDEFINED:
            return Keyboard.KEY_UNLABELED;
        case java.awt.event.KeyEvent.VK_DIVIDE: return Keyboard.KEY_DIVIDE;
        case java.awt.event.KeyEvent.VK_PRINTSCREEN: return Keyboard.KEY_SYSRQ;
//        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_RMENU;
        case java.awt.event.KeyEvent.VK_PAUSE: return Keyboard.KEY_PAUSE;
        case java.awt.event.KeyEvent.VK_HOME: return Keyboard.KEY_HOME;
        case java.awt.event.KeyEvent.VK_UP: return Keyboard.KEY_UP;
//        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_PRIOR;
        case java.awt.event.KeyEvent.VK_PAGE_UP: return Keyboard.KEY_PRIOR;
        case java.awt.event.KeyEvent.VK_LEFT: return Keyboard.KEY_LEFT;
        case java.awt.event.KeyEvent.VK_RIGHT: return Keyboard.KEY_RIGHT;
        case java.awt.event.KeyEvent.VK_END: return Keyboard.KEY_END;
        case java.awt.event.KeyEvent.VK_DOWN: return Keyboard.KEY_DOWN;
//        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_NEXT;
        case java.awt.event.KeyEvent.VK_PAGE_DOWN: return Keyboard.KEY_NEXT;
        case java.awt.event.KeyEvent.VK_INSERT: return Keyboard.KEY_INSERT;
        case java.awt.event.KeyEvent.VK_DELETE: return Keyboard.KEY_DELETE;
//        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_LWIN;
//        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_RWIN;
//        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_APPS;
//        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_POWER;
//        case java.awt.event.KeyEvent.VK_0: return Keyboard.KEY_SLEEP;
        default: return Keyboard.KEY_UNLABELED;
        }
    }

    /**
     * Consumes the destination event if the source event is consumed.
     */
    protected static void maybeConsume (java.awt.event.InputEvent src, InputEvent dest)
    {
        if (src.isConsumed()) {
            dest.consume();
        }
    }

    protected Canvas _canvas;
}
