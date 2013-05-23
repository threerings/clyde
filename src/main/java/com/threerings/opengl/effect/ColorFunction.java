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

import com.threerings.opengl.renderer.Color4f;

/**
 * Provides a color based on a parameter ranging from 0 to 1.
 */
@EditorTypes({
    ColorFunction.Constant.class, ColorFunction.Linear.class,
    ColorFunction.InAndOut.class, ColorFunction.ThreePoint.class,
    ColorFunction.Multipoint.class })
public abstract class ColorFunction extends DeepObject
    implements Exportable, Streamable
{
    /**
     * A color function that always returns the same color.
     */
    public static class Constant extends ColorFunction
    {
        /** The constant value. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f value = new Color4f(1f, 1f, 1f, 1f);

        /**
         * Creates a constant function with the other function's middle.
         */
        public Constant (ColorFunction function)
        {
            function.getValue(0.5f, value);
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Constant ()
        {
        }

        @Override
        public Color4f getValue (float t, Color4f result)
        {
            return result.set(value);
        }

        @Override
        public ColorFunction copy (ColorFunction result)
        {
            Constant cresult = (result instanceof Constant) ? ((Constant)result) : new Constant();
            cresult.value.set(value);
            return cresult;
        }
    }

    /**
     * A color function that blends linearly between two colors.
     */
    public static class Linear extends ColorFunction
    {
        /** The starting color. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f start = new Color4f(1f, 1f, 1f, 1f);

        /** The final color. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f end = new Color4f(1f, 1f, 1f, 0f);

        /** The easing function. */
        @Editable
        public Easing easing = new Easing.None();

        /**
         * Creates a linear function with the other function's start and end.
         */
        public Linear (ColorFunction function)
        {
            function.getValue(0f, start);
            function.getValue(1f, end);
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Linear ()
        {
        }

        @Override
        public Color4f getValue (float t, Color4f result)
        {
            return start.lerp(end, easing.getTime(t), result);
        }

        @Override
        public ColorFunction copy (ColorFunction result)
        {
            Linear lresult = (result instanceof Linear) ? ((Linear)result) : new Linear();
            lresult.start.set(start);
            lresult.end.set(end);
            lresult.easing = easing.copy(lresult.easing);
            return lresult;
        }
    }

    /**
     * A color function that blends linearly from a start color to an end color, then back to
     * the start color.
     */
    public static class InAndOut extends ColorFunction
    {
        /** The starting and ending color. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f start = new Color4f(1f, 1f, 1f, 0f);

        /** The target color. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f end = new Color4f(1f, 1f, 1f, 1f);

        /** The proportional time to spend blending into the target color. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public float in = 0.25f;

        /** The proportional time to spend blending back to the start color. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public float out = 0.25f;

        /**
         * Creates an in-and-out function with the other function's start and end.
         */
        public InAndOut (ColorFunction function)
        {
            function.getValue(0f, start);
            function.getValue(1f, end);
        }

        /**
         * Creates an in-and-out function with the other function's parameters.
         */
        public InAndOut (ThreePoint function)
        {
            start.set(function.start);
            end.set(function.middle);
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
        public Color4f getValue (float t, Color4f result)
        {
            if (t < in) {
                return start.lerp(end, t / in, result);
            } else if (t <= 1f - out) {
                return result.set(end);
            } else {
                return start.lerp(end, (1f - t) / out, result);
            }
        }

        @Override
        public ColorFunction copy (ColorFunction result)
        {
            InAndOut iresult = (result instanceof InAndOut) ? ((InAndOut)result) : new InAndOut();
            iresult.start.set(start);
            iresult.end.set(end);
            iresult.in = in;
            iresult.out = out;
            return iresult;
        }
    }

    /**
     * A color function that blends linearly from a start color to a middle color, then to an end
     * color.
     */
    public static class ThreePoint extends ColorFunction
    {
        /** The starting color. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f start = new Color4f(1f, 1f, 1f, 0f);

        /** The middle color. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f middle = new Color4f(1f, 1f, 1f, 1f);

        /** The end color. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f end = new Color4f(1f, 1f, 1f, 0f);

        /** The proportional time to spend blending into the middle color. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public float in = 0.25f;

        /** The proportional time to spend blending into the end color. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public float out = 0.25f;

        /**
         * Creates a three point function with the other function's start, middle, and end.
         */
        public ThreePoint (ColorFunction function)
        {
            function.getValue(0f, start);
            function.getValue(0.5f, middle);
            function.getValue(1f, end);
        }

        /**
         * Creates a three point function with the other function's parameters.
         */
        public ThreePoint (InAndOut function)
        {
            start.set(function.start);
            middle.set(function.end);
            end.set(function.start);
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
        public Color4f getValue (float t, Color4f result)
        {
            if (t < in) {
                return start.lerp(middle, t / in, result);
            } else if (t <= 1f - out) {
                return result.set(middle);
            } else {
                return end.lerp(middle, (1f - t) / out, result);
            }
        }

        @Override
        public ColorFunction copy (ColorFunction result)
        {
            ThreePoint tresult = (result instanceof ThreePoint) ?
                ((ThreePoint)result) : new ThreePoint();
            tresult.start.set(start);
            tresult.middle.set(middle);
            tresult.end.set(end);
            tresult.in = in;
            tresult.out = out;
            return tresult;
        }
    }

    /**
     * A color function that blends between an arbitrary number of colors.
     */
    public static class Multipoint extends ColorFunction
    {
        /**
         * A single point to blend between.
         */
        public static class Point
            implements Exportable, Streamable
        {
            /** The color of the point. */
            @Editable(mode=Editable.INHERIT_STRING)
            public Color4f color = new Color4f(1f, 1f, 1f, 1f);

            /** The time offset of the point. */
            @Editable(min=0.0, max=1.0, step=0.01)
            public float offset = 0.25f;

            /**
             * Creates a new point with the given color and offset.
             */
            public Point (Color4f color, float offset)
            {
                this.color.set(color);
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
                color.set(other.color);
                offset = other.offset;
            }
        }

        /** The starting color. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f start = new Color4f(1f, 1f, 1f, 1f);

        /** The entries in between the start and end. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Point[] middle = new Point[0];

        /** The final color. */
        @Editable(mode=Editable.INHERIT_STRING)
        public Color4f end = new Color4f(1f, 1f, 1f, 0f);

        /**
         * Creates a multipoint function with the other function's start and end.
         */
        public Multipoint (ColorFunction function)
        {
            function.getValue(0f, start);
            function.getValue(1f, end);
        }

        /**
         * Creates a multipoint function with the other function's parameters.
         */
        public Multipoint (InAndOut function)
        {
            start.set(function.start);
            middle = new Point[] {
                new Point(function.end, function.in),
                new Point(function.end, 1f - function.in - function.out) };
            end.set(function.start);
        }

        /**
         * Creates a multipoint function with the other function's parameters.
         */
        public Multipoint (ThreePoint function)
        {
            start.set(function.start);
            middle = new Point[] {
                new Point(function.middle, function.in),
                new Point(function.middle, 1f - function.in - function.out) };
            end.set(function.end);
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Multipoint ()
        {
        }

        @Override
        public Color4f getValue (float t, Color4f result)
        {
            Color4f last = start;
            float remaining = 1f;
            for (Point point : middle) {
                if (t < point.offset) {
                    return last.lerp(point.color, t / point.offset, result);
                }
                t -= point.offset;
                remaining -= point.offset;
                last = point.color;
            }
            return last.lerp(end, t / remaining, result);
        }

        @Override
        public ColorFunction copy (ColorFunction result)
        {
            Multipoint mresult = (result instanceof Multipoint) ?
                ((Multipoint)result) : new Multipoint();
            mresult.start.set(start);
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
            mresult.end.set(end);
            return mresult;
        }
    }

    /**
     * Computes the value at the specified time.
     *
     * @return a reference to the result color, for chaining.
     */
    public abstract Color4f getValue (float t, Color4f result);

    /**
     * Copies this function.
     *
     * @param result an object to repopulate, if possible.
     * @return either the result object, if it could be repopulated, or a new object containing the
     * result.
     */
    public abstract ColorFunction copy (ColorFunction result);
}
