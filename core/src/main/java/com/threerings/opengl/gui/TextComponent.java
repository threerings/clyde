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

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.config.FontConfig;
import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.text.TextFactory;

/**
 * Defines methods and mechanisms common to components that render a string of
 * text.
 */
public abstract class TextComponent extends Component
{
    /**
     * Creates a new text component.
     */
    public TextComponent (GlContext ctx)
    {
        super(ctx);
    }

    /**
     * Updates the text displayed by this component.
     */
    public abstract void setText (String text);

    /**
     * Returns the text currently being displayed by this component.
     */
    public abstract String getText ();

    /**
     * Return the "value" of the TextComponent.
     *
     * By default will return a String equal to {@code #getText}, but EditableTextComponents
     * will return the value from their Document, which may not even be a String.
     */
    public Object getValue ()
    {
        return getText();
    }

    /**
     * Returns a text factory suitable for creating text in the style defined
     * by the component's current state.
     */
    public TextFactory getTextFactory ()
    {
        TextFactory textfact = _textfacts[getState()];
        return (textfact != null) ? textfact : _textfacts[DEFAULT];
    }

    /**
     * Returns the horizontal alignment for this component's text.
     */
    public int getHorizontalAlignment ()
    {
        if (_haligns != null) {
            int halign = _haligns[getState()];
            return (halign != -1) ? halign : _haligns[DEFAULT];
        }
        return UIConstants.LEFT;
    }

    /**
     * Returns the vertical alignment for this component's text.
     */
    public int getVerticalAlignment ()
    {
        if (_valigns != null) {
            int valign = _valigns[getState()];
            return (valign != -1) ? valign : _valigns[DEFAULT];
        }
        return UIConstants.CENTER;
    }

    /**
     * Returns the effect for this component's text.
     */
    public int getTextEffect ()
    {
        if (_teffects != null) {
            int teffect = _teffects[getState()];
            return (teffect != -1) ? teffect : _teffects[DEFAULT];
        }
        return UIConstants.NORMAL;
    }

    /**
     * Returns the effect size for this component's text.
     */
    public int getEffectSize ()
    {
        if (_effsizes != null) {
            int effsize = _effsizes[getState()];
            return (effsize > 0) ? effsize : _effsizes[DEFAULT];
        }
        return UIConstants.DEFAULT_SIZE;
    }

    /**
     * Returns the color to use for our text effect.
     */
    public Color4f getEffectColor ()
    {
        if (_effcols != null) {
            Color4f effcol = _effcols[getState()];
            return (effcol != null) ? effcol : _effcols[DEFAULT];
        }
        return Color4f.WHITE;
    }

    /**
     * Returns the line spacing for our text.
     */
    public int getLineSpacing ()
    {
        if (_lineSpacings != null) {
            return _lineSpacings[getState()];
        }
        return UIConstants.DEFAULT_SPACING;
    }

    @Override
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);

        _haligns[state] = config.textAlignment.getConstant();
        _valigns[state] = config.verticalAlignment.getConstant();
        _teffects[state] = config.textEffect.getConstant();
        _effsizes[state] = config.effectSize;
        _effcols[state] = config.effectColor;

        FontConfig fconfig = _ctx.getConfigManager().getConfig(
            FontConfig.class, config.font);
        if (fconfig == null) {
            fconfig = FontConfig.NULL;
        }
        _textfacts[state] = fconfig.getTextFactory(_ctx, config.fontStyle, config.fontSize);

        _lineSpacings[state] = fconfig.adjustSpacing(config.lineSpacing);
    }

    /**
     * Returns the text factory that should be used by the supplied label renderer (for which we
     * are by definition acting as container) to generate its text.
     */
    protected TextFactory getTextFactory (LabelRenderer forLabel)
    {
        return getTextFactory();
    }

    /**
     * Creates a text configuration for the supplied label renderer (for which we are by definition
     * acting as container).
     */
    protected LabelRenderer.Config getLabelRendererConfig (LabelRenderer forLabel, int twidth)
    {
        LabelRenderer.Config config = new LabelRenderer.Config();
        config.text = forLabel.getText();
        config.color = getColor();
        config.effect = getTextEffect();
        config.effectSize = getEffectSize();
        config.effectColor = getEffectColor();
        config.spacing = getLineSpacing();
        config.minwidth = config.maxwidth = twidth;
        return config;
    }

    protected int[] _haligns = new int[getStateCount()];
    protected int[] _valigns = new int[getStateCount()];
    protected int[] _teffects = new int[getStateCount()];
    protected int[] _effsizes = new int[getStateCount()];
    protected int[] _lineSpacings = new int[getStateCount()];
    protected Color4f[] _effcols = new Color4f[getStateCount()];
    protected TextFactory[] _textfacts = new TextFactory[getStateCount()];
}
