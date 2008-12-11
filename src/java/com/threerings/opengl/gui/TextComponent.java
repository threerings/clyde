//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.GlContext;

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

    // documentation inherited
    protected void configureStyle (StyleSheet style)
    {
        super.configureStyle(style);

        int[] haligns = new int[getStateCount()];
        for (int ii = 0; ii < getStateCount(); ii++) {
            haligns[ii] = style.getTextAlignment(this, getStatePseudoClass(ii));
        }
        _haligns = checkNonDefault(haligns, UIConstants.LEFT);

        int[] valigns = new int[getStateCount()];
        for (int ii = 0; ii < getStateCount(); ii++) {
            valigns[ii] = style.getVerticalAlignment(
                this, getStatePseudoClass(ii));
        }
        _valigns = checkNonDefault(valigns, UIConstants.CENTER);

        int[] teffects = new int[getStateCount()];
        for (int ii = 0; ii < getStateCount(); ii++) {
            teffects[ii] = style.getTextEffect(this, getStatePseudoClass(ii));
        }
        _teffects = checkNonDefault(teffects, UIConstants.NORMAL);

        int[] effsizes = new int[getStateCount()];
        for (int ii = 0; ii < getStateCount(); ii++) {
            effsizes[ii] = style.getEffectSize(this, getStatePseudoClass(ii));
        }
        _effsizes = checkNonDefault(effsizes, UIConstants.DEFAULT_SIZE);

        int[] lineSpacings = new int[getStateCount()];
        for (int ii = 0; ii < getStateCount(); ii++) {
            lineSpacings[ii] = style.getLineSpacing(this, getStatePseudoClass(ii));
        }
        _lineSpacings = checkNonDefaultInt(lineSpacings, UIConstants.DEFAULT_SPACING);

        Color4f[] effcols = new Color4f[getStateCount()];
        boolean nondef = false;
        for (int ii = 0; ii < getStateCount(); ii++) {
            effcols[ii] = style.getEffectColor(this, getStatePseudoClass(ii));
            nondef = nondef || (effcols[ii] != null);
            _textfacts[ii] =
                style.getTextFactory(this, getStatePseudoClass(ii));
        }
        if (nondef) {
            _effcols = effcols;
        }
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

    protected int[] checkNonDefault (int[] styles, int defval)
    {
        return checkNonDefaultVal(styles, defval, -1);
    }

    protected int[] checkNonDefaultInt (int[] styles, int defval)
    {
        return checkNonDefaultVal(styles, defval, defval);
    }

    protected int[] checkNonDefaultVal (int[] styles, int defval1, int defval2)
    {
        for (int ii = 0; ii < styles.length; ii++) {
            if (styles[ii] != defval1 && styles[ii] != defval2) {
                return styles;
            }
        }
        return null;
    }

    protected int[] _haligns;
    protected int[] _valigns;
    protected int[] _teffects;
    protected int[] _effsizes;
    protected int[] _lineSpacings;
    protected Color4f[] _effcols;
    protected TextFactory[] _textfacts = new TextFactory[getStateCount()];
}
