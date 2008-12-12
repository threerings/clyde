//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.config.StyleConfig;
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

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return "Default/CheckBox";
    }

    @Override // documentation inherited
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);

        _icons[state] = (config.icon == null) ? null : config.icon.getIcon(_ctx);
        if (getState() == state) {
            _label.setIcon(_icons[state]);
        }
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
