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

package com.threerings.opengl.gui.text;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;
import com.google.common.base.Objects;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntTuple;
import com.samskivert.util.ObjectUtil;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.TextureUnit;

import com.threerings.opengl.gui.UIConstants;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Formats text by rendering individual characters into a set of shared textures, then returning
 * {@link Text} instances that render groups of quads, one for each character.
 */
public class CharacterTextFactory extends TextFactory
    implements UIConstants
{
    /**
     * Returns a shared factory instance.
     */
    public static CharacterTextFactory getInstance (
            Font font, boolean antialias, float descentModifier)
    {
        return getInstance(font, antialias, descentModifier, 0);
    }

    public static CharacterTextFactory getInstance (
            Font font, boolean antialias, float descentModifier, int heightModifier)
    {
        FactoryKey key = new FactoryKey(font, antialias, descentModifier, heightModifier);
        CharacterTextFactory factory = _instances.get(key);
        if (factory == null) {
            _instances.put(
                    key, factory = new CharacterTextFactory(font, antialias, descentModifier, heightModifier));
        }
        return factory;
    }

    /**
     * Creates a character text factory with the supplied font.
     */
    public CharacterTextFactory (Font font, boolean antialias, float descentModifier , int heightModifier)
    {
        _font = font;

        // we need a graphics context to retrieve the metrics
        _scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        _graphics = _scratch.createGraphics();
        _metrics = _graphics.getFontMetrics(font);
        _descentOffset = Math.round(_metrics.getHeight() * descentModifier);
        _heightModifier = heightModifier;

        // create a test glyph to determine the size
        FontRenderContext ctx = _graphics.getFontRenderContext();
        GlyphVector vector = _font.createGlyphVector(ctx, "J");
        java.awt.Rectangle bounds = vector.getPixelBounds(ctx, 0f, 0f);

        // allow up to four times the sample dimensions for descenders, effects, etc.
        _scratch = new BufferedImage(bounds.width*4, bounds.height*4, BufferedImage.TYPE_INT_ARGB);
        _graphics.dispose();
        _graphics = _scratch.createGraphics();
        _graphics.setFont(font);
        _graphics.setBackground(new Color(0, true));
        _graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            antialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        _graphics.setRenderingHint(
            RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    }

    /**
     * Renders a string directly, without creating a text object.
     */
    public void render (Renderer renderer, String text, int x, int y, Color4f color)
    {
        for (int ii = 0, nn = text.length(); ii < nn; ii++) {
            Glyph glyph = getGlyph(text.charAt(ii));
            glyph.render(renderer, x, y);
            x += glyph.width;
        }
    }

    @Override
    public int getHeight ()
    {
        return _metrics.getHeight() + _heightModifier;
    }

    @Override
    public Text createText (
        final String text, final Color4f color, final int effect,
        final int effectSize, final Color4f effectColor, boolean useAdvance)
    {
        // get/create glyphs
        final Glyph[] glyphs = new Glyph[text.length()];
        int width = 0;
        for (int ii = 0; ii < glyphs.length; ii++) {
            glyphs[ii] = getGlyph(text.charAt(ii));
            width += glyphs[ii].width;
        }

        final Dimension size = new Dimension(width, getHeight());

        // and outlines, if necessary
        final Glyph[] outlines = (effect == OUTLINE) ? new Glyph[text.length()] : null;
        if (effect == OUTLINE) {
            for (int ii = 0; ii < outlines.length; ii++) {
                outlines[ii] = getGlyph(text.charAt(ii), OUTLINE, effectSize);
            }
        }

        return new Text() {
            public int getLength () {
                return glyphs.length;
            }
            public Dimension getSize () {
                return size;
            }
            public int getHitPos (int x, int y) {
                int tx = 0;
                for (int ii = 0; ii < glyphs.length; ii++) {
                    int hwidth = glyphs[ii].width/2;
                    tx += hwidth;
                    if (x < tx) {
                        return ii;
                    }
                    tx += (glyphs[ii].width - hwidth);
                }
                return glyphs.length;
            }
            public int getCursorPos (int index) {
                int x = 0;
                for (int ii = 0, nn = Math.min(index, glyphs.length); ii < nn; ii++) {
                    x += glyphs[ii].width;
                }
                return x;
            }
            public void render (Renderer renderer, int x, int y, float alpha) {
                // add the descent above the baseline
                y += _metrics.getDescent() + _descentOffset;

                // multi-pixel outlines go below the character
                if (outlines != null && effectSize > 1) {
                    renderGlyphs(renderer, outlines, effectColor, x, y, alpha);
                }
                // as do shadows
                if (effect == SHADOW) {
                    renderGlyphs(
                        renderer, glyphs, effectColor, x + effectSize - 1, y - effectSize, alpha);
                    x += 1;
                }

                // now draw the characters
                renderGlyphs(renderer, glyphs, color, x, y, alpha);

                // single-pixel outlines go on top of the character
                if (outlines != null && effectSize == 1) {
                    renderGlyphs(renderer, outlines, effectColor, x, y, alpha);
                }
            }
            protected void renderGlyphs (
                Renderer renderer, Glyph[] glyphs, Color4f color, int x, int y, float alpha) {
                float a = color.a * alpha;
                renderer.setColorState(color.r * a, color.g * a, color.b * a, a);
                for (Glyph glyph : glyphs) {
                    glyph.render(renderer, x, y);
                    x += glyph.width;
                }
            }
        };
    }

    @Override
    public Text[] wrapText (String text, Color4f color, int effect, int effectSize,
                            Color4f effectColor, int maxWidth)
    {
        ArrayList<Text> lines = new ArrayList<Text>();
        StringBuilder line = new StringBuilder();
        int width = 0;
        for (int ii = 0, nn = text.length(); ii < nn; ii++) {
            char c = text.charAt(ii);
            Glyph glyph = getGlyph(c);
            if (c == '\n' || width + glyph.width > maxWidth) {
                String extra = "";
                if (c != '\n' && c != ' ') {
                    // scan backwards, see if we can break on a space
                    line.append(c);
                    IntTuple bspan = getBreakSpan(line);
                    if (bspan != null) {
                        extra = line.substring(bspan.right, line.length());
                        line.delete(bspan.left, line.length());
                    } else {
                        extra = String.valueOf(c);
                        line.deleteCharAt(line.length() - 1);
                    }
                }
                lines.add(createText(
                    line.toString(), color, effect, effectSize, effectColor, true));
                line.setLength(0);
                line.append(extra);
                width = 0;
                for (int jj = 0, ll = extra.length(); jj < ll; jj++) {
                    width += getGlyph(extra.charAt(jj)).width;
                }
            } else {
                line.append(c);
                width += glyph.width;
            }
        }
        // add the final line
        lines.add(createText(line.toString(), color, effect, effectSize, effectColor, true));
        return lines.toArray(new Text[lines.size()]);
    }

    /**
     * Returns the normal glyph for the given character.
     */
    protected Glyph getGlyph (char c)
    {
        return getGlyph(c, NORMAL, 0);
    }

    /**
     * Returns the glyph for the given character with the given effect and effect size.
     */
    protected Glyph getGlyph (char c, int effect, int size)
    {
        // the key combines the character with the effect and size
        int key = (size << 20) | (effect << 16) | c;
        Glyph glyph = _glyphs.get(key);
        if (glyph == null) {
            _glyphs.put(key, glyph = new Glyph(c, effect, size));
        }
        return glyph;
    }

    /**
     * Searches for an appropriate break span: the region of characters that may be omitted prior
     * to the region to be pushed to the next line. Typically this is the last region of whitespace.
     * The region may be zero-length to indicate that no characters should be removed.
     *
     * "foo[ ]bar" (break after foo, cut the space, and put bar on the next line)
     * "foo-[]bar" (break after the dash, put bar on the next line)
     *
     * @return the start (inclusive) and end (exclusive) indices of the span,
     * or <code>null</code> if no span was found.
     */
    protected IntTuple getBreakSpan (StringBuilder buf)
    {
        for (int ii = buf.length() - 2; ii > 0; ii--) {
            char c = buf.charAt(ii);
            if (Character.isWhitespace(c)) {
                for (int jj = ii - 1; jj >= 0; jj--) {
                    if (!Character.isWhitespace(buf.charAt(jj))) {
                        return new IntTuple(jj + 1, ii + 1);
                    }
                }
                return null; // no non-whitespace before whitespace

            } else if (isBreakChar(c) && (!Character.isWhitespace(buf.charAt(ii - 1)))) {
                return new IntTuple(ii + 1, ii + 1);
            }
        }
        return null; // no whitespace
    }

    /**
     * Returns true if the character is a valid break character.
     */
    protected boolean isBreakChar (char c)
    {
        return '-' == c || (c >= 0x4E00 && c <= 0x9FFF);
    }

    /**
     * Inserts the glyph image in the scratch pad into the current texture (creating a new
     * texture if there is no current texture or the current texture doesn't have enough
     * room), returns the texture unit data, and populates the supplied array with the
     * texture coordinates.
     */
    protected TextureUnit[] addGlyphToTexture (
        Renderer renderer, int width, int height, float[] tcoords)
    {
        // make sure the width and height don't exceed the borders of the scratchpad
        width = Math.min(Math.max(width, 0), _scratch.getWidth());
        height = Math.min(Math.max(height, 0), _scratch.getHeight());

        // try to add to the current texture; if there's not enough room, create a new one
        TextureUnit[] units = (_texture == null) ? null : _texture.add(width, height, tcoords);
        if (units == null) {
            _texture = new GlyphTexture(renderer);
            units = _texture.add(width, height, tcoords);
        }
        return units;
    }

    /**
     * A single glyph.
     */
    protected class Glyph
    {
        /** The advance width of this glyph. */
        public int width;

        public Glyph (char c, int effect, int size)
        {
            width = _metrics.charWidth(_c = c);
            _effect = effect;
            _size = size;
            FontRenderContext ctx = _graphics.getFontRenderContext();
            _vector = _font.createGlyphVector(ctx, Character.toString(c));
            java.awt.Rectangle bounds = _vector.getPixelBounds(ctx, 0f, 0f);
            if (bounds.width > 0 && bounds.height > 0) {
                _bounds = new Rectangle(
                    bounds.x, -bounds.y - bounds.height, bounds.width, bounds.height);
                int grow = 1 + (_effect == OUTLINE ? Math.round(size/2f) : 0);
                _bounds.grow(grow, grow);
            }
        }

        /**
         * Renders this glyph at the specified position.
         */
        public void render (Renderer renderer, int x, int y)
        {
            if (_units == null) {
                if (_bounds == null) {
                    return; // whitespace
                }
                // render the glyph to the scratch image
                _graphics.clearRect(0, 0, _scratch.getWidth(), _scratch.getHeight());
                Shape outline = _vector.getOutline(-_bounds.x, _bounds.y + _bounds.height);
                if (_effect == OUTLINE) {
                    _graphics.setStroke(new BasicStroke(
                        _size, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
                    _graphics.draw(outline);
                } else {
                    _graphics.fill(outline);
                }
                float[] tcoords = new float[4];
                _units = addGlyphToTexture(renderer, _bounds.width, _bounds.height, tcoords);
                _s1 = tcoords[0];
                _t1 = tcoords[1];
                _s2 = tcoords[2];
                _t2 = tcoords[3];
                _vector = null;
            }
            int lx = x + _bounds.x;
            int ly = y + _bounds.y;
            int ux = lx + _bounds.width;
            int uy = ly + _bounds.height;

            renderer.setTextureState(_units);
            renderer.setMatrixMode(GL11.GL_MODELVIEW);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(_s1, _t1);
            GL11.glVertex2f(lx, ly);
            GL11.glTexCoord2f(_s2, _t1);
            GL11.glVertex2f(ux, ly);
            GL11.glTexCoord2f(_s2, _t2);
            GL11.glVertex2f(ux, uy);
            GL11.glTexCoord2f(_s1, _t2);
            GL11.glVertex2f(lx, uy);
            GL11.glEnd();
        }

        /** The glyph character. */
        protected char _c;

        /** The effect and effect size. */
        protected int _effect, _size;

        /** Stores the glyph vector. */
        protected GlyphVector _vector;

        /** The glyph bounds. */
        protected Rectangle _bounds;

        /** The glyph texture units. */
        protected TextureUnit[] _units;

        /** The texture coordinates of the glyph. */
        protected float _s1, _t1, _s2, _t2;
    }

    /**
     * A shared texture.
     */
    protected class GlyphTexture
    {
        public GlyphTexture (Renderer renderer)
        {
            _texture = new Texture2D(renderer);
            _texture.setImage(GL11.GL_RGBA, TEXTURE_SIZE, TEXTURE_SIZE, false, false);
            _texture.setFilters(GL11.GL_LINEAR, GL11.GL_NEAREST);
            _units = new TextureUnit[] { new TextureUnit(_texture) };
        }

        /**
         * Attempts to copy the glyph in the scratch image into this texture.
         */
        public TextureUnit[] add (int width, int height, float[] tcoords)
        {
            // move up to the next row if necessary
            if (_x + width > TEXTURE_SIZE) {
                _y += _height;
                _x = 0;
                _height = 0;
            }
            if (_y + height > TEXTURE_SIZE) {
                return null; // out of room in this texture
            }

            // copy the scratch image into the texture
            _texture.setSubimage(
                _scratch.getSubimage(0, 0, width, height), true, _x, _y, width, height);

            // set the texture coordinates
            tcoords[0] = (float)_x / TEXTURE_SIZE;
            tcoords[1] = (float)_y / TEXTURE_SIZE;
            tcoords[2] = (float)(_x + width) / TEXTURE_SIZE;
            tcoords[3] = (float)(_y + height) / TEXTURE_SIZE;

            // advance to the next position
            _x += width;
            _height = Math.max(_height, height);

            // return the texture units
            return _units;
        }

        /** The shared texture unit array. */
        protected TextureUnit[] _units;

        /** The casted texture. */
        protected Texture2D _texture;

        /** The current x and y position within the texture. */
        protected int _x, _y;

        /** The height of the current row. */
        protected int _height;
    }

    protected static class FactoryKey
    {
        public Font font;

        public boolean antialias;

        public float descentModifier;

        public int heightModifier;

        public FactoryKey (Font font, boolean antialias, float descentModifier, int heightModifier)
        {
            this.font = font;
            this.antialias = antialias;
            this.descentModifier = descentModifier;
            this.heightModifier = heightModifier;
        }

        @Override // from Object
        public int hashCode ()
        {
            int value = 17;
            value = value * 31 + ((font == null) ? 0 : font.hashCode());
            value = value * 31 + (antialias ? 1 : 0);
            value = value * 31 + Float.floatToIntBits(descentModifier);
            value = value * 31 + heightModifier;
            return value;
        }

        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof FactoryKey)) {
                return false;
            }

            FactoryKey key = (FactoryKey)obj;
            return (antialias == key.antialias) &&
                (descentModifier == key.descentModifier) &&
                (heightModifier == key.heightModifier) &&
                Objects.equal(font, key.font);
        }
    }

    /** The font being rendered by this factory. */
    protected Font _font;

    /** A scratchpad image and its graphics context. */
    protected BufferedImage _scratch;
    protected Graphics2D _graphics;

    /** The font metrics. */
    protected FontMetrics _metrics;

    /** Cached glyphs. */
    protected HashIntMap<Glyph> _glyphs = new HashIntMap<Glyph>();

    /** The glyph texture currently being populated. */
    protected GlyphTexture _texture;

    /** The offset for the descent value. */
    protected int _descentOffset;

    protected int _heightModifier;

    /** Shared instances. */
    protected static Map<FactoryKey, CharacterTextFactory> _instances =
        Maps.newHashMap();

    /** The width/height of the glyph textures. */
    protected static final int TEXTURE_SIZE = 256;
}
