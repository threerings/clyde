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

package com.threerings.probs;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.math.FloatMath;

import com.threerings.opengl.renderer.Color4f;

/**
 * A color-valued random variable.
 */
@EditorTypes(value={
    ColorVariable.Constant.class,
    ColorVariable.Uniform.class }, label="distribution")
public abstract class ColorVariable extends DeepObject
    implements Exportable, Streamable
{
    /**
     * Always returns the same value.
     */
    public static class Constant extends ColorVariable
    {
        /** The value to return. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f value = new Color4f();

        /**
         * Creates a constant variable from the parameters of the other variable.
         */
        public Constant (ColorVariable variable)
        {
            variable.getMean(value);
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Constant ()
        {
        }

        @Override
        public Color4f getValue (Color4f result)
        {
            return result.set(value);
        }

        @Override
        public Color4f getMean (Color4f result)
        {
            return result.set(value);
        }
    }

    /**
     * Returns values uniformly distributed in RGBA space.
     */
    public static class Uniform extends ColorVariable
    {
        /** The minimum extent. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f minimum = new Color4f();

        /** The maximum extent. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f maximum = new Color4f();

        /**
         * Creates a uniform variable from the parameters of the other variable.
         */
        public Uniform (ColorVariable variable)
        {
            maximum.set(variable.getMean(minimum));
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Uniform ()
        {
        }

        @Override
        public Color4f getValue (Color4f result)
        {
            return result.set(
                FloatMath.random(minimum.r, maximum.r),
                FloatMath.random(minimum.g, maximum.g),
                FloatMath.random(minimum.b, maximum.b),
                FloatMath.random(minimum.a, maximum.a));
        }

        @Override
        public Color4f getMean (Color4f result)
        {
            return minimum.add(maximum, result).multLocal(0.5f);
        }
    }

    /**
     * Computes a sample value according to the variable's distribution.
     *
     * @return a reference to the result value, for chaining.
     */
    public abstract Color4f getValue (Color4f result);

    /**
     * Computes the mean value.
     *
     * @return a reference to the result value, for chaining.
     */
    public abstract Color4f getMean (Color4f result);
}
