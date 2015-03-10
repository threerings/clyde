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
import com.threerings.opengl.effect.ColorFunction;

/**
 * A {@link ColorFunction} random variable.
 */
@EditorTypes(value={
    ColorFunctionVariable.Fixed.class,
    ColorFunctionVariable.VariableConstant.class,
    ColorFunctionVariable.VariableLinear.class }, label="mode")
public abstract class ColorFunctionVariable extends DeepObject
    implements Exportable, Streamable
{
    /**
     * Simply returns the same function.
     */
    public static class Fixed extends ColorFunctionVariable
    {
        /** The function to return. */
        @Editable(mode=Editable.INHERIT_STRING)
        public ColorFunction function = new ColorFunction.Constant();

        /**
         * Creates a fixed variable with the specified function.
         */
        public Fixed (ColorFunction function)
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
        public ColorFunction getValue (ColorFunction result)
        {
            return function.copy(result);
        }
    }

    /**
     * Returns a constant function with a variable value.
     */
    public static class VariableConstant extends ColorFunctionVariable
    {
        /** The value to return. */
        @Editable(mode=Editable.INHERIT_STRING)
        public ColorVariable value = new ColorVariable.Constant();

        @Override
        public ColorFunction getValue (ColorFunction result)
        {
            ColorFunction.Constant cresult = (result instanceof ColorFunction.Constant) ?
                ((ColorFunction.Constant)result) : new ColorFunction.Constant();
            value.getValue(cresult.value);
            return cresult;
        }
    }

    /**
     * Returns a linear function with independent variable start and end values.
     */
    public static class VariableLinear extends ColorFunctionVariable
    {
        /** The value to return. */
        @Editable(mode=Editable.INHERIT_STRING)
        public ColorVariable start = new ColorVariable.Constant();

        /** The value to return. */
        @Editable(mode=Editable.INHERIT_STRING)
        public ColorVariable end = new ColorVariable.Constant();

        /** The type of easing to use. */
        @Editable
        public Easing easing = new Easing.None();

        @Override
        public ColorFunction getValue (ColorFunction result)
        {
            ColorFunction.Linear lresult = (result instanceof ColorFunction.Linear) ?
                ((ColorFunction.Linear)result) : new ColorFunction.Linear();
            start.getValue(lresult.start);
            end.getValue(lresult.end);
            lresult.easing = easing.copy(lresult.easing);
            return lresult;
        }
    }

    /**
     * Computes a sample value according to the variable's distribution.
     *
     * @param result an object to repopulate, if possible.
     * @return either the result object, if it could be repopulated, or a new object
     * containing the result.
     */
    public abstract ColorFunction getValue (ColorFunction result);
}
