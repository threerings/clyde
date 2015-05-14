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

package com.threerings.tudey.config;

import java.io.StringReader;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.Strippable;
import com.threerings.export.Exportable;
import com.threerings.expr.ExpressionParser;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.probs.FloatVariable;

/**
 * Configurations for (weakly typed) server-side expressions.
 */
@EditorTypes({
    ExpressionConfig.Parsed.class, ExpressionConfig.Constant.class,
    ExpressionConfig.Reference.class, ExpressionConfig.Previous.class,
    ExpressionConfig.Increment.class, ExpressionConfig.Decrement.class,
    ExpressionConfig.Round.class, ExpressionConfig.Ceil.class,
    ExpressionConfig.Floor.class, ExpressionConfig.Power.class,
    ExpressionConfig.Negate.class, ExpressionConfig.Add.class,
    ExpressionConfig.Subtract.class, ExpressionConfig.Multiply.class,
    ExpressionConfig.Divide.class, ExpressionConfig.Remainder.class,
    ExpressionConfig.Not.class, ExpressionConfig.And.class,
    ExpressionConfig.Or.class, ExpressionConfig.Xor.class,
    ExpressionConfig.Less.class, ExpressionConfig.Greater.class,
    ExpressionConfig.Equals.class, ExpressionConfig.LessEquals.class,
    ExpressionConfig.GreaterEquals.class, ExpressionConfig.Variable.class,
    ExpressionConfig.NumTargets.class })
