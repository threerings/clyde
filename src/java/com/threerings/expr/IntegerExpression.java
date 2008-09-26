//
// $Id$

package com.threerings.expr;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.expr.util.ScopeUtil;

/**
 * An integer-valued expression.
 */
@EditorTypes({
    IntegerExpression.Constant.class, IntegerExpression.Reference.class })
public abstract class IntegerExpression extends DeepObject
    implements Exportable
{
    /**
     * A constant expression.
     */
    public static class Constant extends IntegerExpression
    {
        /** The value of the constant. */
        @Editable
        public int value;

        /**
         * Creates a new constant expression with the specified value.
         */
        public Constant (int value)
        {
            this.value = value;
        }

        /**
         * Creates a new constant expression with a value of zero.
         */
        public Constant ()
        {
        }

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            return new Evaluator() {
                public int evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * A reference expression.
     */
    public static class Reference extends IntegerExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The default value of the variable. */
        @Editable
        public int defvalue;

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            // first look for a mutable reference, then for a variable
            final MutableInteger reference = ScopeUtil.resolve(
                scope, name, (MutableInteger)null);
            if (reference != null) {
                return new Evaluator() {
                    public int evaluate () {
                        return reference.value;
                    }
                };
            }
            final Variable variable = ScopeUtil.resolve(
                scope, name, Variable.newInstance(defvalue));
            return new Evaluator() {
                public int evaluate () {
                    return variable.getInt();
                }
            };
        }
    }

    /**
     * Performs the actual evaluation of the expression.
     */
    public static abstract class Evaluator
    {
        /**
         * Evaluates and returns the current value of the expression.
         */
        public abstract int evaluate ();
    }

    /**
     * Creates an expression evaluator for the supplied context.
     */
    public abstract Evaluator createEvaluator (Scope scope);
}
