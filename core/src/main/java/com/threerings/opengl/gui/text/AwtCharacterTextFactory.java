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

import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntTuple;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * AWT-based text factory for use by tool classes (scene editor, model viewer, etc.)
 * that already use AWT/Swing. This calls BufferedImage.createGraphics() which triggers
 * macOS AWT toolkit initialization — do NOT use from the GLFW game client.
 */
public class AwtCharacterTextFactory extends TextFactory
{
  /**
   * Creates a factory for the given AWT font.
   */
  public static AwtCharacterTextFactory getInstance (
    Font font, boolean antialias, float descentModifier)
  {
    return getInstance(font, antialias, descentModifier, 0);
  }

  /**
   * Creates a factory for the given AWT font.
   */
  public static AwtCharacterTextFactory getInstance (
    Font font, boolean antialias, float descentModifier, int heightModifier)
  {
    return new AwtCharacterTextFactory(font, antialias, descentModifier, heightModifier);
  }

  public AwtCharacterTextFactory (Font font, boolean antialias,
    float descentModifier, int heightModifier)
  {
    _awtFont = font;

    _scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    _graphics = _scratch.createGraphics();
    _awtMetrics = _graphics.getFontMetrics(font);

    _awtHeight = _awtMetrics.getHeight() + heightModifier;
    _awtDescent = _awtMetrics.getDescent();
    _awtDescentOffset = Math.round(_awtMetrics.getHeight() * descentModifier);

    // Create scratch pad sized for glyphs
    FontRenderContext ctx = _graphics.getFontRenderContext();
    GlyphVector vector = font.createGlyphVector(ctx, "J");
    java.awt.Rectangle bounds = vector.getPixelBounds(ctx, 0f, 0f);
    _scratch = new BufferedImage(
      Math.max(bounds.width * 4, 4), Math.max(bounds.height * 4, 4),
      BufferedImage.TYPE_INT_ARGB);
    _graphics.dispose();
    _graphics = _scratch.createGraphics();
    _graphics.setFont(font);
    _graphics.setBackground(new Color(0, true));
    _graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      antialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    _graphics.setRenderingHint(
      RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
  }

  @Override
  public int getHeight ()
  {
    return _awtHeight;
  }

  @Override
  public Text createText (
    final String text, final Color4f color, final int effect,
    final int effectSize, final Color4f effectColor, boolean useAdvance)
  {
    final AwtGlyph[] glyphs = new AwtGlyph[text.length()];
    int width = 0;
    for (int ii = 0; ii < glyphs.length; ii++) {
      glyphs[ii] = getAwtGlyph(text.charAt(ii));
      width += glyphs[ii].width;
    }
    final Dimension size = new Dimension(width, getHeight());

    final AwtGlyph[] outlines = (effect == OUTLINE) ? new AwtGlyph[text.length()] : null;
    if (effect == OUTLINE) {
      for (int ii = 0; ii < outlines.length; ii++) {
        outlines[ii] = getAwtGlyph(text.charAt(ii), OUTLINE, effectSize);
      }
    }

    return new Text() {
      public int getLength () { return glyphs.length; }
      public Dimension getSize () { return size; }
      public int getHitPos (int x, int y) {
        int tx = 0;
        for (int ii = 0; ii < glyphs.length; ii++) {
          int hwidth = glyphs[ii].width / 2;
          tx += hwidth;
          if (x < tx) return ii;
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
        y += _awtDescent + _awtDescentOffset;
        if (outlines != null && effectSize > 1) {
          renderAwtGlyphs(renderer, outlines, effectColor, x, y, alpha);
        }
        if (effect == SHADOW) {
          renderAwtGlyphs(renderer, glyphs, effectColor,
            x + effectSize - 1, y - effectSize, alpha);
          x += 1;
        }
        renderAwtGlyphs(renderer, glyphs, color, x, y, alpha);
        if (outlines != null && effectSize == 1) {
          renderAwtGlyphs(renderer, outlines, effectColor, x, y, alpha);
        }
      }
      void renderAwtGlyphs (
        Renderer renderer, AwtGlyph[] glyphs, Color4f color, int x, int y, float alpha) {
        float a = color.a * alpha;
        renderer.setColorState(color.r * a, color.g * a, color.b * a, a);
        for (AwtGlyph glyph : glyphs) {
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
    java.util.ArrayList<Text> lines = new java.util.ArrayList<>();
    StringBuilder line = new StringBuilder();
    int width = 0;
    for (int ii = 0, nn = text.length(); ii < nn; ii++) {
      char c = text.charAt(ii);
      AwtGlyph glyph = getAwtGlyph(c);
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
        lines.add(createText(line.toString(), color, effect, effectSize, effectColor, true));
        line.setLength(0);
        line.append(extra);
        width = 0;
        for (int jj = 0, ll = extra.length(); jj < ll; jj++) {
          width += getAwtGlyph(extra.charAt(jj)).width;
        }
      } else {
        line.append(c);
        width += glyph.width;
      }
    }
    lines.add(createText(line.toString(), color, effect, effectSize, effectColor, true));
    return lines.toArray(new Text[lines.size()]);
  }

  protected AwtGlyph getAwtGlyph (char c)
  {
    return getAwtGlyph(c, NORMAL, 0);
  }

  protected AwtGlyph getAwtGlyph (char c, int effect, int size)
  {
    int key = (size << 20) | (effect << 16) | c;
    AwtGlyph glyph = _awtGlyphs.get(key);
    if (glyph == null) {
      _awtGlyphs.put(key, glyph = new AwtGlyph(c, effect, size));
    }
    return glyph;
  }

  protected class AwtGlyph
  {
    public int width;

    public AwtGlyph (char c, int effect, int size)
    {
      width = _awtMetrics.charWidth(_c = c);
      _effect = effect;
      _size = size;
      FontRenderContext ctx = _graphics.getFontRenderContext();
      _vector = _awtFont.createGlyphVector(ctx, Character.toString(c));
      java.awt.Rectangle bounds = _vector.getPixelBounds(ctx, 0f, 0f);
      if (bounds.width > 0 && bounds.height > 0) {
        _bounds = new Rectangle(
          bounds.x, -bounds.y - bounds.height, bounds.width, bounds.height);
        int grow = 1 + (effect == OUTLINE ? Math.round(size / 2f) : 0);
        _bounds.grow(grow, grow);
      }
    }

    public void render (Renderer renderer, int x, int y)
    {
      if (_units == null) {
        if (_bounds == null) return;
        _graphics.clearRect(0, 0, _scratch.getWidth(), _scratch.getHeight());
        Shape outline = _vector.getOutline(-_bounds.x, _bounds.y + _bounds.height);
        if (_effect == OUTLINE) {
          _graphics.setStroke(new BasicStroke(
            _size, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
          _graphics.draw(outline);
        } else {
          _graphics.fill(outline);
        }

        int w = Math.min(_bounds.width, _scratch.getWidth());
        int h = Math.min(_bounds.height, _scratch.getHeight());
        ByteBuffer rgba = BufferUtils.createByteBuffer(w * h * 4);
        for (int row = 0; row < h; row++) {
          for (int col = 0; col < w; col++) {
            int pixel = _scratch.getRGB(col, row);
            rgba.put((byte)((pixel >> 16) & 0xFF));
            rgba.put((byte)((pixel >> 8) & 0xFF));
            rgba.put((byte)(pixel & 0xFF));
            rgba.put((byte)((pixel >> 24) & 0xFF));
          }
        }
        rgba.flip();

        float[] tcoords = new float[4];
        _units = addGlyphToTexture(renderer, rgba, w, h, tcoords);
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

    protected char _c;
    protected int _effect, _size;
    protected GlyphVector _vector;
    protected Rectangle _bounds;
    protected TextureUnit[] _units;
    protected float _s1, _t1, _s2, _t2;
  }

  protected Font _awtFont;
  protected BufferedImage _scratch;
  protected Graphics2D _graphics;
  protected FontMetrics _awtMetrics;
  protected int _awtHeight;
  protected int _awtDescent;
  protected int _awtDescentOffset;
  protected HashIntMap<AwtGlyph> _awtGlyphs = new HashIntMap<>();
}
