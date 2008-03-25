//
// $Id$

package com.threerings.opengl.gui.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Tuple;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.TextureUnit;

import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Formats text by rendering individual characters into a set of shared textures, then returning
 * {@link Text} instances that render groups of quads, one for each character.
 */
public class CharacterTextFactory extends TextFactory
{
    /**
     * Returns a shared factory instance.
     */
    public static CharacterTextFactory getInstance (Font font, boolean antialias)
    {
        Tuple<Font, Boolean> key = new Tuple<Font, Boolean>(font, antialias);
        CharacterTextFactory factory = _instances.get(key);
        if (factory == null) {
            _instances.put(key, factory = new CharacterTextFactory(font, antialias));
        }
        return factory;
    }

    /**
     * Creates a character text factory with the supplied font.
     */
    public CharacterTextFactory (Font font, boolean antialias)
    {
        _font = font;

        // we need a graphics context to retrieve the metrics
        _scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        _graphics = _scratch.createGraphics();
        _metrics = _graphics.getFontMetrics(font);

        // now that we have the metrics, we can recreate the image in the biggest size we'll
        // need
        Rectangle2D bounds = _metrics.getMaxCharBounds(_graphics);
        _scratch = new BufferedImage(
            (int)Math.ceil(bounds.getWidth()),
            (int)Math.ceil(bounds.getHeight()),
            BufferedImage.TYPE_INT_ARGB);
        _graphics.dispose();
        _graphics = _scratch.createGraphics();
        _graphics.setFont(font);
        _graphics.setBackground(new Color(0, true));
        _graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            antialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
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

    @Override // documentation inherited
    public int getHeight ()
    {
        return _metrics.getHeight();
    }

    @Override // documentation inherited
    public Text createText (final String text, final Color4f color, int effect, int effectSize,
                            Color4f effectColor, boolean useAdvance)
    {
        // get/create glyphs
        final Glyph[] glyphs = new Glyph[text.length()];
        int width = 0;
        for (int ii = 0; ii < glyphs.length; ii++) {
            glyphs[ii] = getGlyph(text.charAt(ii));
            width += glyphs[ii].width;
        }
        final Dimension size = new Dimension(width, _metrics.getHeight());

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
                    tx += glyphs[ii].width;
                    if (x < tx) {
                        return ii;
                    }
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
                float a = color.a * alpha;
                renderer.setColorState(color.r * a, color.g * a, color.b * a, a);
                y += _metrics.getDescent();
                for (Glyph glyph : glyphs) {
                    glyph.render(renderer, x, y);
                    x += glyph.width;
                }
            }
        };
    }

    @Override // documentation inherited
    public Text[] wrapText (String text, Color4f color, int effect, int effectSize,
                            Color4f effectColor, int maxWidth)
    {
        ArrayList<Text> lines = new ArrayList<Text>();
        StringBuffer line = new StringBuffer();
        int width = 0;
        for (int ii = 0, nn = text.length(); ii < nn; ii++) {
            char c = text.charAt(ii);
            Glyph glyph = getGlyph(c);
            if (c == '\n' || width + glyph.width > maxWidth) {
                String extra = "";
                if (c != '\n') {
                    // scan backwards, see if we can break on a space
                    int widx = lastIndexOfWhitespace(line);
                    if (widx != -1) {
                        extra = line.substring(widx + 1, line.length());
                        line.delete(widx, line.length());
                    }
                }
                lines.add(createText(
                    line.toString(), color, effect, effectSize, effectColor, true));
                line = new StringBuffer(extra);
                width = 0;
                for (int jj = 0, ll = extra.length(); jj < ll; jj++) {
                    width += getGlyph(extra.charAt(jj)).width;
                }
            }
            if (c != '\n') {
                line.append(c);
                width += glyph.width;
            }
        }
        if (line.length() > 0) {
            lines.add(createText(line.toString(), color, effect, effectSize, effectColor, true));
        }
        return lines.toArray(new Text[lines.size()]);
    }

    /**
     * Returns the glyph for the given character.
     */
    protected Glyph getGlyph (char c)
    {
        Glyph glyph = _glyphs.get(c);
        if (glyph == null) {
            _glyphs.put(c, glyph = new Glyph(c));
        }
        return glyph;
    }

    /**
     * Returns the index of the last whitespace character in the given buffer, or -1 if there
     * is no whitespace.
     */
    protected int lastIndexOfWhitespace (StringBuffer buf)
    {
        for (int ii = buf.length() - 1; ii >= 0; ii--) {
            if (Character.isWhitespace(buf.charAt(ii))) {
                return ii;
            }
        }
        return -1;
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

        public Glyph (char c)
        {
            width = _metrics.charWidth(_c = c);
            FontRenderContext ctx = _graphics.getFontRenderContext();
            GlyphVector vector = _font.createGlyphVector(ctx, Character.toString(c));
            java.awt.Rectangle bounds = vector.getPixelBounds(ctx, 0f, 0f);
            _bounds = (bounds.width == 0 || bounds.height == 0) ? null : new Rectangle(
                (int)Math.round(bounds.getX()),
                (int)Math.round(-(bounds.getY() + bounds.getHeight())),
                (int)Math.round(bounds.getWidth()),
                (int)Math.round(bounds.getHeight()));
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
                _graphics.drawString(
                    Character.toString(_c), -_bounds.x, _bounds.y + _bounds.height);
                float[] tcoords = new float[4];
                _units = addGlyphToTexture(renderer, _bounds.width, _bounds.height, tcoords);
                _s1 = tcoords[0];
                _t1 = tcoords[1];
                _s2 = tcoords[2];
                _t2 = tcoords[3];
            }
            int lx = x + _bounds.x;
            int ly = y + _bounds.y;
            int ux = lx + _bounds.width;
            int uy = ly + _bounds.height;

            renderer.setTextureState(_units);
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
            _texture.setImage(GL11.GL_RGBA, TEXTURE_SIZE, TEXTURE_SIZE);
            _texture.setMinFilter(GL11.GL_LINEAR);
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

    /** Shared instances. */
    protected static HashMap<Tuple<Font, Boolean>, CharacterTextFactory> _instances =
        Maps.newHashMap();

    /** The width/height of the glyph textures. */
    protected static final int TEXTURE_SIZE = 512;
}
