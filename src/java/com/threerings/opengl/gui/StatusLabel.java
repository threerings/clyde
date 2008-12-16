//
// $Id$

package com.threerings.opengl.gui;

import com.samskivert.util.Interval;

import com.threerings.opengl.gui.Label;
import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.icon.BlankIcon;
import com.threerings.opengl.util.GlContext;

/**
 * Provides a convenient component for displaying feedback.
 */
public class StatusLabel extends Label
{
    /**
     * Creates a new status label.
     */
    public StatusLabel (GlContext ctx)
    {
        super(ctx, "");
    }

    /**
     * Translates and displays the specified status message.
     *
     * @param flash if true, an icon will be flashed three times next to the
     * status message to grab the users attention.
     */
    public void setStatus (String bundle, String message, boolean flash)
    {
        setStatus(_ctx.getApp().xlate(bundle, message), flash);
    }

    /**
     * Displays an <em>already translated</em> status message.
     *
     * @param flash if true, an icon will be flashed three times next to the
     * status message to grab the users attention.
     */
    public void setStatus (String message, boolean flash)
    {
        setText(message);
        if (flash) {
            final Icon alert = _icons[getState()];
            final BlankIcon blank = (alert == null) ?
                null : new BlankIcon(alert.getWidth(), alert.getHeight());
            setIcon(alert);
            Interval flashAlert = new Interval(_ctx.getApp().getRunQueue()) {
                public void expired () {
                    _flashCount++;
                    setIcon(_flashCount % 2 == 0 ? alert : blank);
                    if (_flashCount == 5) {
                        cancel();
                    }
                }
                protected int _flashCount = 0;
            };
            flashAlert.schedule(FLASH_DELAY, true);
        }
    }

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return "Default/StatusLabel";
    }

    @Override // documentation inherited
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);
        _icons[state] = (config.icon == null) ? null : config.icon.getIcon(_ctx);
    }

    /** The icons for each state. */
    protected Icon[] _icons = new Icon[getStateCount()];

    /** The delay between flashes. */
    protected static final long FLASH_DELAY = 300L;
}
