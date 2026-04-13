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

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.samskivert.util.IntTuple;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.TextureUnit;

import com.threerings.opengl.gui.UIConstants;

/**
 * Creates instances of {@link Text} using a particular technology and a particular font
 * configuration.
 */
public abstract class TextFactory
  implements UIConstants
{
  /**
   * Returns the height of our text.
   */
  public abstract int getHeight ();

  /**
   * Creates a text instance using our the font configuration associated with this text factory
   * and the foreground color specified.
   */
  public Text createText (String text, Color4f color)
  {
    return createText(text, color, NORMAL, DEFAULT_SIZE, null, false);
  }

  /**
   * Creates a text instance using our the font configuration associated with this text factory
   * and the foreground color, text effect and text effect color specified.
   *
   * @param useAdvance if true, the advance to the next insertion point will be included in the
   * bounds of the created text (this is needed by editable text displays).
   */
  public abstract Text createText (String text, Color4f color, int effect, int effectSize,
                   Color4f effectColor, boolean useAdvance);

  /**
   * Wraps a string into a set of text objects that do not exceed the specified width.
   */
  public abstract Text[] wrapText (String text, Color4f color, int effect, int effectSize,
                   Color4f effectColor, int maxWidth);

  /** Finds the last word-break span in the buffer for text wrapping. */
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
        return null;
      } else if (isBreakChar(c) && (!Character.isWhitespace(buf.charAt(ii - 1)))) {
        return new IntTuple(ii + 1, ii + 1);
      }
    }
    return null;
  }

  protected boolean isBreakChar (char c)
  {
    return '-' == c || (c >= 0x4E00 && c <= 0x9FFF);
  }

  /** Inserts a glyph bitmap into the texture atlas. */
  protected TextureUnit[] addGlyphToTexture (
    Renderer renderer, ByteBuffer bitmap, int width, int height, float[] tcoords)
  {
    TextureUnit[] units = (_texture == null) ? null : _texture.add(bitmap, width, height, tcoords);
    if (units == null) {
      _texture = new GlyphTexture(renderer);
      units = _texture.add(bitmap, width, height, tcoords);
    }
    return units;
  }

  /**
   * A shared glyph texture atlas.
   */
  protected class GlyphTexture
  {
    public GlyphTexture (Renderer renderer)
    {
      _texture = new Texture2D(renderer);
      ByteBuffer clear = BufferUtils.createByteBuffer(TEXTURE_SIZE * TEXTURE_SIZE * 4);
      _texture.setImage(
        0, GL11.GL_RGBA, TEXTURE_SIZE, TEXTURE_SIZE, false,
        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, clear);
      _texture.setFilters(GL11.GL_LINEAR, GL11.GL_NEAREST);
      _units = new TextureUnit[] { new TextureUnit(_texture) };
    }

    /** Copies an RGBA glyph bitmap into this texture. */
    public TextureUnit[] add (ByteBuffer rgba, int width, int height, float[] tcoords)
    {
      width = Math.min(Math.max(width, 0), TEXTURE_SIZE);
      height = Math.min(Math.max(height, 0), TEXTURE_SIZE);
      if (_x + width > TEXTURE_SIZE) {
        _y += _height;
        _x = 0;
        _height = 0;
      }
      if (_y + height > TEXTURE_SIZE) {
        return null;
      }

      _texture.setSubimage(0, _x, _y, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgba);

      tcoords[0] = (float)_x / TEXTURE_SIZE;
      tcoords[1] = (float)_y / TEXTURE_SIZE;
      tcoords[2] = (float)(_x + width) / TEXTURE_SIZE;
      tcoords[3] = (float)(_y + height) / TEXTURE_SIZE;

      _x += width;
      _height = Math.max(_height, height);
      return _units;
    }

    protected TextureUnit[] _units;
    protected Texture2D _texture;
    protected int _x, _y;
    protected int _height;
  }

  /** The glyph texture currently being populated. */
  protected GlyphTexture _texture;

  /** The width/height of the glyph textures. */
  protected static final int TEXTURE_SIZE = 256;
}
