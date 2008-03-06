//
// $Id$

package com.threerings.probs;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.math.FloatMath;

import com.threerings.opengl.renderer.Color4f;

/**
 * A color-valued random variable.
 */
public abstract class ColorVariable extends DeepObject
    implements Exportable
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

        @Override // documentation inherited
        public Color4f getValue (Color4f result)
        {
            return result.set(value);
        }

        @Override // documentation inherited
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

        @Override // documentation inherited
        public Color4f getValue (Color4f result)
        {
            return result.set(
                FloatMath.random(minimum.r, maximum.r),
                FloatMath.random(minimum.g, maximum.g),
                FloatMath.random(minimum.b, maximum.b),
                FloatMath.random(minimum.a, maximum.a));
        }

        @Override // documentation inherited
        public Color4f getMean (Color4f result)
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
    public abstract Color4f getValue (Color4f result);

    /**
     * Computes the mean value.
     *
     * @return a reference to the result value, for chaining.
     */
    public abstract Color4f getMean (Color4f result);
}
