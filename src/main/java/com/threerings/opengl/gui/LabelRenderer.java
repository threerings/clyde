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

import org.lwjgl.opengl.GL11;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.text.TextFactory;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Handles the underlying layout and rendering for {@link Label} and {@link Button}.
 */
public class LabelRenderer
    implements UIConstants
{
    public LabelRenderer (TextComponent container)
    {
        _container = container;
    }

    /**
     * Updates the text displayed by this label.
     */
    public void setText (String text)
    {
        if (_value != null && _value.equals(text)) {
            return;
        }
        _value = text;

        // our size may have changed so we need to revalidate
        _container.invalidate();
    }

    /**
     * Returns the text currently being displayed by this label.
     */
    public String getText ()
    {
        return _value;
    }

    /**
     * Configures the label to display the specified icon.
     */
    public void setIcon (Icon icon)
    {
        if (_icon == icon) {
            return;
        }
        int owidth = 0, oheight = 0, nwidth = 0, nheight = 0;
        if (_icon != null) {
            owidth = _icon.getWidth();
            oheight = _icon.getHeight();
        }

        _icon = icon;
        if (_icon != null) {
            nwidth = _icon.getWidth();
            nheight = _icon.getHeight();
        }

        if (owidth != nwidth || oheight != nheight) {
            // reset our config so that we force a text reflow to account for the changed icon size
            _container.invalidate();
        } else if (_container.isValid()) {
            _container.layout();
        }
    }

    /**
     * Returns the icon being displayed by this label.
     */
    public Icon getIcon ()
    {
        return _icon;
    }

    /**
     * Configures the gap between the icon and the text.
     */
    public void setIconTextGap (int gap)
    {
        _gap = gap;
    }

    /**
     * Returns the gap between the icon and the text.
     */
    public int getIconTextGap ()
    {
        return _gap;
    }

    /**
     * Sets the rotation for the text (in ninety degree increments).
     */
    public void setTextRotation (int rotation)
    {
        _textRotation = rotation;
    }

    /**
     * Sets the orientation of this label with respect to its icon. If the horizontal (the default)
     * the text is displayed to the right of the icon, if vertical the text is displayed below it.
     */
    public void setOrientation (int orient)
    {
        _orient = orient;
    }

    /**
     * Configures whether this label will wrap, truncate or scale if it cannot fit text into its
     * allotted width. The default is to wrap.
     */
    public void setFit (Label.Fit mode)
    {
        _fit = mode;
    }

    /**
     * Configures the preferred width of this label (the preferred height will be calculated
     * from the font).
     */
    public void setPreferredWidth (int width)
    {
        _prefWidth = width;
    }

    /**
     * Configures the line fade time if you want the text to fade in.
     */
    public void setLineFadeTime (int lineFadeTime)
    {
        _fade.lineFadeTime = lineFadeTime;
        _fade.elapsed = 0;
    }

    /**
     * Tick the renderer.
     */
    public void tick (int elapsed)
    {
        _fade.elapsed += elapsed;
    }

    /**
     * Computes the preferred size of the label.
     */
    public Dimension computePreferredSize (int whint, int hhint)
    {
        // if our cached preferred size is not valid, recompute it
        int hint = getWidth(whint, hhint, _textRotation);
        if (_prefWidth > 0) {
            // our preferred width overrides any hint
            hint = _prefWidth;
        } else if (hint <= 0) {
            // if we have no hints and no preferred width, allow arbitrarily wide lines
            hint = Short.MAX_VALUE-1;
        }
        Config prefconfig = layoutConfig(_prefconfig, hint);
        _prefsize = computeSize(_prefconfig = prefconfig);
        prefconfig.glyphs = null; // we don't need to retain these
        return new Dimension(_prefsize);
    }

    /**
     * Lays out the label text and icon.
     */
    public void layout (Insets insets, int contWidth, int contHeight)
    {
        // compute any offsets needed to center or align things
        Config config = layoutConfig(_config, getWidth(
            contWidth - insets.getHorizontal(),
            contHeight - insets.getVertical(), _textRotation));
        Dimension size = computeSize(config);
        int xoff = 0, yoff = 0;
        switch (_orient) {
        case HORIZONTAL:
            if (_icon != null) {
                _ix = getXOffset(insets, contWidth, size.width);
                _iy = getYOffset(insets, contHeight, _icon.getHeight());
                xoff = (_icon.getWidth() + _gap);
            }
            if (config.glyphs != null) {
                _tx = getXOffset(insets, contWidth, size.width) + xoff;
                _ty = getYOffset(insets, contHeight, config.glyphs.getHeight(_textRotation));
            }
            break;

        case VERTICAL:
            if (config.glyphs != null) {
                _tx = getXOffset(insets, contWidth, config.glyphs.getWidth(_textRotation));
                _ty = getYOffset(insets, contHeight, size.height);
                yoff = (config.glyphs.getHeight(_textRotation) + _gap);
            }
            if (_icon != null) {
                _ix = getXOffset(insets, contWidth, _icon.getWidth());
                _iy = getYOffset(insets, contHeight, size.height) + yoff;
            }
            break;

        case OVERLAPPING:
            if (_icon != null) {
                _ix = getXOffset(insets, contWidth, _icon.getWidth());
                _iy = getYOffset(insets, contHeight, _icon.getHeight());
            }
            if (config.glyphs != null) {
                _tx = getXOffset(insets, contWidth, config.glyphs.getWidth(_textRotation));
                _ty = getYOffset(insets, contHeight, config.glyphs.getHeight(_textRotation));
            }
            break;
        }

        useConfig(config);
    }

    /**
     * Returns the x coordinate of the icon.
     */
    public int getIconX ()
    {
        return _ix;
    }

    /**
     * Returns the y coordinate of the icon.
     */
    public int getIconY ()
    {
        return _iy;
    }

    /**
     * Renders the label text and icon.
     */
    public void render (Renderer renderer, int x, int y, int contWidth, int contHeight, float alpha)
    {
        GL11.glTranslatef(x, y, 0);
        try {
            if (_icon != null) {
                _icon.render(renderer, _ix, _iy, alpha);
            }
            if (_config != null && _config.glyphs != null) {
                Dimension size = _config.glyphs.size;
                int ox = getOffsetX(size.width, size.height, _textRotation) + _tx;
                int oy = getOffsetY(size.width, size.height, _textRotation) + _ty;
                GL11.glTranslatef(ox, oy, 0);
                GL11.glRotatef(_textRotation * 90, 0, 0, 1);
                try {
                    renderText(renderer, contWidth, contHeight, alpha);
                } finally {
                    GL11.glRotatef(_textRotation * -90, 0, 0, 1);
                    GL11.glTranslatef(-ox, -oy, 0);
                }
            }
        } finally {
            GL11.glTranslatef(-x, -y, 0);
        }
    }

    protected void renderText (Renderer renderer, int contWidth, int contHeight, float alpha)
    {
        if (_fit == Label.Fit.WRAP) {
            _config.glyphs.render(
                renderer, 0, 0, _container.getHorizontalAlignment(), alpha, _config.spacing);
            return;
        }

        Insets insets = _container.getInsets();
        int width = contWidth - insets.getHorizontal();
        int height = contHeight - insets.getVertical();
        if (width <= 0 || height <= 0) {
            return;
        }

        if (_fit == Label.Fit.SCALE) {
            _config.glyphs.render(renderer, 0, 0,
                getWidth(width, height, _textRotation),
                getHeight(width, height, _textRotation),
                _container.getHorizontalAlignment(), alpha);
            return;
        }

        Rectangle oscissor = Component.intersectScissor(
            renderer, _srect,
            _container.getAbsoluteX() + insets.left,
            _container.getAbsoluteY() + insets.bottom,
            width, height);
        try {
            _config.glyphs.render(
                renderer, 0, 0, _container.getHorizontalAlignment(), alpha, _config.spacing);
        } finally {
            renderer.setScissor(oscissor);
        }
    }

    protected Dimension computeSize (Config config)
    {
        int iwidth = 0, iheight = 0, twidth = 0, theight = 0, gap = 0;
        if (_icon != null) {
            iwidth = _icon.getWidth();
            iheight = _icon.getHeight();
        }
        if (config.glyphs != null) {
            if (_icon != null) {
                gap = _gap;
            }
            twidth = config.glyphs.getWidth(_textRotation);
            theight = config.glyphs.getHeight(_textRotation);
        }

        int width, height;
        switch (_orient) {
        default:
        case HORIZONTAL:
            width = iwidth + gap + twidth;
            height = Math.max(iheight, theight);
            break;
        case VERTICAL:
            width = Math.max(iwidth, twidth);
            height = iheight + gap + theight;
            break;
        case OVERLAPPING:
            width = Math.max(iwidth, twidth);
            height = Math.max(iheight, theight);
            break;
        }

        return new Dimension(width, height);
    }

    protected int getXOffset (Insets insets, int contWidth, int width)
    {
        switch (_container.getHorizontalAlignment()) {
        default:
        case LEFT: return insets.left;
        case RIGHT: return contWidth - width - insets.right;
        case CENTER: return (contWidth - insets.getHorizontal() - width) / 2 + insets.left;
        }
    }

    protected int getYOffset (Insets insets, int contHeight, int height)
    {
        switch (_container.getVerticalAlignment()) {
        default:
        case TOP: return contHeight - height - insets.top;
        case BOTTOM: return insets.bottom;
        case CENTER: return (contHeight - insets.getVertical() - height) / 2 + insets.bottom;
        }
    }

    /**
     * Creates glyphs for the current text at the specified target width.
     */
    protected Config layoutConfig (Config oconfig, int twidth)
    {
        // if we're not wrapping, force our target width
        if (_fit != Label.Fit.WRAP) {
            twidth = Short.MAX_VALUE-1;
        }

        if (_value != null && _icon != null) {
            // account for the space taken up by the icon
            if ((_textRotation & 0x01) == 0) {
                if (_orient == HORIZONTAL) {
                    twidth -= _gap;
                    twidth -= _icon.getWidth();
                }
            } else {
                if (_orient == VERTICAL) {
                    twidth -= _gap;
                    twidth -= _icon.getHeight();
                }
            }
        }

        // no need to recreate our glyphs if our config hasn't changed
        Config config = _container.getLabelRendererConfig(this, twidth);
        if (oconfig != null && oconfig.glyphs != null && oconfig.matches(config, twidth)) {
            return oconfig;
        }

        // if we have no text, we're done
        if (_value == null || _value.equals("")) {
            return config;
        }

        // sanity check
        if (twidth < 0) {
            Log.log.warning("Requested to layout with negative target width [text=" + _value +
                            ", twidth=" + twidth + "].", new Exception());
            return config;
        }

        // render up some new text
        TextFactory tfact = _container.getTextFactory(this);
        Glyphs glyphs = new Glyphs();
        glyphs.fade = _fade;
        glyphs.lines = tfact.wrapText(
            _value, config.color, config.effect, config.effectSize, config.effectColor, twidth);
        for (int ii = 0; ii < glyphs.lines.length; ii++) {
            glyphs.size.width = Math.max(glyphs.size.width, glyphs.lines[ii].getSize().width);
            glyphs.size.height += glyphs.lines[ii].getSize().height + (ii > 0 ? config.spacing : 0);
        }
        config.glyphs = glyphs;

        // if our old config is the same number of lines as our new config, expand the width region
        // that this configuration will match
        if (oconfig != null && oconfig.glyphs != null &&
            oconfig.glyphs.lines.length == config.glyphs.lines.length) {
            config.minwidth = Math.min(config.minwidth, oconfig.minwidth);
            config.maxwidth = Math.max(config.maxwidth, oconfig.maxwidth);
        }

        return config;
    }

    protected void useConfig (Config config)
    {
        // make sure it's not the one we're already using
        if (_config == config) {
            return;
        }

        // note our new config
        _config = config;
    }

    /**
     * Returns the width under the supplied rotation.
     */
    protected static int getWidth (int width, int height, int rotation)
    {
        return ((rotation & 0x01) == 0) ? width : height;
    }

    /**
     * Returns the height under the supplied rotation.
     */
    protected static int getHeight (int width, int height, int rotation)
    {
        return ((rotation & 0x01) == 0) ? height : width;
    }

    /**
     * Returns the x offset for rotating the supplied dimensions by the specified amount.
     */
    protected static int getOffsetX (int width, int height, int rotation)
    {
        switch (rotation & 0x03) {
            default: case 0: return 0;
            case 1: return height;
            case 2: return width;
            case 3: return 0;
        }
    }

    /**
     * Returns the y offset for rotation the supplied dimensions by the specified amount.
     */
    protected static int getOffsetY (int width, int height, int rotation)
    {
        switch (rotation & 0x03) {
            default: case 0: return 0;
            case 1: return 0;
            case 2: return height;
            case 3: return width;
        }
    }

    protected static class Config
    {
        public String text;
        public Color4f color;
        public int effect;
        public int effectSize;
        public Color4f effectColor;
        public int minwidth, maxwidth;
        public Glyphs glyphs;
        public int spacing;

        public boolean matches (Config other, int twidth) {
            if (other == null) {
                return false;
            }

            // if any of the styles are different, we don't match
            if (effect != other.effect) {
                return false;
            }
            if (text != other.text && (text == null || !text.equals(other.text))) {
                return false;
            }
            if (!color.equals(other.color)) {
                return false;
            }
            if (effectColor != other.effectColor &&
                (effectColor == null || !effectColor.equals(other.effectColor))) {
                return false;
            }
            if (effectSize != other.effectSize || spacing != other.spacing) {
                return false;
            }

            // if we are only one line we are fine as long as we're less than or equal to the
            // target width (only if it is smaller than our minwidth might it cause us to wrap)
            if (glyphs != null && glyphs.lines.length == 1 && minwidth <= twidth) {
                return true;
            }

            // otherwise the new target width has to fall within our handled range
            return (minwidth <= twidth) && (twidth <= maxwidth);
        }

        public String toString () {
            return text + "(" + toString(color) + "," + effect + "," + toString(effectColor) + "," +
                minwidth + "<>" + maxwidth + ")";
        }

        protected String toString (Color4f color) {
            return color == null ? "null" : Integer.toHexString(color.hashCode());
        }
    }

    protected static class Glyphs
    {
        public Text[] lines;
        public Dimension size = new Dimension();
        public Fade fade;

        public int getWidth (int rotation)
        {
            return LabelRenderer.getWidth(size.width, size.height, rotation);
        }

        public int getHeight (int rotation)
        {
            return LabelRenderer.getHeight(size.width, size.height, rotation);
        }

        public void render (Renderer renderer, int tx, int ty, int halign,
                            float alpha, int spacing) {
            // render the lines from the bottom up
            for (int ii = lines.length-1; ii >= 0; ii--) {
                int lx = tx;
                if (halign == RIGHT) {
                    lx += size.width - lines[ii].getSize().width;
                } else if (halign == CENTER) {
                    lx += (size.width - lines[ii].getSize().width)/2;
                }
                float a = alpha;
                if (fade.lineFadeTime > 0) {
                    int start = ii * fade.lineFadeTime / 2;
                    if (start < fade.elapsed) {
                        a *= Math.min(1f, (float)(fade.elapsed - start) / fade.lineFadeTime);
                    } else {
                        a = 0f;
                    }
                }
                lines[ii].render(renderer, lx, ty, a);
                ty += lines[ii].getSize().height + (ii > 0 ? spacing : 0);
            }
        }

        public void render (Renderer renderer, int tx, int ty,
                int width, int height, int halign, float alpha) {
            // render only the first line
            float scale = 1f;
            if (size.width > width) {
                scale = (float)width/size.width;
            }
            if (size.height > height) {
                scale = Math.min(scale, (float)height/size.height);
            }
            width = (int)(size.width * scale);
            height = (int)(size.height * scale);
            if (height < size.height) {
                ty += (size.height - height)/2;
            }
            if (halign == RIGHT) {
                tx += size.width - width;
            } else if (halign == CENTER) {
                tx += (size.width - width)/2;
            }
            lines[0].render(renderer, tx, ty, width, height, alpha);
        }
    }

    protected static class Fade
    {
        public int lineFadeTime;
        public int elapsed;
    }

    protected TextComponent _container;
    protected String _value;

    protected int _textRotation, _orient = HORIZONTAL;
    protected int _gap = 3;
    protected Label.Fit _fit = Label.Fit.WRAP;

    protected Icon _icon;
    protected int _ix, _iy;

    protected Config _config;
    protected int _tx, _ty;
    protected float _alpha = 1f;

    protected Config _prefconfig;
    protected Dimension _prefsize;

    protected int _prefWidth = -1;

    protected Fade _fade = new Fade();

    protected Rectangle _srect = new Rectangle();
}
