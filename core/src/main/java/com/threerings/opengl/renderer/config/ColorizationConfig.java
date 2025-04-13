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
            return super.equals(other) && Arrays.equals(offsets, ((Colorization)other).offsets);
        }
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
