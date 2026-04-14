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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;

import com.google.common.collect.Maps;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntTuple;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.TextureUnit;

import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Rectangle;

import static com.threerings.opengl.Log.log;

/**
 * Formats text by rendering individual characters into a set of shared textures using
 * STB TrueType, then returning {@link Text} instances that render groups of quads.
 * This implementation avoids java.awt.Graphics2D which triggers macOS AWT toolkit
 * initialization that conflicts with GLFW.
 */
public class CharacterTextFactory extends TextFactory
{
  /**
   * Returns a shared STB-based factory instance.
   */
  public static CharacterTextFactory getInstance (
    ByteBuffer fontData, int style, int size, boolean antialias,
    float descentModifier, int heightModifier)
  {
    FactoryKey key = new FactoryKey(fontData, style, size, antialias,
      descentModifier, heightModifier);
    CharacterTextFactory factory = _instances.get(key);
    if (factory == null) {
      _instances.put(key,
        factory = new CharacterTextFactory(fontData, style, size, antialias,
          descentModifier, heightModifier));
    }
    return factory;
  }

  /**
   * Creates a character text factory with STB TrueType.
   *
   * @param fontData the raw TTF/TTC file data (must remain valid for the factory's lifetime)
   * @param style font style flags (java.awt.Font.BOLD, etc.) — used to select bold/italic
   * @param size font size in pixels
   * @param antialias whether to antialias (ignored for now; STB always antialiases)
   * @param descentModifier descent adjustment as fraction of height
   * @param heightModifier additional height in pixels
   */
  public CharacterTextFactory (
    ByteBuffer fontData, int style, int size, boolean antialias,
    float descentModifier, int heightModifier)
  {
    _fontData = fontData;
    _fontSize = size;
    _heightModifier = heightModifier;

    _fontInfo = STBTTFontinfo.create();
    if (!STBTruetype.stbtt_InitFont(_fontInfo, _fontData)) {
      log.warning("Failed to initialize STB TrueType font.");
      _scale = 1f;
      _ascent = size;
      _descent = 0;
      _lineGap = 0;
      _height = size;
      return;
    }

    // Compute the scale factor from font units to pixel size
    _scale = STBTruetype.stbtt_ScaleForPixelHeight(_fontInfo, size);

    // Get vertical metrics in font units, then scale to pixels
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer pAscent = stack.mallocInt(1);
      IntBuffer pDescent = stack.mallocInt(1);
      IntBuffer pLineGap = stack.mallocInt(1);
      STBTruetype.stbtt_GetFontVMetrics(_fontInfo, pAscent, pDescent, pLineGap);
      _ascent = Math.round(pAscent.get(0) * _scale);
      _descent = Math.round(pDescent.get(0) * _scale); // negative value
      _lineGap = Math.round(pLineGap.get(0) * _scale);
    }

