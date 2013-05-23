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

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.util.Dimension;

/**
 * A simple component for displaying a textual label.
 */
public class Label extends TextComponent
    implements UIConstants
{
    /** Configures the label's strategy when it does not fit into its allocated space. */
    public enum Fit { WRAP, TRUNCATE, SCALE };

    /**
     * Creates a label that will display the supplied text.
     */
    public Label (GlContext ctx, String text)
    {
        this(ctx, null, text);
    }

    /**
     * Creates a label that will display the supplied icon.
     */
    public Label (GlContext ctx, Icon icon)
    {
        this(ctx, icon, null);
    }

    /**
     * Creates a label that will display the supplied text and icon (either or
     * both of which can be null).
     */
    public Label (GlContext ctx, Icon icon, String text)
    {
        super(ctx);
        _label = new LabelRenderer(this);
        if (icon != null) {
            setIcon(icon);
        }
        if (text != null) {
            setText(text);
        }
    }

    /**
     * Configures the label to display the specified icon.
     */
    public void setIcon (Icon icon)
    {
        _label.setIcon(icon);
    }

    /**
     * Returns the icon being displayed by this label.
     */
    public Icon getIcon ()
    {
        return _label.getIcon();
    }

    /**
     * Configures the gap between the icon and the text.
     */
    public void setIconTextGap (int gap)
    {
        _label.setIconTextGap(gap);
    }

    /**
     * Returns the gap between the icon and the text.
     */
    public int getIconTextGap ()
    {
        return _label.getIconTextGap();
    }

    /**
     * Sets the rotation for the text (in ninety degree increments).
     */
    public void setTextRotation (int rotation)
    {
        _label.setTextRotation(rotation);
    }

    /**
     * Sets the orientation of this label with respect to its icon. If the
     * horizontal (the default) the text is displayed to the right of the icon,
     * if vertical the text is displayed below it.
     */
    public void setOrientation (int orient)
    {
        _label.setOrientation(orient);
    }

    /**
     * Configures whether this label will wrap, truncate or scale if it cannot
     * fit text into its allotted width. The default is to wrap.
     */
    public void setFit (Fit mode)
    {
        _label.setFit(mode);
    }

    /**
     * Returns the current fit mode for this label.
     */
    public Fit getFit ()
    {
        return _label._fit;
    }

    /**
     * Configures the preferred width of this label (the preferred height will be calculated
     * from the font).
     */
    public void setPreferredWidth (int width)
    {
        _label.setPreferredWidth(width);
    }

    /**
     * Returns a reference to teh label's renderer.
     */
    public LabelRenderer getLabelRenderer ()
    {
        return _label;
    }

    // documentation inherited
    public void setText (String text)
    {
        _label.setText(text);
    }

    // documentation inherited
    public String getText ()
    {
        return _label.getText();
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/Label";
    }

    // documentation inherited
    protected void layout ()
    {
        super.layout();
        _label.layout(getInsets(), getWidth(), getHeight());
    }

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);
        _label.render(renderer, 0, 0, getWidth(), getHeight(), _alpha);
    }

    // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return _label.computePreferredSize(whint, hhint);
    }

    protected LabelRenderer _label;
}
