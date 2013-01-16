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

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.icon.Icon;

/**
 * Like a {@link Button} except that it toggles between two states
 * (selected and normal) when clicked.
 */
public class ToggleButton extends Button
{
    /** Indicates that this button is in the selected state. */
    public static final int SELECTED = Button.STATE_COUNT + 0;

    /** Indicates that this button is in the selected state and hovered. */
    public static final int HOVER_SELECTED = Button.STATE_COUNT + 1;

    /** Indicates that this button is in the selected state and pressed. */
    public static final int DOWN_SELECTED = Button.STATE_COUNT + 2;

    /** Indicates that this button is in the selected state and is disabled. */
    public static final int DISABLED_SELECTED = Button.STATE_COUNT + 3;

    /**
     * Creates a button with the specified textual label.
     */
    public ToggleButton (GlContext ctx, String text)
    {
        super(ctx, text);
    }

    /**
     * Creates a button with the specified label and action. The action
     * will be dispatched via an {@link ActionEvent} when the button
     * changes state.
     */
    public ToggleButton (GlContext ctx, String text, String action)
    {
        super(ctx, text, action);
    }

    /**
     * Creates a button with the specified icon and action. The action
     * will be dispatched via an {@link ActionEvent} when the button
     * changes state.
     */
    public ToggleButton (GlContext ctx, Icon icon, String action)
    {
        super(ctx, icon, action);
    }

    /**
     * Returns whether or not this button is in the selected state.
     */
    public boolean isSelected ()
    {
        return _selected;
    }

    /**
     * Configures the selected state of this button.
     */
    public void setSelected (boolean selected)
    {
        if (_selected != selected) {
            _selected = selected;
            stateDidChange();
        }
    }

    // documentation inherited
    public int getState ()
    {
        int state = super.getState();
        if (!_selected) {
            return state;
        } else if (state == DISABLED) {
            return DISABLED_SELECTED;
        } else if (state == HOVER) {
            return HOVER_SELECTED;
        } else if (_armed) {
            return DOWN_SELECTED;
        } else {
            return SELECTED;
        }
    }

    // documentation inherited
    protected int getStateCount ()
    {
        return STATE_COUNT;
    }

    // documentation inherited
    protected String getStatePseudoClass (int state)
    {
        if (state >= Button.STATE_COUNT) {
            return STATE_PCLASSES[state-Button.STATE_COUNT];
        } else {
            return super.getStatePseudoClass(state);
        }
    }

    // documentation inherited
    protected int getFallbackState (int state)
    {
        return (state == HOVER_SELECTED ||
                state == DOWN_SELECTED ||
                state == DISABLED_SELECTED) ?
                SELECTED : DEFAULT;
    }

    // documentation inherited
    protected void fireAction (long when, int modifiers)
    {
        // when the button fires its action (it was clicked) we know that it's
        // time to change state from selected to deselected or vice versa
        _selected = !_selected;
        super.fireAction(when, modifiers);
    }

    /** Used to track whether we are selected or not. */
    protected boolean _selected;

    protected static final int STATE_COUNT = Button.STATE_COUNT + 4;
    protected static final String[] STATE_PCLASSES = {
        "Selected", "HoverSelected", "DownSelected", "DisabledSelected" };
}
