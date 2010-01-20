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
        if (isEnabled() && event instanceof MouseEvent) {
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

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return "Default/MenuItem";
    }

    /**
     * Called when the menu item is "clicked" which may due to the mouse
     * being pressed and released while over the item or due to keyboard
     * manipulation while the item has focus.
     */
    protected void fireAction (long when, int modifiers)
    {
        for (Component ancestor = _parent; ancestor != null; ancestor = ancestor.getParent()) {
            if (ancestor instanceof PopupMenu) {
                ((PopupMenu)ancestor).itemSelected(this, when, modifiers);
                return;
            }
        }
    }

    protected String _action;
    protected boolean _armed, _pressed;
}
