//
// $Id$

package com.threerings.probs;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.math.FloatMath;

/**
 * A floating point random variable.
 */
public abstract class FloatVariable extends DeepObject
    implements Exportable
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

        @Override // documentation inherited
        public float getValue ()
        {
            return value;
        }

        @Override // documentation inherited
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

        @Override // documentation inherited
        public float getValue ()
        {
            return FloatMath.random(minimum, maximum);
        }

        @Override // documentation inherited
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

        @Override // documentation inherited
        public float getValue ()
        {
            return FloatMath.normal(mean, stddev);
        }

        @Override // documentation inherited
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

        @Override // documentation inherited
        public float getValue ()
        {
            return FloatMath.exponential(mean);
        }

        @Override // documentation inherited
        public float getMean ()
        {
            return mean;
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
        return new Class[] { Constant.class, Uniform.class, Normal.class, Exponential.class };
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
