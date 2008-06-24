//
// $Id$

package com.threerings.probs;

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
    FloatFunctionVariable.VariableLinear.class }, label="mode")
public abstract class FloatFunctionVariable extends DeepObject
    implements Exportable
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

        @Override // documentation inherited
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

        @Override // documentation inherited
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
        /** The value to return. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable start = new FloatVariable.Constant();

        /** The value to return. */
        @Editable(
            min=Editable.INHERIT_DOUBLE, max=Editable.INHERIT_DOUBLE,
            step=Editable.INHERIT_DOUBLE, scale=Editable.INHERIT_DOUBLE)
        public FloatVariable end = new FloatVariable.Constant();

        /** The type of easing to use. */
        @Editable
        public Easing easing = new Easing.None();

        @Override // documentation inherited
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
     * Computes a sample value according to the variable's distribution.
     *
     * @param result an object to repopulate, if possible.
     * @return either the result object, if it could be repopulated, or a new object
     * containing the result.
     */
    public abstract FloatFunction getValue (FloatFunction result);
}
