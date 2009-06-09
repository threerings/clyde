//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.expr;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.expr.util.ScopeUtil;

/**
 * A boolean-valued expression.
 */
@EditorTypes({
    BooleanExpression.Constant.class, BooleanExpression.Reference.class,
    BooleanExpression.Not.class, BooleanExpression.And.class,
    BooleanExpression.Or.class, BooleanExpression.Xor.class,
    BooleanExpression.FloatLess.class, BooleanExpression.FloatGreater.class,
    BooleanExpression.FloatEquals.class, BooleanExpression.StringEquals.class })
public abstract class BooleanExpression extends DeepObject
    implements Exportable
{
    /**
     * A constant expression.
     */
    public static class Constant extends BooleanExpression
    {
        /** The value of the constant. */
        @Editable
        public boolean value;

        /**
         * Creates a new constant expression with the specified value.
         */
        public Constant (boolean value)
        {
            this.value = value;
        }

        /**
         * Creates a new constant expression with a value of false.
         */
        public Constant ()
        {
        }

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            return new Evaluator() {
                public boolean evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * A reference expression.
     */
    public static class Reference extends BooleanExpression
    {
        /** The name of the variable. */
        @Editable(hgroup="n")
        public String name = "";

        /** The default value of the variable. */
        @Editable(hgroup="n")
        public boolean defvalue;

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            // first look for a mutable reference, then for a variable
            final MutableBoolean reference = ScopeUtil.resolve(
                scope, name, (MutableBoolean)null);
            if (reference != null) {
                return new Evaluator() {
                    public boolean evaluate () {
                        return reference.value;
                    }
                };
            }
            final Variable variable = ScopeUtil.resolve(
                scope, name, Variable.newInstance(defvalue));
            return new Evaluator() {
                public boolean evaluate () {
                    return variable.getBoolean();
                }
            };
        }
    }

    /**
     * Returns the logical NOT of the sub-expression.
     */
    public static class Not extends BooleanExpression
    {
        /** The operand expression. */
        @Editable
        public BooleanExpression operand = new Constant();

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            final Evaluator eval = operand.createEvaluator(scope);
            return new Evaluator() {
                public boolean evaluate () {
                    return !eval.evaluate();
                }
            };
        }
    }

    /**
     * The superclass of the binary operations.
     */
    public static abstract class BinaryOperation extends BooleanExpression
    {
        /** The first operand expression. */
        @Editable
        public BooleanExpression firstOperand = new Constant();

        /** The second operand expression. */
        @Editable
        public BooleanExpression secondOperand = new Constant();

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            return createEvaluator(
                firstOperand.createEvaluator(scope), secondOperand.createEvaluator(scope));
        }

        /**
         * Creates the evaluator for this expression, given the evaluators for its operands.
         */
        protected abstract Evaluator createEvaluator (Evaluator eval1, Evaluator eval2);
    }

    /**
     * Computes the logical AND of its operands.
     */
    public static class And extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval1, final Evaluator eval2)
        {
            return new Evaluator() {
                public boolean evaluate () {
                    return eval1.evaluate() && eval2.evaluate();
                }
            };
        }
    }

    /**
     * Computes the logical OR of its operands.
     */
    public static class Or extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval1, final Evaluator eval2)
        {
            return new Evaluator() {
                public boolean evaluate () {
                    return eval1.evaluate() || eval2.evaluate();
                }
            };
        }
    }

    /**
     * Computes the logical XOR of its operands.
     */
    public static class Xor extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval1, final Evaluator eval2)
        {
            return new Evaluator() {
                public boolean evaluate () {
                    return eval1.evaluate() ^ eval2.evaluate();
                }
            };
        }
    }

    /**
     * The superclass of the operations involving two float expressions.
     */
    public static abstract class FloatBinaryOperation extends BooleanExpression
    {
        /** The first operand expression. */
        @Editable
        public FloatExpression firstOperand = new FloatExpression.Constant();

        /** The second operand expression. */
        @Editable
        public FloatExpression secondOperand = new FloatExpression.Constant();

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            return createEvaluator(
                firstOperand.createEvaluator(scope), secondOperand.createEvaluator(scope));
        }

        /**
         * Creates the evaluator for this expression, given the evaluators for its operands.
         */
        protected abstract Evaluator createEvaluator (
            FloatExpression.Evaluator eval1, FloatExpression.Evaluator eval2);
    }

    /**
     * Determines whether the first float is less than the second.
     */
    public static class FloatLess extends FloatBinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (
            final FloatExpression.Evaluator eval1, final FloatExpression.Evaluator eval2)
        {
            return new Evaluator() {
                public boolean evaluate () {
                    return eval1.evaluate() < eval2.evaluate();
                }
            };
        }
    }

    /**
     * Determines whether the first float is greater than the second.
     */
    public static class FloatGreater extends FloatBinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (
            final FloatExpression.Evaluator eval1, final FloatExpression.Evaluator eval2)
        {
            return new Evaluator() {
                public boolean evaluate () {
                    return eval1.evaluate() > eval2.evaluate();
                }
            };
        }
    }

    /**
     * Determines whether the first float is equal to the second.
     */
    public static class FloatEquals extends FloatBinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (
            final FloatExpression.Evaluator eval1, final FloatExpression.Evaluator eval2)
        {
            return new Evaluator() {
                public boolean evaluate () {
                    return eval1.evaluate() == eval2.evaluate();
                }
            };
        }
    }

    /**
     * Compares two strings for equality.
     */
    public static class StringEquals extends BooleanExpression
    {
        /** The first operand expression. */
        @Editable
        public StringExpression firstOperand = new StringExpression.Constant();

        /** The second operand expression. */
        @Editable
        public StringExpression secondOperand = new StringExpression.Constant();

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            final StringExpression.Evaluator eval1 = firstOperand.createEvaluator(scope);
            final StringExpression.Evaluator eval2 = secondOperand.createEvaluator(scope);
            return new Evaluator() {
                public boolean evaluate () {
                    return eval1.evaluate().equals(eval2.evaluate());
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
        public abstract boolean evaluate ();
    }

    /**
     * Creates an expression evaluator for the supplied context.
     */
    public abstract Evaluator createEvaluator (Scope scope);
}