@Strippable
public abstract class ExpressionConfig extends DeepObject
    implements Exportable, Streamable
{
    /**
     * An expression entered as a string to be parsed.
     */
    public static class Parsed extends ExpressionConfig
    {
        /** The expression to parse. */
        @Editable(width=20)
        public String expression = "";

        /**
         * Returns the cached, parsed expression.
         */
        public ExpressionConfig getExpression ()
        {
            if (_expr == null) {
                try {
                    _expr = parseExpression(expression);
                } catch (Exception e) {
                    // don't worry about it; it's probably being entered
                }
                if (_expr == null) {
                    _expr = new Constant();
                }
            }
            return _expr;
        }

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Parsed";
        }

        @Override
        public void invalidate ()
        {
            _expr = null;
        }

        /** The cached, parsed expression. */
        @DeepOmit
        protected transient ExpressionConfig _expr;
    }

    /**
     * A constant expression.
     */
    public static class Constant extends ExpressionConfig
    {
        /** The value of the constant. */
        @Editable
        public String value = "";

        /**
         * Creates a constant expression with the supplied value.
         */
        public Constant (String value)
        {
            this.value = value;
        }

        /**
         * Default constructor.
         */
        public Constant ()
        {
        }

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Constant";
        }
    }

    /**
     * A reference to another variable.
     */
    public static class Reference extends ExpressionConfig
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The target containing the variable. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Reference";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * The previous value.
     */
    public static class Previous extends ExpressionConfig
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Previous";
        }
    }

    /**
     * The side of the target results.
     */
    public static class NumTargets extends ExpressionConfig
    {
        /** The target. */
        @Editable
        public TargetConfig target = new TargetConfig.Tagged();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$NumTargets";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * A float variable.
     */
    public static class Variable extends ExpressionConfig
    {
        /** The variable. */
        @Editable
        public FloatVariable variable = new FloatVariable.Constant(0f);

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Variable";
        }
    }

    /**
     * Base class for unary operations.
     */
    public static abstract class UnaryOperation extends ExpressionConfig
    {
        /** The operand of the expression. */
        @Editable
        public ExpressionConfig operand = new ExpressionConfig.Constant();

        @Override
        public void invalidate ()
        {
            operand.invalidate();
        }
    }

    /**
     * Rounds the value per standard rounding rules
     */
    public static class Round extends UnaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Round";
        }
    }

    /**
     * Rounds the value down
     */
    public static class Floor extends UnaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Floor";
        }
    }

    /**
     * Rounds the value up
     */
    public static class Ceil extends UnaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Ceil";
        }
    }

    /**
     * Adds one to the operand.
     */
    public static class Increment extends UnaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Increment";
        }
    }

    /**
     * Subtracts one from the operand.
     */
    public static class Decrement extends UnaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Decrement";
        }
    }

    /**
     * Negates the operand.
     */
    public static class Negate extends UnaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Negate";
        }
    }

    /**
     * Base class for binary operations.
     */
    public static abstract class BinaryOperation extends ExpressionConfig
    {
        /** The first operand of the expression. */
        @Editable
        public ExpressionConfig firstOperand = new ExpressionConfig.Constant();

        /** The second operand of the expression. */
        @Editable
        public ExpressionConfig secondOperand = new ExpressionConfig.Constant();

        @Override
        public void invalidate ()
        {
            firstOperand.invalidate();
            secondOperand.invalidate();
        }
    }

    /**
     * Takes the first operand to the power of the second operand.
     */
    public static class Power extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Power";
        }
    }

    /**
     * Computes the sum of the operands.
     */
    public static class Add extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Add";
        }
    }

    /**
     * Computes the difference of the operands.
     */
    public static class Subtract extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Subtract";
        }
    }

    /**
     * Computes the product of the operands.
     */
    public static class Multiply extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Multiply";
        }
    }

    /**
     * Computes the quotient of the operands.
     */
    public static class Divide extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Divide";
        }
    }

    /**
     * Computes the remainder of division of the operands.
     */
    public static class Remainder extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Remainder";
        }
    }

    /**
     * Computes the logical NOT of the operand.
     */
    public static class Not extends UnaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Not";
        }
    }

    /**
     * Computes the logical AND of the first and second operands.
     */
    public static class And extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$And";
        }
    }

    /**
     * Computes the logical OR of the first and second operands.
     */
    public static class Or extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Or";
        }
    }

    /**
     * Computes the logical XOR of the first and second operands.
     */
    public static class Xor extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Xor";
        }
    }

    /**
     * Finds out whether the first operand is less then the second.
     */
    public static class Less extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Less";
        }
    }

    /**
     * Finds out whether the first operand is greater than the second.
     */
    public static class Greater extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Greater";
        }
    }

    /**
     * Finds out whether the first operand is equal to the second.
     */
    public static class Equals extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Equals";
        }
    }

    /**
     * Finds out whether the first operand is less than or equal to the second.
     */
    public static class LessEquals extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$LessEquals";
        }
    }

    /**
     * Finds out whether the first operand is greater than or equal to the second.
     */
    public static class GreaterEquals extends BinaryOperation
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$GreaterEquals";
        }
    }

    /**
     * Returns the name of the server-side logic class for this expression.
     */
    public abstract String getLogicClassName ();

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
    protected static ExpressionConfig parseExpression (String expression)
        throws Exception
    {
        return new ExpressionParser<ExpressionConfig>(new StringReader(expression)) {
            @Override protected ExpressionConfig handleNumber (double value) {
                return new Constant(String.valueOf(value));
            }
            @Override protected ExpressionConfig handleString (String value) {
                return new Constant(value);
            }
            @Override protected ExpressionConfig handleOperator (String operator, int arity)
                    throws Exception {
                if (arity == 1) {
                    UnaryOperation result;
                    if (operator.equals("++")) {
                        result = new Increment();
                    } else if (operator.equals("--")) {
                        result = new Decrement();
                    } else if (operator.equals("+")) {
                        return _output.pop();
                    } else if (operator.equals("-")) {
                        result = new Negate();
                    } else if (operator.equals("!")) {
                        result = new Not();
                    } else {
                        return super.handleOperator(operator, arity);
                    }
                    result.operand = _output.pop();
                    return result;

                } else { // arity == 2
                    BinaryOperation result;
                    if (operator.equals("+")) {
                        result = new Add();
                    } else if (operator.equals("-")) {
                        result = new Subtract();
                    } else if (operator.equals("*")) {
                        result = new Multiply();
                    } else if (operator.equals("/")) {
                        result = new Divide();
                    } else if (operator.equals("%")) {
                        result = new Remainder();
                    } else if (operator.equals("&") || operator.equals("&&")) {
                        result = new And();
                    } else if (operator.equals("|") || operator.equals("||")) {
                        result = new Or();
                    } else if (operator.equals("^")) {
                        result = new Xor();
                    } else if (operator.equals("<")) {
                        result = new Less();
                    } else if (operator.equals(">")) {
                        result = new Greater();
                    } else if (operator.equals("=") || operator.equals("==")) {
                        result = new Equals();
                    } else if (operator.equals("<=")) {
                        result = new LessEquals();
                    } else if (operator.equals(">=")) {
                        result = new GreaterEquals();
                    } else {
                        return super.handleOperator(operator, arity);
                    }
                    result.secondOperand = _output.pop();
                    result.firstOperand = _output.pop();
                    return result;
                }
            }
            @Override protected ExpressionConfig handleIdentifier (String name) {
                if (name.equalsIgnoreCase("null") || name.equalsIgnoreCase("true") ||
                        name.equalsIgnoreCase("false")) {
                    return new Constant(name);

                } else if (name.equalsIgnoreCase("previous")) {
                    return new Previous();
                }
                Reference ref = new Reference();
                ref.name = name;
                return ref;
            }
        }.parse();
    }
}
