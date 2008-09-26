//
// $Id$

package com.threerings.expr;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;

import com.threerings.expr.util.ScopeUtil;

/**
 * A string-valued expression.
 */
@EditorTypes({
    StringExpression.Constant.class,
    StringExpression.Reference.class })
public abstract class StringExpression extends ObjectExpression<String>
{
    /**
     * A constant expression.
     */
    public static class Constant extends StringExpression
    {
        /** The value of the constant. */
        @Editable
        public String value = "";

        @Override // documentation inherited
        public Evaluator<String> createEvaluator (Scope scope)
        {
            return new Evaluator<String>() {
                public String evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * A reference expression.
     */
    public static class Reference extends StringExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The default value of the variable. */
        @Editable
        public String defvalue = "";

        @Override // documentation inherited
        public Evaluator<String> createEvaluator (Scope scope)
        {
            final Variable variable = ScopeUtil.resolve(
                scope, name, Variable.newInstance(defvalue));
            return new Evaluator<String>() {
                public String evaluate () {
                    return (String)variable.get();
                }
            };
        }
    }
}
