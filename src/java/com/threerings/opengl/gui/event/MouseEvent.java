//
// $Id$

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

    /** An event generated when the mouse enters a component's bounds. */
    public static final int MOUSE_ENTERED = 2;

    /** An event generated when the mouse exits a component's bounds. */
    public static final int MOUSE_EXITED = 3;

    /** An event generated when the mouse is moved. */
    public static final int MOUSE_MOVED = 4;

    /** An event generated when the mouse is dragged. Drag events are
     * dispatched to the component in which a mouse is clicked and held
     * for all movement until all buttons are released. */
    public static final int MOUSE_DRAGGED = 5;

    /** An event generated when the mouse wheel was rotated. */
    public static final int MOUSE_WHEELED = 6;

    /** A constant representing the "left" mouse button. */
    public static final int BUTTON1 = 0;

    /** A constant representing the "right" mouse button. */
    public static final int BUTTON2 = 1;

    /** A constant representing the middle mouse button. */
    public static final int BUTTON3 = 2;

    public MouseEvent (Object source, long when, int modifiers, int type,
                       int mx, int my)
    {
        this(source, when, modifiers, type, -1, mx, my, 0);
    }

    public MouseEvent (Object source, long when, int modifiers, int type,
                       int button, int mx, int my)
    {
        this(source, when, modifiers, type, button, mx, my, 0);
    }

    public MouseEvent (Object source, long when, int modifiers, int type,
                       int button, int mx, int my, int delta)
    {
        super(source, when, modifiers);
        _type = type;
        _button = button;
        _mx = mx;
        _my = my;
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

    protected void toString (StringBuffer buf)
    {
        super.toString(buf);
        buf.append(", type=").append(_type);
        buf.append(", button=").append(_button);
        buf.append(", x=").append(_mx);
        buf.append(", y=").append(_my);
        if (_delta != 0) {
            buf.append(", delta=").append(_delta);
        }
    }

    protected int _type;
    protected int _button;
    protected int _mx;
    protected int _my;
    protected int _delta;
}
