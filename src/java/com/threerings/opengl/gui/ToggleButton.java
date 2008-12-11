//
// $Id$

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

    /** Indicates that this button is in the selected state and is disabled. */
    public static final int DISSELECTED = Button.STATE_COUNT + 1;

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
        return _selected ? (state == DISABLED ? DISSELECTED : SELECTED) : state;
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
    protected void fireAction (long when, int modifiers)
    {
        // when the button fires its action (it was clicked) we know that it's
        // time to change state from selected to deselected or vice versa
        _selected = !_selected;
        super.fireAction(when, modifiers);
    }

    /** Used to track whether we are selected or not. */
    protected boolean _selected;

    protected static final int STATE_COUNT = Button.STATE_COUNT + 2;
    protected static final String[] STATE_PCLASSES = {
        "selected", "disselected" };
}
