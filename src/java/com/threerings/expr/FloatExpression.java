//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

import java.io.StringReader;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.util.DeepObject;
import com.threerings.util.ExpressionParser;

import com.threerings.expr.util.ScopeUtil;

/**
 * A float-valued expression.
 */
@EditorTypes({
    FloatExpression.Parsed.class, FloatExpression.Constant.class,
    FloatExpression.Reference.class, FloatExpression.Clock.class,
    FloatExpression.Negate.class, FloatExpression.Add.class,
    FloatExpression.Subtract.class, FloatExpression.Multiply.class,
    FloatExpression.Divide.class, FloatExpression.Remainder.class,
    FloatExpression.Pow.class, FloatExpression.Sin.class,
    FloatExpression.Cos.class, FloatExpression.Tan.class })
public abstract class FloatExpression extends DeepObject
    implements Exportable
{
    /**
     * An expression entered as a string to be parsed.
     */
    public static class Parsed extends FloatExpression
    {
        /** The expression to parse. */
        @Editable
        public String expression = "0.0";

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            Evaluator eval = null;
            try {
                eval = createParsedEvaluator(expression, scope);
            } catch (Exception e) {
                // don't worry about it; it's probably being entered
            }
            return (eval == null) ? createConstantEvaluator(0f) : eval;
        }
    }

    /**
     * A constant expression.
     */
    public static class Constant extends FloatExpression
    {
        /** The value of the constant. */
        @Editable(step=0.01)
        public float value;

        /**
         * Creates a new constant expression with the specified value.
         */
        public Constant (float value)
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
            return createConstantEvaluator(value);
        }
    }

    /**
     * A reference expression.
     */
    public static class Reference extends FloatExpression
    {
        /** The name of the variable. */
        @Editable(hgroup="n")
        public String name = "";

        /** The default value of the variable. */
        @Editable(step=0.01, hgroup="n")
        public float defvalue;

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            return createReferenceEvaluator(name, defvalue, scope);
        }
    }

    /**
     * A clock-based expression.
     */
    public static class Clock extends FloatExpression
    {
        /** The scope of the epoch reference. */
        @Editable
        public String scope = "";

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            return createClockEvaluator(this.scope.trim(), scope);
        }
    }

    /**
     * The superclass of the unary operations.
     */
    public static abstract class UnaryOperation extends FloatExpression
    {
        /** The operand expression. */
        @Editable
        public FloatExpression operand = new Constant();

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            return createEvaluator(operand.createEvaluator(scope));
        }

        /**
         * Creates the evaluator for this expression, given the evaluator for its operand.
         */
        protected abstract Evaluator createEvaluator (Evaluator eval);
    }

    /**
     * Negates its operand.
     */
    public static class Negate extends UnaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval)
        {
            return new Evaluator() {
                public float evaluate () {
                    return -eval.evaluate();
                }
            };
        }
    }

    /**
     * Computes the sine of its operand.
     */
    public static class Sin extends UnaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (Evaluator eval)
        {
            return createSinEvaluator(eval);
        }
    }

    /**
     * Computes the cosine of its operand.
     */
    public static class Cos extends UnaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (Evaluator eval)
        {
            return createCosEvaluator(eval);
        }
    }

    /**
     * Computes the tangent of its operand.
     */
    public static class Tan extends UnaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (Evaluator eval)
        {
            return createTanEvaluator(eval);
        }
    }

    /**
     * The superclass of the binary operations.
     */
    public static abstract class BinaryOperation extends FloatExpression
    {
        /** The first operand expression. */
        @Editable
        public FloatExpression firstOperand = new Constant();

        /** The second operand expression. */
        @Editable
        public FloatExpression secondOperand = new Constant();

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
     * Adds its operands.
     */
    public static class Add extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (Evaluator eval1, Evaluator eval2)
        {
            return createAddEvaluator(eval1, eval2);
        }
    }

    /**
     * Subtracts the second operand from the first.
     */
    public static class Subtract extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (Evaluator eval1, Evaluator eval2)
        {
            return createSubtractEvaluator(eval1, eval2);
        }
    }

    /**
     * Multiplies its operands.
     */
    public static class Multiply extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (Evaluator eval1, Evaluator eval2)
        {
            return createMultiplyEvaluator(eval1, eval2);
        }
    }

    /**
     * Divides the first operand by the second.
     */
    public static class Divide extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (Evaluator eval1, Evaluator eval2)
        {
            return createDivideEvaluator(eval1, eval2);
        }
    }

    /**
     * Computes the floating point remainder when the first operand is divided by the second.
     */
    public static class Remainder extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (Evaluator eval1, Evaluator eval2)
        {
            return createRemainderEvaluator(eval1, eval2);
        }
    }

    /**
     * Raises the first operand to the power of the second.
     */
    public static class Pow extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (Evaluator eval1, Evaluator eval2)
        {
            return createPowEvaluator(eval1, eval2);
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
        public abstract float evaluate ();
    }

    /**
     * Creates an expression evaluator for the supplied context.
     */
    public abstract Evaluator createEvaluator (Scope scope);

    /**
     * Creates an evaluator for the supplied expression.
     */
    protected static Evaluator createParsedEvaluator (String expression, final Scope scope)
        throws Exception
    {
        return (Evaluator)new ExpressionParser<Object>(new StringReader(expression)) {
            @Override protected Object handleNumber (double value) {
                return createConstantEvaluator((float)value);
            }
            @Override protected Object handleOperator (String operator, int arity)
                    throws Exception {
                if (operator.equals("+")) {
                    Evaluator eval2 = (Evaluator)_output.pop(), eval1 = (Evaluator)_output.pop();
                    return createAddEvaluator(eval1, eval2);

                } else if (operator.equals("-")) {
                    Evaluator eval2 = (Evaluator)_output.pop(), eval1 = (Evaluator)_output.pop();
                    return createSubtractEvaluator(eval1, eval2);

                } else if (operator.equals("*")) {
                    Evaluator eval2 = (Evaluator)_output.pop(), eval1 = (Evaluator)_output.pop();
                    return createMultiplyEvaluator(eval1, eval2);

                } else if (operator.equals("/")) {
                    Evaluator eval2 = (Evaluator)_output.pop(), eval1 = (Evaluator)_output.pop();
                    return createDivideEvaluator(eval1, eval2);

                } else if (operator.equals("%")) {
                    Evaluator eval2 = (Evaluator)_output.pop(), eval1 = (Evaluator)_output.pop();
                    return createRemainderEvaluator(eval1, eval2);

                } else {
                    return super.handleOperator(operator, arity);
                }
            }
            @Override protected Object handleFunctionCall (String function, int arity)
                    throws Exception {
                if (function.equals("clock")) {
                    return createClockEvaluator("", scope);

                } else if (function.equals("pow")) {
                    Evaluator eval2 = (Evaluator)_output.pop(), eval1 = (Evaluator)_output.pop();
                    return createPowEvaluator(eval1, eval2);

                } else if (function.equals("sin")) {
                    return createSinEvaluator((Evaluator)_output.pop());

                } else if (function.equals("cos")) {
                    return createCosEvaluator((Evaluator)_output.pop());

                } else if (function.equals("tan")) {
                    return createTanEvaluator((Evaluator)_output.pop());

                } else {
                    return super.handleFunctionCall(function, arity);
                }
            }
            @Override protected Object handleIdentifier (String name) {
                return createReferenceEvaluator(name, 0f, scope);
            }
        }.parse();
    }

    /**
     * Creates a constant "evaluator."
     */
    protected static Evaluator createConstantEvaluator (final float value)
    {
        return new Evaluator() {
            public float evaluate () {
                return value;
            }
        };
    }

    /**
     * Creates a reference evaluator.
     */
    protected static Evaluator createReferenceEvaluator (String name, float defvalue, Scope scope)
    {
        // first look for a mutable reference, then for a variable
        final MutableFloat reference = ScopeUtil.resolve(
            scope, name, (MutableFloat)null);
        if (reference != null) {
            return new Evaluator() {
                public float evaluate () {
                    return reference.value;
                }
            };
        }
        final Variable variable = ScopeUtil.resolve(
            scope, name, Variable.newInstance(defvalue));
        return new Evaluator() {
            public float evaluate () {
                return variable.getFloat();
            }
        };
    }

    /**
     * Creates a clock evaluator.
     */
    protected static Evaluator createClockEvaluator (String name, Scope scope)
    {
        name = (name.length() > 0) ? (name + ":" + Scope.EPOCH) : Scope.EPOCH;
        MutableLong defvalue = new MutableLong(System.currentTimeMillis());
        final MutableLong epoch = ScopeUtil.resolve(scope, name, defvalue);
        final MutableLong now = ScopeUtil.resolve(scope, Scope.NOW, defvalue);
        return new Evaluator() {
            public float evaluate () {
                return (now.value - epoch.value) / 1000f;
            }
        };
    }

    /**
     * Creates a pow evaluator.
     */
    protected static Evaluator createAddEvaluator (final Evaluator eval1, final Evaluator eval2)
    {
        return new Evaluator() {
            public float evaluate () {
                return eval1.evaluate() + eval2.evaluate();
            }
        };
    }

    /**
     * Creates a pow evaluator.
     */
    protected static Evaluator createSubtractEvaluator (
        final Evaluator eval1, final Evaluator eval2)
    {
        return new Evaluator() {
            public float evaluate () {
                return eval1.evaluate() - eval2.evaluate();
            }
        };
    }

    /**
     * Creates a pow evaluator.
     */
    protected static Evaluator createMultiplyEvaluator (
        final Evaluator eval1, final Evaluator eval2)
    {
        return new Evaluator() {
            public float evaluate () {
                return eval1.evaluate() * eval2.evaluate();
            }
        };
    }

    /**
     * Creates a pow evaluator.
     */
    protected static Evaluator createDivideEvaluator (final Evaluator eval1, final Evaluator eval2)
    {
        return new Evaluator() {
            public float evaluate () {
                return eval1.evaluate() / eval2.evaluate();
            }
        };
    }

    /**
     * Creates a pow evaluator.
     */
    protected static Evaluator createRemainderEvaluator (
        final Evaluator eval1, final Evaluator eval2)
    {
        return new Evaluator() {
            public float evaluate () {
                return eval1.evaluate() % eval2.evaluate();
            }
        };
    }

    /**
     * Creates a pow evaluator.
     */
    protected static Evaluator createPowEvaluator (final Evaluator eval1, final Evaluator eval2)
    {
        return new Evaluator() {
            public float evaluate () {
                return FloatMath.pow(eval1.evaluate(), eval2.evaluate());
            }
        };
    }

    /**
     * Creates a sin evaluator.
     */
    protected static Evaluator createSinEvaluator (final Evaluator eval)
    {
        return new Evaluator() {
            public float evaluate () {
                return FloatMath.sin(eval.evaluate());
            }
        };
    }

    /**
     * Creates a cos evaluator.
     */
    protected static Evaluator createCosEvaluator (final Evaluator eval)
    {
        return new Evaluator() {
            public float evaluate () {
                return FloatMath.cos(eval.evaluate());
            }
        };
    }

    /**
     * Creates a tan evaluator.
     */
    protected static Evaluator createTanEvaluator (final Evaluator eval)
    {
        return new Evaluator() {
            public float evaluate () {
                return FloatMath.tan(eval.evaluate());
            }
        };
    }
}
