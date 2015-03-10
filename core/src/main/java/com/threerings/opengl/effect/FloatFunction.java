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

import com.threerings.math.FloatMath;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Provides a float value based on a parameter ranging from 0 to 1.
 */
@EditorTypes({
    FloatFunction.Constant.class, FloatFunction.Linear.class,
    FloatFunction.InAndOut.class, FloatFunction.ThreePoint.class,
    FloatFunction.Multipoint.class })
public abstract class FloatFunction extends DeepObject
    implements Exportable
{
    /**
     * Returns a constant value.
     */
    public static class Constant extends FloatFunction
    {
        /** The value to return. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float value;

        /**
         * Creates a constant function with the specified value.
         */
        public Constant (float value)
        {
            this.value = value;
        }

        /**
         * Creates a constant function with the specified function's middle.
         */
        public Constant (FloatFunction function)
        {
            value = function.getValue(0.5f);
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Constant ()
        {
        }

        @Override
        public float getValue (float t)
        {
            return value;
        }

        @Override
        public FloatFunction copy (FloatFunction result)
        {
            Constant cresult = (result instanceof Constant) ? ((Constant)result) : new Constant();
            cresult.value = value;
            return cresult;
        }
    }

    /**
     * Linearly interpolates between a start and an end value.
     */
    public static class Linear extends FloatFunction
    {
        /** The starting value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float start;

        /** The final value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float end;

        /** The type of easing to use. */
        @Editable
        public Easing easing = new Easing.None();

        /**
         * Creates a function to blend between the specified values.
         */
        public Linear (float start, float end)
        {
            this.start = start;
            this.end = end;
        }

        /**
         * Creates a linear function with the specified function's middle.
         */
        public Linear (FloatFunction function)
        {
            start = end = function.getValue(0.5f);
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Linear ()
        {
        }

        @Override
        public float getValue (float t)
        {
            return start + easing.getTime(t)*(end - start);
        }

        @Override
        public FloatFunction copy (FloatFunction result)
        {
            Linear lresult = (result instanceof Linear) ? ((Linear)result) : new Linear();
            lresult.start = start;
            lresult.end = end;
            lresult.easing = easing.copy(lresult.easing);
            return lresult;
        }
    }

    /**
     * A float function that blends linearly from a start value to an end value, then back to
     * the start value.
     */
    public static class InAndOut extends FloatFunction
    {
        /** The starting and ending values. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float start;

        /** The target value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float end;

        /** The proportional time to spend blending into the target value. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public float in = 0.25f;

        /** The proportional time to spend blending back to the start value. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public float out = 0.25f;

        /**
         * Creates an in-and-out function with the other function's start and end.
         */
        public InAndOut (FloatFunction function)
        {
            start = function.getValue(0f);
            end = function.getValue(1f);
        }

        /**
         * Creates an in-and-out function with the other function's parameters.
         */
        public InAndOut (ThreePoint function)
        {
            start = function.start;
            end = function.middle;
            in = function.in;
            out = function.out;
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public InAndOut ()
        {
        }

        @Override
        public float getValue (float t)
        {
            if (t < in) {
                return FloatMath.lerp(start, end, t / in);
            } else if (t <= 1f - out) {
                return end;
            } else {
                return FloatMath.lerp(start, end, (1f - t) / out);
            }
        }

        @Override
        public FloatFunction copy (FloatFunction result)
        {
            InAndOut iresult = (result instanceof InAndOut) ? ((InAndOut)result) : new InAndOut();
            iresult.start = start;
            iresult.end = end;
            iresult.in = in;
            iresult.out = out;
            return iresult;
        }
    }

    /**
     * A float function that blends linearly from a start value to a middle value, then to an end
     * value.
     */
    public static class ThreePoint extends FloatFunction
    {
        /** The starting value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float start;

        /** The middle value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float middle;

        /** The end value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float end;

        /** The proportional time to spend blending into the middle value. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public float in = 0.25f;

        /** The proportional time to spend blending into the end value. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public float out = 0.25f;

        /**
         * Creates a three point function with the other function's start, middle, and end.
         */
        public ThreePoint (FloatFunction function)
        {
            start = function.getValue(0f);
            middle = function.getValue(0.5f);
            end = function.getValue(1f);
        }

        /**
         * Creates a three point function with the other function's parameters.
         */
        public ThreePoint (InAndOut function)
        {
            start = function.start;
            middle = function.end;
            end = function.start;
            in = function.in;
            out = function.out;
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public ThreePoint ()
        {
        }

        @Override
        public float getValue (float t)
        {
            if (t < in) {
                return FloatMath.lerp(start, middle, t / in);
            } else if (t <= 1f - out) {
                return middle;
            } else {
                return FloatMath.lerp(end, middle, (1f - t) / out);
            }
        }

        @Override
        public FloatFunction copy (FloatFunction result)
        {
            ThreePoint tresult = (result instanceof ThreePoint) ?
                ((ThreePoint)result) : new ThreePoint();
            tresult.start = start;
            tresult.middle = middle;
            tresult.end = end;
            tresult.in = in;
            tresult.out = out;
            return tresult;
        }
    }

    /**
     * A float function that blends between an arbitrary number of values.
     */
    public static class Multipoint extends FloatFunction
    {
        /**
         * A single point to blend between.
         */
        public static class Point extends DeepObject
            implements Exportable
        {
            /** The value of the point. */
            @Editable(
                min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
                step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
            public float value;

            /** The time offset of the point. */
            @Editable(min=0.0, max=1.0, step=0.01)
            public float offset = 0.25f;

            /**
             * Creates a new point with the given value and offset.
             */
            public Point (float value, float offset)
            {
                this.value = value;
                this.offset = offset;
            }

            /**
             * No-arg constructor for deserialization, etc.
             */
            public Point ()
            {
            }

            /**
             * Copies the fields of another point.
             */
            public void set (Point other)
            {
                value = other.value;
                offset = other.offset;
            }
        }

        /** The starting value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float start;

        /** The entries in between the start and end. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public Point[] middle = new Point[0];

        /** The final value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public float end;

        /**
         * Creates a multipoint function with the other function's start and end.
         */
        public Multipoint (FloatFunction function)
        {
            start = function.getValue(0f);
            end = function.getValue(1f);
        }

        /**
         * Creates a multipoint function with the other function's parameters.
         */
        public Multipoint (InAndOut function)
        {
            start = function.start;
            middle = new Point[] {
                new Point(function.end, function.in),
                new Point(function.end, 1f - function.in - function.out) };
            end = function.start;
        }

        /**
         * Creates a multipoint function with the other function's parameters.
         */
        public Multipoint (ThreePoint function)
        {
            start = function.start;
            middle = new Point[] {
                new Point(function.middle, function.in),
                new Point(function.middle, 1f - function.in - function.out) };
            end = function.end;
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Multipoint ()
        {
        }

        @Override
        public float getValue (float t)
        {
            float last = start;
            float remaining = 1f;
            for (Point point : middle) {
                if (t < point.offset) {
                    return FloatMath.lerp(last, point.value, t / point.offset);
                }
                t -= point.offset;
                remaining -= point.offset;
                last = point.value;
            }
            return FloatMath.lerp(last, end, t / remaining);
        }

        @Override
        public FloatFunction copy (FloatFunction result)
        {
            Multipoint mresult = (result instanceof Multipoint) ?
                ((Multipoint)result) : new Multipoint();
            mresult.start = start;
            if (mresult.middle.length != middle.length) {
                Point[] oarray = mresult.middle;
                mresult.middle = new Point[middle.length];
                int len = Math.min(oarray.length, middle.length);
                System.arraycopy(oarray, 0, mresult.middle, 0, len);
                for (int ii = len; ii < middle.length; ii++) {
                    mresult.middle[ii] = new Point();
                }
            }
            for (int ii = 0; ii < middle.length; ii++) {
                mresult.middle[ii].set(middle[ii]);
            }
            mresult.end = end;
            return mresult;
        }
    }

    /**
     * Computes the value at the specified time.
     */
    public abstract float getValue (float t);

    /**
     * Copies this function.
     *
     * @param result an object to repopulate, if possible.
     * @return either the result object, if it could be repopulated, or a new object containing the
     * result.
     */
    public abstract FloatFunction copy (FloatFunction result);
}
