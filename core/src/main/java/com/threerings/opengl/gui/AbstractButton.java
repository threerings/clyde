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

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.icon.Icon;

/**
 * Building-block for a button.
 */
public abstract class AbstractButton extends Label
{
    /**
     * Creates a button with the specified icon, label, action, and argument.
     */
    public AbstractButton (GlContext ctx, Icon icon, String text, String action, Object argument)
    {
        super(ctx, icon, text);
        _action = action;
        _argument = argument;
    }

    /**
     * Configures the action to be generated when this button is clicked.
     */
    public void setAction (String action)
    {
        _action = action;
    }

    /**
     * Returns the action generated when this button is clicked.
     */
    public String getAction ()
    {
        return _action;
    }

    /**
     * Set the argument dispatched by this button.
     */
    public void setArgument (Object argument)
    {
        _argument = argument;
    }

    /**
     * Get the argument dispatched by this button.
     */
    public Object getArgument ()
    {
        return _argument;
    }

    // documentation inherited
    public boolean dispatchEvent (Event event)
    {
        if (isEnabled() && event instanceof MouseEvent) {
            int ostate = getState();
            MouseEvent mev = (MouseEvent)event;
            switch (mev.getType()) {
            default:
                return super.dispatchEvent(event);

            case MouseEvent.MOUSE_DRAGGED:
                // disarm if the mouse is dragged beyond the bounds (this is not an "exit": that
                // happens when the mouse button is released
                int mx = mev.getX(), my = mev.getY();
                int ax = getAbsoluteX(), ay = getAbsoluteY();
                _armed = _pressed &&
                    ((mx >= ax) && (my >= ay) && (mx < ax + _width) && (my < ay + _height));
                break;

            case MouseEvent.MOUSE_PRESSED:
                // this also disarms the button currently armed and another mousebutton is pressed
                _pressed = _armed = (mev.getButton() == MouseEvent.BUTTON1);
                break;

            case MouseEvent.MOUSE_RELEASED:
                if (_armed) {
                    fireAction(mev.getWhen(), mev.getModifiers());
                }
                _pressed = _armed = false;
                break;
            }

            // update our background image if necessary
            int state = getState();
            if (state != ostate) {
                stateDidChange();
            }

            // dispatch this event to our listeners
            if (_listeners != null) {
                for (int ii = 0, ll = _listeners.size(); ii < ll; ii++) {
                    event.dispatch(_listeners.get(ii));
                }
            }

            return true;
        }

        return super.dispatchEvent(event);
    }

    /**
     * Called when the button is "clicked" which may due to the mouse being
     * pressed and released while over the button or due to keyboard
     * manipulation while the button has focus.
     */
    protected abstract void fireAction (long when, int modifiers);

    /** The action we'll fire. */
    protected String _action;

    /** The argument for the action. */
    protected Object _argument;

    /** Has a pressed action been started on this button? */
    protected boolean _pressed;

    /** Is this button actually armed? */
    protected boolean _armed;
}
