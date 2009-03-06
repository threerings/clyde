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

package com.threerings.opengl.gui;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;

/**
 * Displays a simple button that can be depressed and which generates an action
 * event when pressed and released.
 */
public class Button extends Label
    implements UIConstants
{
    /** Indicates that this button is in the down state. */
    public static final int DOWN = Component.STATE_COUNT + 0;

    /**
     * Creates a button with the specified textual label.
     */
    public Button (GlContext ctx, String text)
    {
        this(ctx, text, "");
    }

    /**
     * Creates a button with the specified label and action. The action
     * will be dispatched via an {@link ActionEvent} when the button is
     * clicked.
     */
    public Button (GlContext ctx, String text, String action)
    {
        this(ctx, text, null, action);
    }

    /**
     * Creates a button with the specified label and action. The action will be
     * dispatched via an {@link ActionEvent} to the specified {@link
     * ActionListener} when the button is clicked.
     */
    public Button (GlContext ctx, String text, ActionListener listener, String action)
    {
        super(ctx, text);
        _action = action;
        if (listener != null) {
            addListener(listener);
        }
    }

    /**
     * Creates a button with the specified icon and action. The action will be
     * dispatched via an {@link ActionEvent} when the button is clicked.
     */
    public Button (GlContext ctx, Icon icon, String action)
    {
        this(ctx, icon, null, action);
    }

    /**
     * Creates a button with the specified icon and action. The action will be
     * dispatched via an {@link ActionEvent} to the specified {@link
     * ActionListener} when the button is clicked.
     */
    public Button (GlContext ctx, Icon icon, ActionListener listener, String action)
    {
        super(ctx, icon);
        _action = action;
        if (listener != null) {
            addListener(listener);
        }
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

    // documentation inherited
    public int getState ()
    {
        int state = super.getState();
        if (state == DISABLED) {
            return state;
        }

        if (_armed && _pressed) {
            return DOWN;
        } else {
            return state; // most likely HOVER
        }
    }

    // documentation inherited
    public boolean dispatchEvent (Event event)
    {
        if (isEnabled() && event instanceof MouseEvent) {
            int ostate = getState();
            MouseEvent mev = (MouseEvent)event;
            switch (mev.getType()) {
            case MouseEvent.MOUSE_DRAGGED:
                // disarm if the mouse is dragged beyond the bounds (this is not an "exit": that
                // happens when the mouse button is released
                int mx = mev.getX(), my = mev.getY();
                int ax = getAbsoluteX(), ay = getAbsoluteY();
                if ((mx >= ax) && (my >= ay) && (mx < ax + _width) && (my < ay + _height)) {
                    _armed = _pressed = (mev.getModifiers() & MouseEvent.BUTTON1_DOWN_MASK) != 0;
                } else {
                    _armed = _pressed = false;
                }
                break;

            case MouseEvent.MOUSE_ENTERED:
                _armed = _pressed;
                // let the normal component hovered processing take place
                return super.dispatchEvent(event);

            case MouseEvent.MOUSE_EXITED:
                _armed = false;
                // let the normal component hovered processing take place
                return super.dispatchEvent(event);

            case MouseEvent.MOUSE_PRESSED:
                if (mev.getButton() == 0) {
                    _pressed = true;
                    _armed = true;
                }
                break;

            case MouseEvent.MOUSE_RELEASED:
                if (_armed && _pressed) {
                    // create and dispatch an action event
                    fireAction(mev.getWhen(), mev.getModifiers());
                    _armed = false;
                }
                _pressed = false;
                break;

            default:
                return super.dispatchEvent(event);
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

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return "Default/Button";
    }

    // documentation inherited
    protected int getStateCount ()
    {
        return STATE_COUNT;
    }

    // documentation inherited
    protected String getStatePseudoClass (int state)
    {
        if (state >= Component.STATE_COUNT) {
            return STATE_PCLASSES[state-Component.STATE_COUNT];
        } else {
            return super.getStatePseudoClass(state);
        }
    }

    @Override // documentation inherited
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);

        // check to see if our stylesheet provides us with an icon
        if (state == DEFAULT && config.icon != null) {
            _label.setIcon(config.icon.getIcon(_ctx));
        }
    }

    /**
     * Called when the button is "clicked" which may due to the mouse being
     * pressed and released while over the button or due to keyboard
     * manipulation while the button has focus.
     */
    protected void fireAction (long when, int modifiers)
    {
        emitEvent(new ActionEvent(this, when, modifiers, _action));
    }

    protected boolean _armed, _pressed;
    protected String _action;

    protected static final int STATE_COUNT = Component.STATE_COUNT + 1;
    protected static final String[] STATE_PCLASSES = { "Down" };
}
