//
// $Id$

package com.threerings.expr;

import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * The superclass of expressions that evaluate to objects.
 */
public abstract class ObjectExpression<T> extends DeepObject
    implements Exportable
{
    /**
     * Performs the actual evaluation of the expression.
     */
    public static abstract class Evaluator<T>
    {
        /**
         * Evaluates and returns the current value of the expression.
         */
        public abstract T evaluate ();
    }

    /**
     * Creates an expression evaluator for the supplied context.
     */
    public abstract Evaluator<T> createEvaluator (Scope scope);
}
