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

import com.threerings.opengl.effect.Easing;
import com.threerings.opengl.effect.FloatFunction;

/**
 * A {@link FloatFunction} random variable.
 */
@EditorTypes(value={
    FloatFunctionVariable.Fixed.class,
    FloatFunctionVariable.VariableConstant.class,
    FloatFunctionVariable.VariableLinear.class,
    FloatFunctionVariable.VariableInAndOut.class,
    FloatFunctionVariable.VariableThreePoint.class,
    FloatFunctionVariable.VariableMultipoint.class }, label="mode")
public abstract class FloatFunctionVariable extends DeepObject
    implements Exportable, Streamable
{
    /**
     * Simply returns the same function.
     */
    public static class Fixed extends FloatFunctionVariable
    {
        /** The function to return. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatFunction function = new FloatFunction.Constant();

        /**
         * Creates a fixed variable with the specified function.
         */
        public Fixed (FloatFunction function)
        {
            this.function = function;
        }

        /**
         * No-arg constructor for deserialization, etc.
         */
        public Fixed ()
        {
        }

        @Override
        public FloatFunction getValue (FloatFunction result)
        {
            return function.copy(result);
        }
    }

    /**
     * Returns a constant function with a variable value.
     */
    public static class VariableConstant extends FloatFunctionVariable
    {
        /** The value to return. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable value = new FloatVariable.Constant();

        @Override
        public FloatFunction getValue (FloatFunction result)
        {
            FloatFunction.Constant cresult = (result instanceof FloatFunction.Constant) ?
                ((FloatFunction.Constant)result) : new FloatFunction.Constant();
            cresult.value = value.getValue();
            return cresult;
        }
    }

    /**
     * Returns a linear function with independent variable start and end values.
     */
    public static class VariableLinear extends FloatFunctionVariable
    {
        /** The start value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable start = new FloatVariable.Constant();

        /** The end value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable end = new FloatVariable.Constant();

        /** The type of easing to use. */
        @Editable
        public Easing easing = new Easing.None();

        @Override
        public FloatFunction getValue (FloatFunction result)
        {
            FloatFunction.Linear lresult = (result instanceof FloatFunction.Linear) ?
                ((FloatFunction.Linear)result) : new FloatFunction.Linear();
            lresult.start = start.getValue();
            lresult.end = end.getValue();
            lresult.easing = easing.copy(lresult.easing);
            return lresult;
        }
    }

    /**
     * Returns an in-and-out function with independent variable start and end values.
     */
    public static class VariableInAndOut extends FloatFunctionVariable
    {
        /** The start value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable start = new FloatVariable.Constant();

        /** The end value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable end = new FloatVariable.Constant();

        /** The proportional time to spend blending into the middle value. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public FloatVariable in = new FloatVariable.Constant(0.25f);

        /** The proportional time to spend blending into the end value. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public FloatVariable out = new FloatVariable.Constant(0.25f);

        @Override
        public FloatFunction getValue (FloatFunction result)
        {
            FloatFunction.InAndOut ioresult = (result instanceof FloatFunction.InAndOut) ?
                ((FloatFunction.InAndOut)result) : new FloatFunction.InAndOut();
            ioresult.start = start.getValue();
            ioresult.end = end.getValue();
            ioresult.in = in.getValue();
            ioresult.out = out.getValue();
            return ioresult;
        }
    }

    /**
     * Returns a three-point function with independent variable start, middle, and end values.
     */
    public static class VariableThreePoint extends FloatFunctionVariable
    {
        /** The start value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable start = new FloatVariable.Constant();

        /** The middle value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable middle = new FloatVariable.Constant();

        /** The end value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable end = new FloatVariable.Constant();

        /** The proportional time to spend blending into the middle value. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public FloatVariable in = new FloatVariable.Constant(0.25f);

        /** The proportional time to spend blending into the end value. */
        @Editable(min=0.0, max=1.0, step=0.01)
        public FloatVariable out = new FloatVariable.Constant(0.25f);

        @Override
        public FloatFunction getValue (FloatFunction result)
        {
            FloatFunction.ThreePoint tpresult = (result instanceof FloatFunction.ThreePoint) ?
                ((FloatFunction.ThreePoint)result) : new FloatFunction.ThreePoint();
            tpresult.start = start.getValue();
            tpresult.middle = middle.getValue();
            tpresult.end = end.getValue();
            tpresult.in = in.getValue();
            tpresult.out = out.getValue();
            return tpresult;
        }
    }

    /**
     * Returns a multipoint function with independent values.
     */
    public static class VariableMultipoint extends FloatFunctionVariable
    {
        /**
         * A single point to blend between.
         */
        public static class Point extends DeepObject
            implements Exportable, Streamable
        {
            /** The value of the point. */
            @Editable(
                min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
                step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
            public FloatVariable value = new FloatVariable.Constant();

            /** The time offset of the point. */
            @Editable(min=0.0, max=1.0, step=0.01)
            public FloatVariable offset = new FloatVariable.Constant(0.25f);
        }

        /** The start value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable start = new FloatVariable.Constant();

        /** The middle values. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public Point[] middle = new Point[0];

        /** The end value. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable end = new FloatVariable.Constant();

        @Override
        public FloatFunction getValue (FloatFunction result)
        {
            FloatFunction.Multipoint mpresult = (result instanceof FloatFunction.Multipoint) ?
                ((FloatFunction.Multipoint)result) : new FloatFunction.Multipoint();
            mpresult.start = start.getValue();
            if (mpresult.middle.length != middle.length) {
                mpresult.middle = new FloatFunction.Multipoint.Point[middle.length];
                for (int ii = 0; ii < middle.length; ii++) {
                    mpresult.middle[ii] = new FloatFunction.Multipoint.Point();
                }
            }
            for (int ii = 0; ii < middle.length; ii++) {
                Point point = middle[ii];
                FloatFunction.Multipoint.Point mpoint = mpresult.middle[ii];
                mpoint.value = point.value.getValue();
                mpoint.offset = point.offset.getValue();
            }
            mpresult.end = end.getValue();
            return mpresult;
        }
    }

    /**
     * Computes a sample value according to the variable's distribution.
     *
     * @param result an object to repopulate, if possible.
     * @return either the result object, if it could be repopulated, or a new object
     * containing the result.
     */
    public abstract FloatFunction getValue (FloatFunction result);
}
