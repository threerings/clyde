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

package com.threerings.opengl.renderer.config;

import java.awt.Color;

import java.util.Arrays;

import com.threerings.io.Streamable;

import com.threerings.media.image.ColorPository;
import com.threerings.media.image.ColorPository.ClassRecord;
import com.threerings.media.image.Colorization;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.PreparedEditable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.log;

/**
 * Describes a colorization.
 */
@EditorTypes({
  ColorizationConfig.Normal.class, ColorizationConfig.TransNormal.class,
  ColorizationConfig.LumaTint.class,
  ColorizationConfig.CustomOffsets.class, ColorizationConfig.FullyCustom.class,
  ColorizationConfig.Translated.class
})
public abstract class ColorizationConfig extends DeepObject
  implements Exportable, Streamable
{
  /**
   * Creates a colorization config
   */
  public static ColorizationConfig.CustomOffsets createConfig (
      int clazz, float hue, float saturation, float value)
  {
    ColorizationConfig.CustomOffsets config = new ColorizationConfig.CustomOffsets();
    config.clazz = clazz;
    config.offsets.hue = hue;
    config.offsets.saturation = saturation;
    config.offsets.value = value;
    return config;
  }

  /**
   * Creates a normal colorization config.
   */
  public static ColorizationConfig.Normal createConfig (int colorization)
  {
    ColorizationConfig.Normal config = new ColorizationConfig.Normal();
    config.colorization = colorization;
    return config;
  }

  /**
   * A reference to a pository colorization.
   */
  public static class Normal extends ColorizationConfig
  {
    /** The colorization reference. */
    @Editable(editor="colorization")
    public int colorization;

    @Override
    public Colorization getColorization (ColorPository colorpos)
    {
      return colorpos.getColorization(colorization);
    }

    /** Basic constructor needed explicitly when there are others, thanks Java. */
    public Normal () {}

    /** Convenience for editor use: convert transnormal. */
    public Normal (TransNormal trans)
    {
      this.colorization = (trans.clazz == trans.colorization >> 8) ? trans.colorization
        : (trans.clazz << 8) + 1;
    }
  }

  /**
   * Uses a pository class and a custom color.
   */
  public static class CustomOffsets extends ColorizationConfig
  {
    /** The colorization class. */
    @Editable(editor="colorization", mode="class")
    public int clazz;

    /** The color offsets. */
    @Editable
    public Triplet offsets = new Triplet();

    @Override
    public Colorization getColorization (ColorPository colorpos)
    {
      ClassRecord crec = colorpos.getClassRecord(clazz);
      return (crec == null) ? null : new CustomOffsetsColorization(crec, offsets.getValues());
    }
  }

  /**
   * A fully custom colorization.
   */
  public static class FullyCustom extends ColorizationConfig
  {
    /** The source color. */
    @Editable
    public Color4f source = new Color4f();

    /** The range to recolor. */
    @Editable
    public Triplet range = new Triplet();

    /** The color offsets. */
    @Editable
    public Triplet offsets = new Triplet();

    @Override
    public Colorization getColorization (ColorPository colorpos)
    {
      return new FullyCustomColorization(
        source.getColor(), range.getValues(), offsets.getValues());
    }
  }

  /**
   * Translate a colorization to another target.
   */
  public static class Translated extends ColorizationConfig
    implements PreparedEditable
  {
    /** The colorization class. */
    @Editable(editor="colorization", mode="class")
    public int clazz;

    /** The colorization to translate. */
    @Editable
    public ColorizationConfig source;

    @Override
    public Colorization getColorization (ColorPository colorpos)
    {
      ClassRecord crec = colorpos.getClassRecord(clazz);
      Colorization src = (source == null)
          ? null
          : source.getColorization(colorpos);
      return (src == null || crec == null)
          ? null
          : new CustomOffsetsColorization(crec, src);
    }

    // from PreparedEditable
    public void prepareInstanceToEdit ()
    {
      if (source == null) source = new ColorizationConfig.Normal();
    }

    /** Basic constructor needed explicitly when there are others, thanks Java. */
    public Translated () {}

    /** Convenience for editor use: convert normal. */
    public Translated (Normal normal)
    {
      this.clazz = normal.colorization >> 8;
      this.source = normal;
    }

    public Translated (TransNormal tn)
    {
      Normal norm = new Normal();
      norm.colorization = tn.colorization;
      this.clazz = tn.clazz;
      this.source = norm;
    }
  }

  /**
   * Translate a normal colorization to a different base clazz.
   */
  public static class TransNormal extends ColorizationConfig
  {
    /** The colorization class. */
    @Editable(editor="colorization", mode="class", hgroup="c")
    public int clazz;

    /** The colorization reference. */
    @Editable(editor="colorization", hgroup="c")
    public int colorization;

    @Override
    public Colorization getColorization (ColorPository colorpos)
    {
      ClassRecord crec = colorpos.getClassRecord(clazz);
      Colorization src = colorpos.getColorization(colorization);
      return (src == null || crec == null)
          ? null
          : new CustomOffsetsColorization(crec, src);
    }

    /** Basic constructor needed explicitly when there are others, thanks Java. */
    public TransNormal () {}

    /** Convenience for editor use: convert normal. */
    public TransNormal (Normal normal)
    {
      this.clazz = normal.colorization >> 8;
      this.colorization = normal.colorization;
    }

    public TransNormal (Translated lated)
    {
      this.clazz = lated.clazz;
      if (lated.source instanceof Normal) {
        this.colorization = ((Normal)lated.source).colorization;
      }
    }

    /** Convenience for editor use: switch from a luma-tint (same fields). */
    public TransNormal (LumaTint luma)
    {
      this.clazz = luma.clazz;
      this.colorization = luma.colorization;
    }
  }

  /**
   */
  public static class LumaTint extends ColorizationConfig
  {
    /** The colorization class whose painted region we recolor (selects which pixels match). */
    @Editable(editor="colorization", mode="class", hgroup="c")
    public int clazz;

    /** The colorization whose resolved color sits at the pivot (the midtone). */
    @Editable(editor="colorization", hgroup="c")
    public int colorization;

    /** Color the shadow end ramps toward (default black = neutral darkening; a warm dark color
     * gives the SK warm-shadow look). */
    @Editable
    public Color4f shadowTint = new Color4f(0f, 0f, 0f, 1f);

    /** Color the highlight end ramps toward (default white). */
    @Editable
    public Color4f highlightTint = new Color4f(1f, 1f, 1f, 1f);

    @Override
    public Colorization getColorization (ColorPository colorpos)
    {
      ClassRecord crec = colorpos.getClassRecord(clazz);
      Colorization src = colorpos.getColorization(colorization);
      return (src == null || crec == null)
          ? null
          : new LumaTintColorization(crec, src, shadowTint.getColor(), highlightTint.getColor());
    }

    /** Basic constructor needed explicitly when there are others, thanks Java. */
    public LumaTint () {}

    /** Convenience for editor use: switch from a TransNormal (same fields). */
    public LumaTint (TransNormal trans)
    {
      this.clazz = trans.clazz;
      this.colorization = trans.colorization;
    }
  }

  /**
   * Represents a set of hue, saturation, and value values.
   */
  public static class Triplet extends DeepObject
    implements Exportable, Streamable
  {
    /** The hue, saturation, and value offsets. */
    @Editable(min=-1.0, max=+1.0, step=0.001, width=5, hgroup="v")
    public float hue, saturation, value;

    /**
     * Returns a float array containing the triplet values.
     */
    public float[] getValues ()
    {
      return new float[] { hue, saturation, value };
    }
  }

  /**
   * A colorization that uses a pository class and custom offsets.
   */
  public static class CustomOffsetsColorization extends Colorization
  {
    /**
     * Creates a new custom offsets colorization.
     */
    public CustomOffsetsColorization (ClassRecord crec, float[] offsets)
    {
      super(crec.classId << 8, crec, offsets);
    }

    /**
     * Create a custom colorization as translated from another colorization.
     */
    public CustomOffsetsColorization (ClassRecord target, Colorization source)
    {
      this(target, new float[3]);
      for (int ii = 0; ii < 3; ii++) {
        this.offsets[ii] = source.offsets[ii] + (source.getRootHsv(ii) - _hsv[ii]);
      }
    }

    @Override
    public int hashCode ()
    {
      return super.hashCode() ^ Arrays.hashCode(offsets);
    }

    @Override
    public boolean equals (Object other)
    {
      // getClass(), not instanceof: a hue-locking subclass recolors differently from the additive
      // base even with identical offsets, so the two must never collide as image-cache keys.
      return other != null && getClass() == other.getClass() &&
        super.equals(other) && Arrays.equals(offsets, ((Colorization)other).offsets);
    }
  }

  /**
   * The source pixel's lightness picks a
   * point on the ramp {@code shadowTint -> target -> highlightTint}, pivoted at the region root's
   * lightness, so the artist controls exactly which colors shadows and highlights tend toward.
   */
  public static class LumaTintColorization extends CustomOffsetsColorization
  {
    public LumaTintColorization (
        ClassRecord region, Colorization source, Color shadowTint, Color highlightTint)
    {
      super(region, source);
      Color target = source.getColorizedRoot();
      _tr = target.getRed();        _tg = target.getGreen();        _tb = target.getBlue();
      _sr = shadowTint.getRed();    _sg = shadowTint.getGreen();    _sb = shadowTint.getBlue();
      _hr = highlightTint.getRed(); _hg = highlightTint.getGreen(); _hb = highlightTint.getBlue();
      _rootL = _hsv[2] * (1f - _hsv[1] / 2f);
    }

    @Override
    public int recolorColor (float[] hsv)
    {
      float lightness = hsv[2] * (1f - hsv[1] / 2f);
      if (lightness >= _rootL) {
        // lighter than the root: ramp target -> highlightTint
        float t = (_rootL < 1f) ? (lightness - _rootL) / (1f - _rootL) : 0f;
        return 0xFF000000 | (lerp(_tr, _hr, t) << 16) | (lerp(_tg, _hg, t) << 8) | lerp(_tb, _hb, t);
      }
      // darker than the root: ramp target -> shadowTint
      float t = (_rootL > 0f) ? (1f - lightness / _rootL) : 0f;
      return 0xFF000000 | (lerp(_tr, _sr, t) << 16) | (lerp(_tg, _sg, t) << 8) | lerp(_tb, _sb, t);
    }

    private static int lerp (int from, int to, float t)
    {
      return Math.round(from + (to - from) * t);
    }

    @Override
    public int hashCode ()
    {
      return super.hashCode() ^ ((_sr << 16) | (_sg << 8) | _sb) ^
        Integer.rotateLeft((_hr << 16) | (_hg << 8) | _hb, 1);
    }

    @Override
    public boolean equals (Object other)
    {
      if (!super.equals(other)) {   // already requires an identical runtime class
        return false;
      }
      LumaTintColorization o = (LumaTintColorization)other;
      return _sr == o._sr && _sg == o._sg && _sb == o._sb &&
        _hr == o._hr && _hg == o._hg && _hb == o._hb;
    }

    /** Target (RGB) at the pivot, the shadow and highlight tint RGBs, and the lightness pivot. */
    protected int _tr, _tg, _tb, _sr, _sg, _sb, _hr, _hg, _hb;
    protected float _rootL;
  }

  /**
   * A fully custom colorization.
   */
  public static class FullyCustomColorization extends Colorization
  {
    /**
     * Creates a new fully custom colorization.
     */
    public FullyCustomColorization (Color source, float[] range, float[] offsets)
    {
      super(0, source, range, offsets);
    }

    @Override
    public int hashCode ()
    {
      return super.hashCode() ^ Arrays.hashCode(offsets);
    }

    @Override
    public boolean equals (Object other)
    {
      Colorization ozation;
      return super.equals(other) &&
        (ozation = (Colorization)other).rootColor.equals(rootColor) &&
        Arrays.equals(ozation.range, range) && Arrays.equals(ozation.offsets, offsets);
    }
  }

  /**
   * Returns the colorization for this config.
   * This method used to be abstract but now it's final to signal that it defers to the other.
   * We could just remove it, TBH.
   */
  public final Colorization getColorization (GlContext ctx)
  {
    return getColorization(ctx.getColorPository());
  }

  /**
   * Returns the colorization for this config.
   */
  public abstract Colorization getColorization (ColorPository colorpos);
}