    _height = _ascent - _descent + _lineGap;
    _descentOffset = Math.round(_height * descentModifier);
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
    return _height + _heightModifier;
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
        y += (-_descent) + _descentOffset;

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
    lines.add(createText(line.toString(), color, effect, effectSize, effectColor, true));
    return lines.toArray(new Text[lines.size()]);
  }

  /** Returns the normal glyph for the given character. */
  protected Glyph getGlyph (char c)
  {
    return getGlyph(c, NORMAL, 0);
  }

  /** Returns the glyph for the given character with the given effect. */
  protected Glyph getGlyph (char c, int effect, int size)
  {
    int key = (size << 20) | (effect << 16) | c;
    Glyph glyph = _glyphs.get(key);
    if (glyph == null) {
      _glyphs.put(key, glyph = new Glyph(c, effect, size));
    }
    return glyph;
  }

  /**
   * A single glyph, rasterized via STB TrueType.
   */
  protected class Glyph
  {
    /** The advance width of this glyph in pixels. */
    public int width;

    public Glyph (char c, int effect, int effectSize)
    {
      _c = c;
      _effect = effect;
      _effectSize = effectSize;

      try (MemoryStack stack = MemoryStack.stackPush()) {
        IntBuffer pAdvance = stack.mallocInt(1);
        IntBuffer pLsb = stack.mallocInt(1);
        STBTruetype.stbtt_GetCodepointHMetrics(_fontInfo, c, pAdvance, pLsb);
        width = Math.round(pAdvance.get(0) * _scale);

        // Get the bounding box for this glyph
        IntBuffer x0 = stack.mallocInt(1), y0 = stack.mallocInt(1);
        IntBuffer x1 = stack.mallocInt(1), y1 = stack.mallocInt(1);
        STBTruetype.stbtt_GetCodepointBitmapBox(
          _fontInfo, c, _scale, _scale, x0, y0, x1, y1);
        int bw = x1.get(0) - x0.get(0);
        int bh = y1.get(0) - y0.get(0);
        if (bw > 0 && bh > 0) {
          int grow = 1 + (effect == OUTLINE ? Math.round(effectSize / 2f) : 0);
          _bounds = new Rectangle(
            x0.get(0) - grow,
            -y1.get(0) - grow, // flip Y: STB has y-down, we need y-up from baseline
            bw + grow * 2,
            bh + grow * 2);
        }
      }
    }

    /** Renders this glyph at the specified position. */
    public void render (Renderer renderer, int x, int y)
    {
      if (_units == null) {
        if (_bounds == null) {
          return; // whitespace
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
          IntBuffer pw = stack.mallocInt(1), ph = stack.mallocInt(1);
          IntBuffer pxoff = stack.mallocInt(1), pyoff = stack.mallocInt(1);
          ByteBuffer bitmap = STBTruetype.stbtt_GetCodepointBitmap(
            _fontInfo, _scale, _scale, _c, pw, ph, pxoff, pyoff);

          if (bitmap != null) {
            int bw = pw.get(0);
            int bh = ph.get(0);

            // Convert single-channel alpha bitmap to RGBA
            ByteBuffer rgba = BufferUtils.createByteBuffer(
              _bounds.width * _bounds.height * 4);
            int padX = (-pxoff.get(0)) + _bounds.x;
            int padY = (-pyoff.get(0)) - (_bounds.y + _bounds.height);
            for (int i = 0; i < _bounds.width * _bounds.height * 4; i++) {
              rgba.put(i, (byte)0);
            }
            // Copy bitmap alpha into premultiplied RGBA, flipping vertically
            for (int row = 0; row < bh; row++) {
              for (int col = 0; col < bw; col++) {
                int dx = col - padX;
                int dy = (bh - 1 - row) - padY;
                if (dx >= 0 && dx < _bounds.width && dy >= 0 && dy < _bounds.height) {
                  byte alpha = bitmap.get(row * bw + col);
                  int idx = (dy * _bounds.width + dx) * 4;
                  rgba.put(idx, alpha);
                  rgba.put(idx + 1, alpha);
                  rgba.put(idx + 2, alpha);
                  rgba.put(idx + 3, alpha);
                }
              }
            }
            rgba.rewind();

            float[] tcoords = new float[4];
            _units = addGlyphToTexture(renderer, rgba, _bounds.width, _bounds.height, tcoords);
            _s1 = tcoords[0];
            _t1 = tcoords[1];
            _s2 = tcoords[2];
            _t2 = tcoords[3];

            STBTruetype.stbtt_FreeBitmap(bitmap);
          }
        }
      }
      if (_units == null || _bounds == null) {
        return;
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

    protected char _c;
    protected int _effect, _effectSize;
    protected Rectangle _bounds;
    protected TextureUnit[] _units;
    protected float _s1, _t1, _s2, _t2;
  }

  protected static class FactoryKey
  {
    public ByteBuffer fontData;
    public int style, size;
    public boolean antialias;
    public float descentModifier;
    public int heightModifier;

    public FactoryKey (ByteBuffer fontData, int style, int size,
      boolean antialias, float descentModifier, int heightModifier)
    {
      this.fontData = fontData;
      this.style = style;
      this.size = size;
      this.antialias = antialias;
      this.descentModifier = descentModifier;
      this.heightModifier = heightModifier;
    }

    @Override
    public int hashCode ()
    {
      int value = 17;
      value = value * 31 + System.identityHashCode(fontData);
      value = value * 31 + style;
      value = value * 31 + size;
      value = value * 31 + (antialias ? 1 : 0);
      value = value * 31 + Float.floatToIntBits(descentModifier);
      value = value * 31 + heightModifier;
      return value;
    }

    @Override
    public boolean equals (Object obj)
    {
      if (!(obj instanceof FactoryKey)) return false;
      FactoryKey k = (FactoryKey)obj;
      return fontData == k.fontData && style == k.style && size == k.size &&
        antialias == k.antialias && descentModifier == k.descentModifier &&
        heightModifier == k.heightModifier;
    }
  }

  /** The raw TTF data (must remain valid). */
  protected ByteBuffer _fontData;

  /** The STB font info. */
  protected STBTTFontinfo _fontInfo;

  /** The font size in pixels and the scale from font units to pixels. */
  protected int _fontSize;
  protected float _scale;

  /** Vertical metrics in pixels. */
  protected int _ascent, _descent, _lineGap;

  /** Total line height in pixels. */
  protected int _height;

  /** Height modifier from config. */
  protected int _heightModifier;

  /** Descent offset from config. */
  protected int _descentOffset;

  /** Cached glyphs. */
  protected HashIntMap<Glyph> _glyphs = new HashIntMap<Glyph>();

  /** Shared instances. */
  protected static Map<FactoryKey, CharacterTextFactory> _instances = Maps.newHashMap();
}
