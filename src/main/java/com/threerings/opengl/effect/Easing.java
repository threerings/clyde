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

package com.threerings.opengl.effect;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Represents the type of easing to use, which affects the time parameter.  These functions mimic
 * the ones by <a href="http://www.robertpenner.com/easing/">Robert Penner</a> included in the Flex
 * API.  TODO: Allow control over the amount/duration of easing?
 */
@EditorTypes({
    Easing.None.class,
    Easing.QuadraticIn.class, Easing.QuadraticOut.class, Easing.QuadraticInAndOut.class,
    Easing.CubicIn.class, Easing.CubicOut.class, Easing.CubicInAndOut.class,
    Easing.CubicInOvershoot.class, Easing.CubicOutOvershoot.class })
public abstract class Easing extends DeepObject
    implements Exportable, Streamable
{
    /**
     * Performs no easing.
     */
    public static class None extends Easing
    {
        @Override
        public float getTime (float t)
        {
            return t;
        }

        @Override
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
        @Override
        public float getTime (float t)
        {
            return t*t;
        }

        @Override
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
        @Override
        public float getTime (float t)
        {
            return t*(2f - t);
        }

        @Override
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
        @Override
        public float getTime (float t)
        {
            return (t <= .5f) ? (2f*t*t) : (2f*t*(2f - t) - 1f);
        }

        @Override
        public Easing copy (Easing result)
        {
            return (result instanceof QuadraticInAndOut) ? result : new QuadraticInAndOut();
        }
    }

    /**
     * Performs a simple cubic ease-in.
     */
    public static class CubicIn extends Easing
    {
        @Override
        public float getTime (float t)
        {
            return t*t*t;
        }

        @Override
        public Easing copy (Easing result)
        {
            return (result instanceof CubicIn) ? result : new CubicIn();
        }
    }

    /**
     * Performs a simple cubic ease-out.
     */
    public static class CubicOut extends Easing
    {
        @Override
        public float getTime (float t)
        {
            t -= 1f;
            return t*t*t + 1f;
        }

        @Override
        public Easing copy (Easing result)
        {
            return (result instanceof CubicOut) ? result : new CubicOut();
        }
    }

    /**
     * Performs a simple cubic ease-in and ease-out.
     */
    public static class CubicInAndOut extends Easing
    {
        @Override
        public float getTime (float t)
        {
            t *= 2f;
            if (t < 1f) {
                return .5f*t*t*t;
            }
            t -= 2f;
            return .5f * (t*t*t + 2f);
        }

        @Override
        public Easing copy (Easing result)
        {
            return (result instanceof CubicInAndOut) ? result : new CubicInAndOut();
        }
    }

    /**
     * Performs a simple in and back ease.
     */
    public static class CubicInOvershoot extends Easing
    {
        /** The amount of overshoot: the default value produces about 10 percent overshoot. */
        @Editable(min=0, step=.00001)
        public float overshoot = 1.70158f;

        @Override
        public float getTime (float t)
        {
            return t*t*((overshoot + 1)*t - overshoot);
        }

        @Override
        public Easing copy (Easing result)
        {
            CubicInOvershoot that = (result instanceof CubicInOvershoot)
                ? (CubicInOvershoot)result
                : new CubicInOvershoot();
            that.overshoot = this.overshoot;
            return that;
        }
    }

    /**
     * Performs a simple cubic ease out with overshoot.
     */
    public static class CubicOutOvershoot extends Easing
    {
        /** The amount of overshoot: the default value produces about 10 percent overshoot. */
        @Editable(min=0, step=.00001)
        public float overshoot = 1.70158f;

        @Override
        public float getTime (float t)
        {
            t -= 1f;
            return t*t*((overshoot + 1)*t + overshoot) + 1;
        }

        @Override
        public Easing copy (Easing result)
        {
            CubicOutOvershoot that = (result instanceof CubicOutOvershoot)
                ? (CubicOutOvershoot)result
                : new CubicOutOvershoot();
            that.overshoot = this.overshoot;
            return that;
        }
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
