//
// $Id$

package com.threerings.probs;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;
import com.threerings.math.Vector3f;

/**
 * A quaternion-valued random variable.
 */
public abstract class QuaternionVariable extends DeepObject
    implements Exportable
{
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

        @Override // documentation inherited
        public Quaternion getValue (Quaternion result)
        {
            return result.set(value);
        }

        @Override // documentation inherited
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

        @Override // documentation inherited
        public Quaternion getValue (Quaternion result)
        {
            // pick angles according to the surface area distribution
            return result.fromAngles(
                FloatMath.random(minimum.x, maximum.x),
                FloatMath.asin(
                    FloatMath.random(FloatMath.sin(minimum.y), FloatMath.sin(maximum.y))),
                FloatMath.random(minimum.z, maximum.z));
        }

        @Override // documentation inherited
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
        @Override // documentation inherited
        public Quaternion getValue (Quaternion result)
        {
            return result.randomize();
        }

        @Override // documentation inherited
        public Quaternion getMean (Quaternion result)
        {
            return result.set(Quaternion.IDENTITY);
        }
    }

    /**
     * Returns the translatable label to use in the editor for selecting subtypes.
     */
    public static String getEditorTypeLabel ()
    {
        return "distribution";
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { Constant.class, Uniform.class, Random.class };
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
