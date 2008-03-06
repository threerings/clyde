//
// $Id$

package com.threerings.probs;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.effect.Easing;
import com.threerings.opengl.effect.ColorFunction;

/**
 * A {@link ColorFunction} random variable.
 */
public abstract class ColorFunctionVariable extends DeepObject
    implements Exportable
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

        @Override // documentation inherited
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

        @Override // documentation inherited
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

        @Override // documentation inherited
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
     * Returns the translatable label to use in the editor for selecting subtypes.
     */
    public static String getEditorTypeLabel ()
    {
        return "mode";
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { Fixed.class, VariableConstant.class, VariableLinear.class };
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
