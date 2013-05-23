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
import com.threerings.math.Quaternion;
import com.threerings.math.Vector3f;

/**
 * A quaternion-valued random variable.
 */
@EditorTypes(value={
    QuaternionVariable.Identity.class, QuaternionVariable.Constant.class,
    QuaternionVariable.Uniform.class, QuaternionVariable.Random.class },
    label="distribution")
public abstract class QuaternionVariable extends DeepObject
    implements Exportable, Streamable
{
    /**
     * Always return the identity value.
     */
    public static class Identity extends QuaternionVariable
    {
        @Override
        public Quaternion getValue (Quaternion result)
        {
            return result.set(Quaternion.IDENTITY);
        }

        @Override
        public Quaternion getMean (Quaternion result)
        {
            return result.set(Quaternion.IDENTITY);
        }
    }

    /**
     * Always returns the same value.
     */
    public static class Constant extends QuaternionVariable
    {
        /** The value to return. */
        @Editable
        public Quaternion value = new Quaternion();

        /**
         * Creates a constant variable from the parameters of the other variable.
         */
        public Constant (QuaternionVariable variable)
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
        public Quaternion getValue (Quaternion result)
        {
            return result.set(value);
        }

        @Override
        public Quaternion getMean (Quaternion result)
        {
            return result.set(value);
        }
    }

    /**
     * Returns a uniformly distributed value.
     */
    public static class Uniform extends QuaternionVariable
    {
        /** The minimum angles. */
        @Editable(hgroup="range", mode="angles")
        public Vector3f minimum = new Vector3f();

        /** The maximum angles. */
        @Editable(hgroup="range", mode="angles")
        public Vector3f maximum = new Vector3f();

        /**
         * Creates a uniform variable from the parameters of the other variable.
         */
        public Uniform (QuaternionVariable variable)
        {
            Quaternion mean = variable.getMean(new Quaternion());
            maximum.set(mean.toAngles(minimum));
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Uniform ()
        {
        }

        @Override
        public Quaternion getValue (Quaternion result)
        {
            // pick angles according to the surface area distribution
            return result.fromAngles(
                FloatMath.random(minimum.x, maximum.x),
                FloatMath.asin(
                    FloatMath.random(FloatMath.sin(minimum.y), FloatMath.sin(maximum.y))),
                FloatMath.random(minimum.z, maximum.z));
        }

        @Override
        public Quaternion getMean (Quaternion result)
        {
            return result.fromAngles(
                (minimum.x + maximum.x) * 0.5f,
                FloatMath.asin((FloatMath.sin(minimum.y) + FloatMath.sin(maximum.y)) * 0.5f),
                (minimum.z + maximum.z) * 0.5f);
        }
    }

    /**
     * Returns a totally random rotation.
     */
    public static class Random extends QuaternionVariable
    {
        @Override
        public Quaternion getValue (Quaternion result)
        {
            return result.randomize();
        }

        @Override
        public Quaternion getMean (Quaternion result)
        {
            return result.set(Quaternion.IDENTITY);
        }
    }

    /**
     * Computes a sample value according to the variable's distribution.
     *
     * @return a reference to the result value, for chaining.
     */
    public abstract Quaternion getValue (Quaternion result);

    /**
     * Computes the mean value.
     *
     * @return a reference to the result value, for chaining.
     */
    public abstract Quaternion getMean (Quaternion result);
}
