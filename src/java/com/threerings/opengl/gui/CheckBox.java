//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.icon.Icon;

/**
 * Displays a label with a check-box button next to it.
 */
public class CheckBox extends ToggleButton
{
    public CheckBox (GlContext ctx, String label)
    {
        super(ctx, label);
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "checkbox";
    }

    // documentation inherited
    protected void configureStyle (StyleSheet style)
    {
        super.configureStyle(style);

        for (int ii = 0; ii < getStateCount(); ii++) {
            _icons[ii] = style.getIcon(this, getStatePseudoClass(ii));
        }
        _label.setIcon(_icons[getState()]);
    }

    // documentation inherited
    protected void stateDidChange ()
    {
        super.stateDidChange();

        // configure our checkbox icon
        _label.setIcon(_icons[getState()]);
    }

    protected Icon[] _icons = new Icon[getStateCount()];
}
