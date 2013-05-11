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

package com.threerings.opengl.gui.event;

/**
 * Encapsulates the information associated with a mouse event.
 */
public class MouseEvent extends InputEvent
{
    /** An event generated when a mouse button is pressed. */
    public static final int MOUSE_PRESSED = 0;

    /** An event generated when a mouse button is released. */
    public static final int MOUSE_RELEASED = 1;

    /** An event generated when a mouse button is clicked. */
    public static final int MOUSE_CLICKED = 2;

    /** An event generated when the mouse enters a component's bounds. */
    public static final int MOUSE_ENTERED = 3;

    /** An event generated when the mouse exits a component's bounds. */
    public static final int MOUSE_EXITED = 4;

    /** An event generated when the mouse is moved. */
    public static final int MOUSE_MOVED = 5;

    /** An event generated when the mouse is dragged. Drag events are
     * dispatched to the component in which a mouse is clicked and held
     * for all movement until all buttons are released. */
    public static final int MOUSE_DRAGGED = 6;

    /** An event generated when the mouse wheel was rotated. */
    public static final int MOUSE_WHEELED = 7;

    /** A constant representing the "left" mouse button. */
    public static final int BUTTON1 = 0;

    /** A constant representing the "right" mouse button. */
    public static final int BUTTON2 = 1;

    /** A constant representing the middle mouse button. */
    public static final int BUTTON3 = 2;

    /** A constant representing the 4th mouse button. */
    public static final int BUTTON4 = 3;

    /** A constant representing the 5th mouse button. */
    public static final int BUTTON5 = 4;
    public static final int BUTTON6 = 5;
    public static final int BUTTON7 = 6;
    public static final int BUTTON8 = 7;
    public static final int BUTTON9 = 8;
    public static final int BUTTON10 = 9;
    public static final int BUTTON11 = 10;
    public static final int BUTTON12 = 11;
    public static final int BUTTON13 = 12;
    public static final int BUTTON14 = 13;
    public static final int BUTTON15 = 14;
    public static final int BUTTON16 = 15;

    public static final int MAX_BUTTONS = 16;

    public MouseEvent (Object source, long when, int modifiers, int type,
                       int mx, int my)
    {
        this(source, when, modifiers, type, -1, mx, my);
    }

    public MouseEvent (Object source, long when, int modifiers, int type,
                       int button, int mx, int my)
    {
        this(source, when, modifiers, type, button, mx, my, 0);
    }

    public MouseEvent (Object source, long when, int modifiers, int type,
                       int button, int mx, int my, int ccount)
    {
        this(source, when, modifiers, type, button, mx, my, ccount, 0);
    }

    public MouseEvent (Object source, long when, int modifiers, int type,
                       int button, int mx, int my, int ccount, int delta)
    {
        super(source, when, modifiers);
        _type = type;
        _button = button;
        _mx = mx;
        _my = my;
        _ccount = ccount;
        _delta = delta;
    }

    /**
     * Returns the type of this event, one of {@link #MOUSE_PRESSED},
     * {#link MOUSE_RELEASE}, etc.
     */
    public int getType ()
    {
        return _type;
    }

    /**
     * Returns the index of the button pertaining to this event ({@link
     * #BUTTON1}, {@link #BUTTON2}, or {@link #BUTTON3}) or -1 if this is
     * not a button related event.
     */
    public int getButton ()
    {
        return _button;
    }

    /**
     * Returns the (absolute) x coordinate of the mouse at the time this
     * event was generated.
     */
    public int getX ()
    {
        return _mx;
    }

    /**
     * Returns the (absolute) y coordinate of the mouse at the time this
     * event was generated.
     */
    public int getY ()
    {
        return _my;
    }

    /**
     * Returns the click count of the event.
     */
    public int getClickCount ()
    {
        return _ccount;
    }

    /**
     * For mouse wheel events this indicates the delta by which the wheel
     * was moved. The units seem to be defined by the underlying OpenGL
     * wrapper, presently LWJGL.
     */
    public int getDelta ()
    {
        return _delta;
    }

    // documentation inherited
    public void dispatch (ComponentListener listener)
    {
        super.dispatch(listener);
        switch (_type) {
        case MOUSE_PRESSED:
            if (listener instanceof MouseListener) {
                ((MouseListener)listener).mousePressed(this);
            }
            break;

        case MOUSE_RELEASED:
            if (listener instanceof MouseListener) {
                ((MouseListener)listener).mouseReleased(this);
            }
            break;

        case MOUSE_CLICKED:
            if (listener instanceof MouseListener) {
                ((MouseListener)listener).mouseClicked(this);
            }
            break;

        case MOUSE_ENTERED:
            if (listener instanceof MouseListener) {
                ((MouseListener)listener).mouseEntered(this);
            }
            break;

        case MOUSE_EXITED:
            if (listener instanceof MouseListener) {
                ((MouseListener)listener).mouseExited(this);
            }
            break;

        case MOUSE_MOVED:
            if (listener instanceof MouseMotionListener) {
                ((MouseMotionListener)listener).mouseMoved(this);
            }
            break;

        case MOUSE_DRAGGED:
            if (listener instanceof MouseMotionListener) {
                ((MouseMotionListener)listener).mouseDragged(this);
            }
            break;

        case MOUSE_WHEELED:
            if (listener instanceof MouseWheelListener) {
                ((MouseWheelListener)listener).mouseWheeled(this);
            }
            break;
        }
    }

    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", type=").append(_type);
        buf.append(", button=").append(_button);
        buf.append(", x=").append(_mx);
        buf.append(", y=").append(_my);
        if (_ccount != 0) {
            buf.append(", ccount=").append(_ccount);
        }
        if (_delta != 0) {
            buf.append(", delta=").append(_delta);
        }
    }

    protected int _type;
    protected int _button;
    protected int _mx;
    protected int _my;
    protected int _ccount;
    protected int _delta;
}
