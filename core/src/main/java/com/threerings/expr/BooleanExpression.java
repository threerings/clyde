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

package com.threerings.expr;

import java.io.StringReader;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.expr.util.ScopeUtil;

/**
 * A boolean-valued expression.
 */
@EditorTypes({
    BooleanExpression.Parsed.class, BooleanExpression.Constant.class,
    BooleanExpression.Reference.class, BooleanExpression.Not.class,
    BooleanExpression.And.class, BooleanExpression.Or.class,
    BooleanExpression.Xor.class, BooleanExpression.BooleanEquals.class,
    BooleanExpression.FloatLess.class, BooleanExpression.FloatGreater.class,
    BooleanExpression.FloatEquals.class, BooleanExpression.FloatLessEquals.class,
    BooleanExpression.FloatGreaterEquals.class, BooleanExpression.StringEquals.class })
public abstract class BooleanExpression extends DeepObject
    implements Exportable
{
    /**
     * An expression entered as a string to be parsed.
     */
    public static class Parsed extends BooleanExpression
    {
        /** The expression to parse. */
        @Editable(width=20)
        public String expression = "false";

        @Override
        public Evaluator createEvaluator (Scope scope)
        {
            if (_expr == null) {
                try {
                    _expr = parseExpression(expression);
                } catch (Exception e) {
                    // don't worry about it; it's probably being entered
                }
                if (_expr == null) {
                    _expr = new Constant(false);
                }
            }
            return _expr.createEvaluator(scope);
        }

        @Override
        public void invalidate ()
        {
            _expr = null;
        }

        /** The cached, parsed expression. */
        @DeepOmit
        protected transient BooleanExpression _expr;
    }

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

        @Override
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

        @Override
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

        @Override
        public Evaluator createEvaluator (Scope scope)
        {
            final Evaluator eval = operand.createEvaluator(scope);
            return new Evaluator() {
                public boolean evaluate () {
                    return !eval.evaluate();
                }
            };
        }

        @Override
        public void invalidate ()
        {
            operand.invalidate();
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

        @Override
        public Evaluator createEvaluator (Scope scope)
        {
            return createEvaluator(
                firstOperand.createEvaluator(scope), secondOperand.createEvaluator(scope));
        }

        @Override
        public void invalidate ()
        {
            firstOperand.invalidate();
            secondOperand.invalidate();
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
        @Override
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
        @Override
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
        @Override
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
     * Checks the equality of its operands.
     */
    public static class BooleanEquals extends BinaryOperation
    {
        @Override
        protected Evaluator createEvaluator (final Evaluator eval1, final Evaluator eval2)
        {
            return new Evaluator() {
                public boolean evaluate () {
                    return eval1.evaluate() == eval2.evaluate();
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

        @Override
        public Evaluator createEvaluator (Scope scope)
        {
            return createEvaluator(
                firstOperand.createEvaluator(scope), secondOperand.createEvaluator(scope));
        }

        @Override
        public void invalidate ()
        {
            firstOperand.invalidate();
            secondOperand.invalidate();
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
        @Override
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
        @Override
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
        @Override
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
     * Determines whether the first float is greater than or equal to the second.
     */
    public static class FloatLessEquals extends FloatBinaryOperation
    {
        @Override
        protected Evaluator createEvaluator (
            final FloatExpression.Evaluator eval1, final FloatExpression.Evaluator eval2)
        {
            return new Evaluator() {
                public boolean evaluate () {
                    return eval1.evaluate() <= eval2.evaluate();
                }
            };
        }
    }

    /**
     * Determines whether the first float is greater than or equal to the second.
     */
    public static class FloatGreaterEquals extends FloatBinaryOperation
    {
        @Override
        protected Evaluator createEvaluator (
            final FloatExpression.Evaluator eval1, final FloatExpression.Evaluator eval2)
        {
            return new Evaluator() {
                public boolean evaluate () {
                    return eval1.evaluate() >= eval2.evaluate();
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

        @Override
        public Evaluator createEvaluator (Scope scope)
        {
            final ObjectExpression.Evaluator<String> eval1 = firstOperand.createEvaluator(scope);
            final ObjectExpression.Evaluator<String> eval2 = secondOperand.createEvaluator(scope);
            return new Evaluator() {
                public boolean evaluate () {
                    return eval1.evaluate().equals(eval2.evaluate());
                }
            };
        }

        @Override
        public void invalidate ()
        {
            firstOperand.invalidate();
            secondOperand.invalidate();
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

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }

    /**
     * Parses the supplied string expression.
     */
    protected static BooleanExpression parseExpression (String expression)
        throws Exception
    {
        return (BooleanExpression)new ExpressionParser<Object>(new StringReader(expression)) {
            @Override protected Object handleNumber (double value) {
                return new FloatExpression.Constant((float)value);
            }
            @Override protected Object handleString (String value) {
                return new StringExpression.Constant(value);
            }
            @Override protected Object handleOperator (String operator, int arity)
                    throws Exception {
                if (arity == 1) {
                    if (operator.equals("!")) {
                        Not not = new Not();
                        not.operand = (BooleanExpression)_output.pop();
                        return not;

                    } else {
                        return super.handleOperator(operator, arity);
                    }
                } else { // arity == 2
                    BinaryOperation result;
                    if (operator.equals("&") || operator.equals("&&")) {
                        result = new And();
                    } else if (operator.equals("|") || operator.equals("||")) {
                        result = new Or();
                    } else if (operator.equals("^")) {
                        result = new Xor();
                    } else if (operator.equals("<")) {
                        return handleFloatBinaryOperation(new FloatLess());
                    } else if (operator.equals(">")) {
                        return handleFloatBinaryOperation(new FloatGreater());
                    } else if (operator.equals("=") || operator.equals("==")) {
                        return handleEquals();
                    } else if (operator.equals("!=")) {
                        Not not = new Not();
                        not.operand = handleEquals();
                        return not;
                    } else if (operator.equals("<=")) {
                        return handleFloatBinaryOperation(new FloatLessEquals());
                    } else if (operator.equals(">=")) {
                        return handleFloatBinaryOperation(new FloatGreaterEquals());
                    } else {
                        return super.handleOperator(operator, arity);
                    }
                    result.secondOperand = (BooleanExpression)_output.pop();
                    result.firstOperand = (BooleanExpression)_output.pop();
                    return result;
                }
            }
            @Override protected Object handleFunctionCall (String function, int arity)
                    throws Exception {
                if (function.equals("float")) {
                    StringExpression.Constant expr = (StringExpression.Constant)_output.pop();
                    return FloatExpression.parseExpression(expr.value);

                } else if (function.equals("string")) {
                    StringExpression.Constant expr = (StringExpression.Constant)_output.pop();
                    return StringExpression.parseExpression(expr.value);

                } else {
                    return super.handleFunctionCall(function, arity);
                }
            }
            @Override protected Object handleIdentifier (String name) {
                if (name.equalsIgnoreCase("true")) {
                    return new Constant(true);
                } else if (name.equalsIgnoreCase("false")) {
                    return new Constant(false);
                }
                Reference ref = new Reference();
                ref.name = name;
                return ref;
            }
            protected Object handleFloatBinaryOperation (FloatBinaryOperation op)
                    throws Exception {
                op.secondOperand = coerceToFloatExpression(_output.pop());
                op.firstOperand = coerceToFloatExpression(_output.pop());
                return op;
            }
            protected BooleanExpression handleEquals () throws Exception {
                Object secondOperand = _output.pop();
                Object firstOperand = _output.pop();
                if (firstOperand instanceof FloatExpression ||
                        secondOperand instanceof FloatExpression) {
                    FloatEquals result = new FloatEquals();
                    result.firstOperand = coerceToFloatExpression(firstOperand);
                    result.secondOperand = coerceToFloatExpression(secondOperand);
                    return result;

                } else if (firstOperand instanceof StringExpression ||
                        secondOperand instanceof StringExpression) {
                    StringEquals result = new StringEquals();
                    result.firstOperand = coerceToStringExpression(firstOperand);
                    result.secondOperand = coerceToStringExpression(secondOperand);
                    return result;

                } else {
                    BooleanEquals result = new BooleanEquals();
                    result.firstOperand = coerceToBooleanExpression(firstOperand);
                    result.secondOperand = coerceToBooleanExpression(secondOperand);
                    return result;
                }
            }
            protected BooleanExpression coerceToBooleanExpression (Object object)
                    throws Exception {
                if (object instanceof BooleanExpression) {
                    return (BooleanExpression)object;
                } else {
                    throw new Exception("Cannot coerce to boolean expression " + object);
                }
            }
            protected FloatExpression coerceToFloatExpression (Object object)
                    throws Exception {
                if (object instanceof FloatExpression) {
                    return (FloatExpression)object;
                } else if (object instanceof Reference) {
                    // convert to a float reference
                    Reference ref = (Reference)object;
                    FloatExpression.Reference nref = new FloatExpression.Reference();
                    nref.name = ref.name;
                    return nref;
                } else {
                    throw new Exception("Cannot coerce to float expression " + object);
                }
            }
            protected StringExpression coerceToStringExpression (Object object)
                throws Exception {
                if (object instanceof StringExpression) {
                    return (StringExpression)object;
                } else if (object instanceof Reference) {
                    // convert to a string reference
                    Reference ref = (Reference)object;
                    StringExpression.Reference nref = new StringExpression.Reference();
                    nref.name = ref.name;
                    return nref;
                } else {
                    throw new Exception("Cannot coerce to string expression " + object);
                }
            }
        }.parse();
    }
}
