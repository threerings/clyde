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

import com.threerings.config.ConfigReference;

import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.text.HTMLView;
import com.threerings.opengl.util.GlContext;

/**
 * Overrides default values of a component's tooltip.
 */
public class Tooltip
{
    /**
     * Configures the tooltip text for this component. If the text starts with &lt;html&gt; then
     * the tooltip will be displayed with an {@link HTMLView} otherwise it will be displayed with a
     * {@link Label}.
     */
    public void setText (String text)
    {
        _tiptext = text;
    }

    /**
     * Returns the tooltip text configured for this component.
     */
    public String getText ()
    {
        return _tiptext;
    }

    /**
     * Sets where to position the tooltip window.
     *
     * @param mouse if true, the window will appear relative to the mouse position, if false, the
     * window will appear relative to the component bounds.
     */
    public void setRelativeToMouse (boolean mouse)
    {
        _tipmouse = mouse;
    }

    /**
     * Returns true if the tooltip window should be position relative to the mouse.
     */
    public boolean isRelativeToMouse ()
    {
        return _tipmouse;
    }

    /**
     * Returns the component's tooltip timeout, or -1 to use the default.
     */
    public float getTimeout ()
    {
        return -1f;
    }

    /**
     * Returns a reference to the component's tooltip window style config.
     */
    public String getWindowStyle ()
    {
        return "Default/TooltipWindow";
    }

    public void setStyle (ConfigReference<StyleConfig> style)
    {
        _style = style;
    }

    /**
     * Creates the component that will be used to display our tooltip. This method will only be
     * called if {@link Component#getTooltipText} returns non-null text.
     */
    protected Component createComponent (GlContext ctx, String tiptext)
    {
        return Component.createDefaultTooltipComponent(ctx, tiptext, _style);
    }

    /** The style to use for tooltips. */
    protected ConfigReference<StyleConfig> _style;

    /** The tip text. */
    protected String _tiptext;

    /** Tip relative to mouse. */
    protected boolean _tipmouse;
}
