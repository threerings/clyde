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

/**
 * A floating point random variable.
 */
@EditorTypes(value={
    FloatVariable.Constant.class,
    FloatVariable.Uniform.class,
    FloatVariable.Normal.class,
    FloatVariable.Exponential.class }, label="distribution")
public abstract class FloatVariable extends DeepObject
    implements Exportable, Streamable
{
    /**
     * Always returns the same value.
     */
    public static class Constant extends FloatVariable
    {
        /** The value to return. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float value;

        /**
         * Creates a constant variable with the specified value.
         */
        public Constant (float value)
        {
            this.value = value;
        }

        /**
         * Creates a constant variable with the specified variable's mean.
         */
        public Constant (FloatVariable variable)
        {
            value = variable.getMean();
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Constant ()
        {
        }

        @Override
        public float getValue ()
        {
            return value;
        }

        @Override
        public float getMean ()
        {
            return value;
        }
    }

    /**
     * Returns a uniformly distributed value.
     */
    public static class Uniform extends FloatVariable
    {
        /** The minimum value to return. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float minimum;

        /** The maximum value to return. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float maximum;

        /**
         * Creates a uniformly distributed variable with the specified minimum and maximum values.
         */
        public Uniform (float minimum, float maximum)
        {
            this.minimum = minimum;
            this.maximum = maximum;
        }

        /**
         * Creates a uniformly distributed variable with the specified variable's mean.
         */
        public Uniform (FloatVariable variable)
        {
            minimum = maximum = variable.getMean();
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Uniform ()
        {
        }

        @Override
        public float getValue ()
        {
            return FloatMath.random(minimum, maximum);
        }

        @Override
        public float getMean ()
        {
            return (minimum + maximum) * 0.5f;
        }
    }

    /**
     * Returns a normally distributed value.
     */
    public static class Normal extends FloatVariable
    {
        /** The mean value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float mean;

        /** The standard deviation. */
        @Editable(
            min=0.0, max=Double.MAX_VALUE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float stddev;

        /**
         * Creates a normally distributed variable with the specified mean and standard deviation.
         */
        public Normal (float mean, float stddev)
        {
            this.mean = mean;
            this.stddev = stddev;
        }

        /**
         * Creates a normally distributed variable with the specified variable's mean.
         */
        public Normal (FloatVariable variable)
        {
            mean = variable.getMean();
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Normal ()
        {
        }

        @Override
        public float getValue ()
        {
            return FloatMath.normal(mean, stddev);
        }

        @Override
        public float getMean ()
        {
            return mean;
        }
    }

    /**
     * Returns an exponentially distributed value.
     */
    public static class Exponential extends FloatVariable
    {
        /** The mean value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float mean;

        /**
         * Creates an exponentially distributed variable with the specified mean.
         */
        public Exponential (float mean)
        {
            this.mean = mean;
        }

        /**
         * Creates an exponentially distributed variable with the specified variable's mean.
         */
        public Exponential (FloatVariable variable)
        {
            mean = variable.getMean();
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Exponential ()
        {
        }

        @Override
        public float getValue ()
        {
            return FloatMath.exponential(mean);
        }

        @Override
        public float getMean ()
        {
            return mean;
        }
    }

    /**
     * Returns a sample value according to the variable's distribution.
     */
    public abstract float getValue ();

    /**
     * Returns the variable's mean.
     */
    public abstract float getMean ();
}
