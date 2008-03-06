//
// $Id$

package com.threerings.probs;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;

/**
 * A vector-valued random variable.
 */
public abstract class VectorVariable extends DeepObject
    implements Exportable
{
    /**
     * Always returns the same value.
     */
    public static class Constant extends VectorVariable
    {
        /** The value to return. */
        @Editable(step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public Vector3f value = new Vector3f();

        /**
         * Creates a constant variable from the parameters of the other variable.
         */
        public Constant (VectorVariable variable)
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
        public Vector3f getValue (Vector3f result)
        {
            return result.set(value);
        }

        @Override // documentation inherited
        public Vector3f getMean (Vector3f result)
        {
            return result.set(value);
        }
    }

    /**
     * Returns a uniformly distributed value.
     */
    public static class Uniform extends VectorVariable
    {
        /** The minimum extent. */
        @Editable(hgroup="range", step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public Vector3f minimum = new Vector3f();

        /** The maximum extent. */
        @Editable(hgroup="range", step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public Vector3f maximum = new Vector3f();

        /**
         * Creates a uniform variable from the parameters of the other variable.
         */
        public Uniform (VectorVariable variable)
        {
            maximum.set(variable.getMean(minimum));
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Uniform ()
        {
        }

        @Override // documentation inherited
        public Vector3f getValue (Vector3f result)
        {
            return result.set(
                FloatMath.random(minimum.x, maximum.x),
                FloatMath.random(minimum.y, maximum.y),
                FloatMath.random(minimum.z, maximum.z));
        }

        @Override // documentation inherited
        public Vector3f getMean (Vector3f result)
        {
            return minimum.add(maximum, result).multLocal(0.5f);
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
        return new Class[] { Constant.class, Uniform.class };
    }

    /**
     * Computes a sample value according to the variable's distribution.
     *
     * @return a reference to the result value, for chaining.
     */
    public abstract Vector3f getValue (Vector3f result);

    /**
     * Computes the mean value.
     *
     * @return a reference to the result value, for chaining.
     */
    public abstract Vector3f getMean (Vector3f result);
}
