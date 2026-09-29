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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntTuple;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.TextureUnit;

import com.threerings.opengl.gui.UIConstants;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.util.GlUtil;

/**
 * Formats text by rendering individual characters into sets of shared textures, one set for each
 * scale at which the text is drawn, then returning {@link Text} instances that render groups of
 * quads, one for each character.
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
    _antialias = antialias;

    // we need a graphics context to retrieve the metrics
    Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
    _metrics = graphics.getFontMetrics(font);
    _descentOffset = Math.round(_metrics.getHeight() * descentModifier);
    _heightModifier = heightModifier;

    // and to create the glyph outlines
    applyRenderingHints(graphics);
    _frc = graphics.getFontRenderContext();
    graphics.dispose();

    // the glyphs are placed according to the font's own fractional, kerned spacing
    _layoutFont = font.deriveFont(
      Collections.singletonMap(TextAttribute.KERNING, TextAttribute.KERNING_ON));
    _layoutFrc = new FontRenderContext(null, antialias, true);
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
    // get/create glyphs and place them, noting the pen position of each and then of the end
    final Glyph[] glyphs = new Glyph[text.length()];
    final float[] positions = new float[glyphs.length + 1];
    float pen = 0f;
    for (int ii = 0; ii < glyphs.length; ii++) {
      glyphs[ii] = getGlyph(text.charAt(ii));
      if (ii > 0) {
        pen += getKerning(text.charAt(ii - 1), text.charAt(ii));
      }
      positions[ii] = pen;
      pen += glyphs[ii].advance;
    }
    positions[glyphs.length] = pen;

    final Dimension size = new Dimension((int)Math.ceil(pen), getHeight());

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
        for (int ii = 0; ii < glyphs.length; ii++) {
          if (x < (positions[ii] + positions[ii + 1]) / 2) {
            return ii;
          }
        }
        return glyphs.length;
      }
      public int getCursorPos (int index) {
        return Math.round(positions[Math.max(0, Math.min(index, glyphs.length))]);
      }
      public void render (Renderer renderer, int x, int y, float alpha, float scale) {
        GlyphAtlas atlas = getAtlas(scale);

        // add the descent above the baseline
        y += _metrics.getDescent() + _descentOffset;

        // multi-pixel outlines go below the character
        if (outlines != null && effectSize > 1) {
          renderGlyphs(renderer, atlas, outlines, effectColor, x, y, alpha);
        }
        // as do shadows
        if (effect == SHADOW) {
          renderGlyphs(
            renderer, atlas, glyphs, effectColor, x + effectSize - 1, y - effectSize, alpha);
          x += 1;
        }

        // now draw the characters
        renderGlyphs(renderer, atlas, glyphs, color, x, y, alpha);

        // single-pixel outlines go on top of the character
        if (outlines != null && effectSize == 1) {
          renderGlyphs(renderer, atlas, outlines, effectColor, x, y, alpha);
        }
      }
      protected void renderGlyphs (
        Renderer renderer, GlyphAtlas atlas, Glyph[] glyphs, Color4f color,
        int x, int y, float alpha) {
        float a = color.a * alpha;
        renderer.setColorState(color.r * a, color.g * a, color.b * a, a);
        // offsets in whole pixels from the start of the run, so the glyphs snap to the pixel grid
        // together rather than each on its own
        float scale = atlas.scale;
        for (int ii = 0; ii < glyphs.length; ii++) {
          atlas.render(renderer, glyphs[ii], x + Math.round(positions[ii] * scale) / scale, y);
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
    // measured exactly as createText places glyphs, so each line fits the width it's wrapped to
    float width = 0f;
    for (int ii = 0, nn = text.length(); ii < nn; ii++) {
      char c = text.charAt(ii);
      Glyph glyph = getGlyph(c);
      float kerning = (line.length() > 0) ? getKerning(line.charAt(line.length() - 1), c) : 0f;
      if (c == '\n' || width + kerning + glyph.advance > maxWidth) {
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
        width = 0f;
        for (int jj = 0, ll = extra.length(); jj < ll; jj++) {
          if (jj > 0) {
            width += getKerning(extra.charAt(jj - 1), extra.charAt(jj));
          }
          width += getGlyph(extra.charAt(jj)).advance;
        }
      } else {
        line.append(c);
        width += kerning;
        width += glyph.advance;
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
   * Returns the adjustment the font's kerning makes to the pen between two characters.
   */
  protected float getKerning (char left, char right)
  {
    int key = (left << 16) | right;
    Float kerning = _kerning.get(key);
    if (kerning == null) {
      char[] pair = { left, right };
      GlyphVector vector = _layoutFont.layoutGlyphVector(
        _layoutFrc, pair, 0, 2, Font.LAYOUT_LEFT_TO_RIGHT);
      // shaping may make one glyph of the two, which we can't place separately
      _kerning.put(key, kerning = (vector.getNumGlyphs() == 2)
        ? (float)vector.getGlyphPosition(1).getX() - getGlyph(left).advance : 0f);
    }
    return kerning;
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
   * Configures a graphics context for laying out and rendering our glyphs.
   */
  protected void applyRenderingHints (Graphics2D graphics)
  {
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      _antialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    graphics.setRenderingHint(
      RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
  }

  /**
   * Returns the atlas of our glyphs rasterized at the specified scale, creating it if necessary.
   */
  protected GlyphAtlas getAtlas (float scale)
  {
    // kept in most recently used order
    for (int ii = 0, nn = _atlases.size(); ii < nn; ii++) {
      GlyphAtlas atlas = _atlases.get(ii);
      if (atlas.scale == scale) {
        if (ii > 0) {
          _atlases.add(0, _atlases.remove(ii));
        }
        return atlas;
      }
    }
    GlyphAtlas atlas = new GlyphAtlas(scale);
    _atlases.add(0, atlas);
    if (_atlases.size() > MAX_ATLASES) {
      _atlases.remove(MAX_ATLASES).dispose();
    }
    return atlas;
  }

  /**
   * A single glyph, independent of the scale at which it's drawn.
   */
  protected class Glyph
  {
    /** The advance of this glyph, by the font's own (fractional) spacing. */
    public float advance;

    public Glyph (char c, int effect, int size)
    {
      advance = (float)_layoutFont.createGlyphVector(_layoutFrc, Character.toString(_c = c))
        .getGlyphPosition(1).getX();
      _effect = effect;
      _size = size;
      _outline = _font.createGlyphVector(_frc, Character.toString(c)).getOutline();
      Rectangle2D bounds = _outline.getBounds2D();
      if (!bounds.isEmpty()) {
        _bounds = bounds;
      }
    }

    /**
     * Renders this glyph at the specified scale into a new image. Populates the supplied
     * rectangle with the image's offset from the pen, in pixels with y up, and its size.
     */
    protected BufferedImage createImage (float scale, Rectangle bounds)
    {
      // size from the outline we fill; getPixelBounds() measures the font's own glyph image,
      // which can be narrower
      int x1 = (int)Math.floor(_bounds.getMinX() * scale);
      int y1 = (int)Math.floor(_bounds.getMinY() * scale);
      int x2 = (int)Math.ceil(_bounds.getMaxX() * scale);
      int y2 = (int)Math.ceil(_bounds.getMaxY() * scale);
      bounds.set(x1, -y2, x2 - x1, y2 - y1);
      int grow = 1 + (_effect == OUTLINE ? (int)Math.ceil(_size * scale / 2f) : 0);
      bounds.grow(grow, grow);

      BufferedImage image = new BufferedImage(
        bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = image.createGraphics();
      try {
        applyRenderingHints(graphics);
        Shape outline = new AffineTransform(
          scale, 0f, 0f, scale, -bounds.x, bounds.y + bounds.height)
          .createTransformedShape(_outline);
        if (_effect == OUTLINE) {
          graphics.setStroke(new BasicStroke(
            _size * scale, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
          graphics.draw(outline);
        } else {
          graphics.fill(outline);
        }
      } finally {
        graphics.dispose();
      }
      return image;
    }

    /** The glyph character. */
    protected char _c;

    /** The effect and effect size. */
    protected int _effect, _size;

    /** The outline, relative to the pen with y down. */
    protected Shape _outline;

    /** The bounds of the outline, or null if it's blank. */
    protected Rectangle2D _bounds;
  }

  /**
   * Our glyphs rasterized at one scale, and the textures that hold them.
   */
  protected static class GlyphAtlas
  {
    /** The number of pixels per unit at which glyphs are rasterized. */
    public final float scale;

    public GlyphAtlas (float scale)
    {
      this.scale = scale;
    }

    /**
     * Renders a glyph with its pen at the specified position.
     */
    public void render (Renderer renderer, Glyph glyph, float x, float y)
    {
      if (glyph._bounds == null) {
        return; // whitespace
      }
      Raster raster = _rasters.get(glyph);
      if (raster == null) {
        _rasters.put(glyph, raster = createRaster(renderer, glyph));
      }
      raster.render(renderer, x, y);
    }

    /**
     * Deletes our textures.
     */
    public void dispose ()
    {
      for (GlyphTexture texture : _textures) {
        texture.delete();
      }
    }

    /**
     * Rasterizes a glyph into one of our textures.
     */
    protected Raster createRaster (Renderer renderer, Glyph glyph)
    {
      Rectangle bounds = new Rectangle();
      BufferedImage image = glyph.createImage(scale, bounds);

      // try to add to the current texture; if there's not enough room, create a new one
      Raster raster = new Raster();
      float[] tcoords = new float[4];
      GlyphTexture texture = _textures.isEmpty() ? null : _textures.get(_textures.size() - 1);
      if (texture == null || (raster.units = texture.add(image, tcoords)) == null) {
        int size = Math.max(TEXTURE_SIZE, GlUtil.nextPowerOfTwo(
          Math.max(bounds.width, bounds.height)));
        _textures.add(texture = new GlyphTexture(renderer, size, scale != 1f));
        raster.units = texture.add(image, tcoords);
      }
      raster.s1 = tcoords[0];
      raster.t1 = tcoords[1];
      raster.s2 = tcoords[2];
      raster.t2 = tcoords[3];

      // at one, we draw as we always have; otherwise each texel covers exactly one pixel, and we
      // nudge the quad so that pixel centers don't land on texel edges when a glyph falls on a
      // half pixel
      float bias = (scale == 1f) ? 0f : SNAP_BIAS;
      raster.x1 = (bounds.x + bias) / scale;
      raster.y1 = (bounds.y + bias) / scale;
      raster.x2 = (bounds.x + bounds.width + bias) / scale;
      raster.y2 = (bounds.y + bounds.height + bias) / scale;
      return raster;
    }

    /** The glyphs rasterized so far. */
    protected Map<Glyph, Raster> _rasters = new IdentityHashMap<Glyph, Raster>();

    /** Our textures; the last is the one being populated. */
    protected ArrayList<GlyphTexture> _textures = new ArrayList<GlyphTexture>();
  }

  /**
   * A glyph rasterized at one scale.
   */
  protected static class Raster
  {
    /** The texture units holding the glyph image. */
    public TextureUnit[] units;

    /** The corners of the glyph's quad relative to the pen, in unscaled units. */
    public float x1, y1, x2, y2;

    /** The texture coordinates of the glyph image. */
    public float s1, t1, s2, t2;

    /**
     * Renders the glyph with its pen at the specified position.
     */
    public void render (Renderer renderer, float x, float y)
    {
      float lx = x + x1, ly = y + y1;
      float ux = x + x2, uy = y + y2;

      renderer.setTextureState(units);
      renderer.setMatrixMode(GL11.GL_MODELVIEW);
      GL11.glBegin(GL11.GL_QUADS);
      GL11.glTexCoord2f(s1, t1);
      GL11.glVertex2f(lx, ly);
      GL11.glTexCoord2f(s2, t1);
      GL11.glVertex2f(ux, ly);
      GL11.glTexCoord2f(s2, t2);
      GL11.glVertex2f(ux, uy);
      GL11.glTexCoord2f(s1, t2);
      GL11.glVertex2f(lx, uy);
      GL11.glEnd();
    }
  }

  /**
   * A shared texture.
   */
  protected static class GlyphTexture
  {
    /**
     * @param snap if true, sample nearest texels even when minifying: the glyphs are drawn with a
     * pixel for every texel, and must stay on the pixel grid.
     */
    public GlyphTexture (Renderer renderer, int size, boolean snap)
    {
      _size = size;
      _texture = new Texture2D(renderer);
      _texture.setImage(GL11.GL_RGBA, size, size, false, false);
      _texture.setFilters(snap ? GL11.GL_NEAREST : GL11.GL_LINEAR, GL11.GL_NEAREST);
      _units = new TextureUnit[] { new TextureUnit(_texture) };
    }

    /**
     * Attempts to copy the glyph image into this texture.
     */
    public TextureUnit[] add (BufferedImage image, float[] tcoords)
    {
      int width = image.getWidth(), height = image.getHeight();

      // move up to the next row if necessary
      if (_x + width > _size) {
        _y += _height;
        _x = 0;
        _height = 0;
      }
      if (_x + width > _size || _y + height > _size) {
        return null; // out of room in this texture
      }

      // copy the image into the texture
      _texture.setSubimage(image, true, _x, _y, width, height);

      // set the texture coordinates
      tcoords[0] = (float)_x / _size;
      tcoords[1] = (float)_y / _size;
      tcoords[2] = (float)(_x + width) / _size;
      tcoords[3] = (float)(_y + height) / _size;

      // advance to the next position
      _x += width;
      _height = Math.max(_height, height);

      // return the texture units
      return _units;
    }

    /**
     * Deletes the texture.
     */
    public void delete ()
    {
      _texture.delete();
    }

    /** The width and height of the texture. */
    protected int _size;

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
        Objects.equals(font, key.font);
    }
  }

  /** The font being rendered by this factory. */
  protected Font _font;

  /** Whether or not to antialias the glyphs. */
  protected boolean _antialias;

  /** The context in which we create glyph outlines. */
  protected FontRenderContext _frc;

  /** Our font with kerning enabled, and the (fractional) context, for placing glyphs. */
  protected Font _layoutFont;
  protected FontRenderContext _layoutFrc;

  /** The font metrics. */
  protected FontMetrics _metrics;

  /** Cached glyphs. */
  protected HashIntMap<Glyph> _glyphs = new HashIntMap<Glyph>();

  /** Cached kerning between pairs of characters, keyed by the pair. */
  protected HashIntMap<Float> _kerning = new HashIntMap<Float>();

  /** Our glyphs rasterized at each scale in use, most recently used first. */
  protected ArrayList<GlyphAtlas> _atlases = new ArrayList<GlyphAtlas>();

  /** The offset for the descent value. */
  protected int _descentOffset;

  protected int _heightModifier;

  /** Shared instances. */
  protected static Map<FactoryKey, CharacterTextFactory> _instances =
    Maps.newHashMap();

  /** The width/height of the glyph textures (larger if a glyph needs it). */
  protected static final int TEXTURE_SIZE = 256;

  /** The most scales for which we keep glyphs: the root's and one (for billboards and such) are
   * typically all that's in use, plus one left behind when the root's changes. */
  protected static final int MAX_ATLASES = 3;

  /** The fraction of a pixel by which we nudge glyphs rasterized at scales other than one.
   * TODO: at arbitrary scales (e.g. a "max" UI scale of 45/32) some glyphs still land exactly on
   * a tie; Apple's GPU snaps them whole, but if another tears them, align each quad to the pixel
   * grid using its absolute position instead. */
  protected static final float SNAP_BIAS = 1/8f;
}
