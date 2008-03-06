//
// $Id$

package com.threerings.opengl.effect;

import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Represents the type of easing to use, which affects the time parameter.  These functions mimic
 * the ones by <a href="http://www.robertpenner.com/easing/">Robert Penner</a> included in the Flex
 * API.  TODO: Allow control over the amount/duration of easing?
 */
public abstract class Easing extends DeepObject
    implements Exportable
{
    /**
     * Performs no easing.
     */
    public static class None extends Easing
    {
        @Override // documentation inherited
        public float getTime (float t)
        {
            return t;
        }

        @Override // documentation inherited
        public Easing copy (Easing result)
        {
            return (result instanceof None) ? result : new None();
        }
    }

    /**
     * Performs a simple quadratic ease-in.
     */
    public static class QuadraticIn extends Easing
    {
        @Override // documentation inherited
        public float getTime (float t)
        {
            return t*t;
        }

        @Override // documentation inherited
        public Easing copy (Easing result)
        {
            return (result instanceof QuadraticIn) ? result : new QuadraticIn();
        }
    }

    /**
     * Performs a simple quadratic ease-out.
     */
    public static class QuadraticOut extends Easing
    {
        @Override // documentation inherited
        public float getTime (float t)
        {
            return t*(2f - t);
        }

        @Override // documentation inherited
        public Easing copy (Easing result)
        {
            return (result instanceof QuadraticOut) ? result : new QuadraticOut();
        }
    }

    /**
     * Performs a simple ease-in and a simple ease-out.
     */
    public static class QuadraticInAndOut extends Easing
    {
        @Override // documentation inherited
        public float getTime (float t)
        {
            return (t <= 0.5f) ? (2f*t*t) : (2f*t*(2f - t) - 1f);
        }

        @Override // documentation inherited
        public Easing copy (Easing result)
        {
            return (result instanceof QuadraticInAndOut) ? result : new QuadraticInAndOut();
        }
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] {
            None.class, QuadraticIn.class, QuadraticOut.class, QuadraticInAndOut.class };
    }

    /**
     * Computes the eased time based on the provided linear parameter.
     */
    public abstract float getTime (float t);

    /**
     * Copies this easing function.
     *
     * @param result an object to repopulate, if possible.
     * @return either the result object, if it could be repopulated, or a new object containing the
     * result.
     */
    public abstract Easing copy (Easing result);
}
