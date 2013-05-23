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
        super.setText(message);
        if (_flashAlert != null) {
            _flashAlert.cancel();
        }
        final Icon alert = _icons[getState()];
        final BlankIcon blank = (alert == null) ?
            null : new BlankIcon(alert.getWidth(), alert.getHeight());
        setIcon(flash ? alert : blank);
        if (flash) {
            (_flashAlert = new Interval(_ctx.getApp().getRunQueue()) {
                public void expired () {
                    _flashCount++;
                    setIcon(_flashCount % 2 == 0 ? alert : blank);
                    if (_flashCount == 5) {
                        cancel();
                    }
                }
                protected int _flashCount = 0;
            }).schedule(FLASH_DELAY, true);
        }
    }

    @Override
    public void setText (String text)
    {
        if (_icons == null) {
            // not yet initialized
            super.setText(text);

        } else {
            setStatus(text, false);
        }
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/StatusLabel";
    }

    @Override
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);
        _icons[state] = (config.icon == null) ? null : config.icon.getIcon(_ctx);
    }

    /** The icons for each state. */
    protected Icon[] _icons = new Icon[getStateCount()];

    /** The flash interval, if any. */
    protected Interval _flashAlert;

    /** The delay between flashes. */
    protected static final long FLASH_DELAY = 300L;
}
