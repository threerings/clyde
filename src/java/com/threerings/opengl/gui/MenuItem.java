//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.icon.Icon;

/**
 * Displays a single menu item.
 */
public class MenuItem extends Label
{
    /**
     * Creates a menu item with the specified text that will generate an
     * {@link ActionEvent} with the specified action when selected.
     */
    public MenuItem (GlContext ctx, String text, String action)
    {
        this(ctx, text, null, action);
    }

    /**
     * Creates a menu item with the specified icon that will generate an
     * {@link ActionEvent} with the specified action when selected.
     */
    public MenuItem (GlContext ctx, Icon icon, String action)
    {
        this(ctx, null, icon, action);
    }

    /**
     * Creates a menu item with the specified text and icon that will generate
     * an {@link ActionEvent} with the specified action when selected.
     */
    public MenuItem (GlContext ctx, String text, Icon icon, String action)
    {
        super(ctx, text);
        if (icon != null) {
            setIcon(icon);
        }
        _action = action;
    }

    /**
     * Returns the action configured for this menu item.
     */
    public String getAction ()
    {
        return _action;
    }

    // documentation inherited
    public boolean dispatchEvent (Event event)
    {
        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            switch (mev.getType()) {
            case MouseEvent.MOUSE_ENTERED:
                _armed = _pressed;
                break; // we don't consume this event

            case MouseEvent.MOUSE_EXITED:
                _armed = false;
                break; // we don't consume this event

            case MouseEvent.MOUSE_PRESSED:
                if (mev.getButton() == 0) {
                    _pressed = true;
                    _armed = true;
                } else if (mev.getButton() == 1) {
                    // clicking the right mouse button after arming the
                    // component disarms it
                    _armed = false;
                }
                return true; // consume this event

            case MouseEvent.MOUSE_RELEASED:
                if (_armed && _pressed) {
                    // create and dispatch an action event
                    fireAction(mev.getWhen(), mev.getModifiers());
                    _armed = false;
                }
                _pressed = false;
                return true; // consume this event
            }
        }

        return super.dispatchEvent(event);
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "menuitem";
    }

    /**
     * Called when the menu item is "clicked" which may due to the mouse
     * being pressed and released while over the item or due to keyboard
     * manipulation while the item has focus.
     */
    protected void fireAction (long when, int modifiers)
    {
        if (_parent instanceof PopupMenu) {
            ((PopupMenu)_parent).itemSelected(this, when, modifiers);
        }
    }

    protected String _action;
    protected boolean _armed, _pressed;
}
