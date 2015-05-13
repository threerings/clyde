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

package com.threerings.tudey.server.logic;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ExpressionConfig;
import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.server.TudeySceneManager;

import static com.threerings.tudey.Log.log;

/**
 * Handles the server-side expression evaluation.
 */
public abstract class ExpressionLogic extends Logic
{
    /**
     * Evaluates a parsed expression.
     */
    public static class Parsed extends ExpressionLogic
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return _expr.evaluate(activator, previous);
        }

        @Override
        protected void didInit ()
        {
            _expr = createExpression(((ExpressionConfig.Parsed)_config).getExpression(), _source);
        }

        /** The parsed value. */
        protected ExpressionLogic _expr;
    }

    /**
     * Evaluates a constant expression.
     */
    public static class Constant extends ExpressionLogic
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return _value;
        }

        @Override
        protected void didInit ()
        {
            _value = parseValue(((ExpressionConfig.Constant)_config).value);
        }

        /** The parsed value. */
        protected Object _value;
    }

    /**
     * Evaluates a reference expression.
     */
    public static class Reference extends ExpressionLogic
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            String name = ((ExpressionConfig.Reference)_config).name;
            _target.resolve(activator, _targets);
            for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                Object value = _targets.get(ii).getVariable(name);
                if (value != null) {
                    _targets.clear();
                    return value;
                }
            }
            _targets.clear();
            return null;
        }

        @Override
        protected void didInit ()
        {
            _target = createTarget(((ExpressionConfig.Reference)_config).target, _source);
        }

        /** The target logic. */
        protected TargetLogic _target;

        /** A container for the resolved targets. */
        protected List<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates a num targets expression.
     */
    public static class NumTargets extends ExpressionLogic
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            _target.resolve(activator, _targets);
            Integer numTargets = new Integer(_targets.size());
            _targets.clear();
            return numTargets;
        }

        @Override
        protected void didInit ()
        {
            _target = createTarget(((ExpressionConfig.NumTargets)_config).target, _source);
        }

        /** The target logic. */
        protected TargetLogic _target;

        /** A container for the resolved targets. */
        protected List<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates a variable expression.
     */
    public static class Variable extends ExpressionLogic
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return ((ExpressionConfig.Variable)_config).variable.getValue();
        }
    }

    /**
     * Evaluates a constant expression.
     */
    public static class Previous extends ExpressionLogic
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return previous;
        }
    }

    /**
     * Base class for unary operations.
     */
    public static abstract class UnaryOperation extends ExpressionLogic
    {
        @Override
        protected void didInit ()
        {
            _operand = createExpression(
                ((ExpressionConfig.UnaryOperation)_config).operand, _source);
        }

        /** The operand logic. */
        protected ExpressionLogic _operand;
    }

    /**
     * Evaluates a round expression.
     */
    public static class Round extends UnaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return Math.round(coerceToDouble(_operand.evaluate(activator, previous)));
        }
    }

    /**
     * Evaluates a ciel expression.
     */
    public static class Ceil extends UnaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return Math.ceil(coerceToDouble(_operand.evaluate(activator, previous)));
        }
    }

    /**
     * Evaluates a floor expression.
     */
    public static class Floor extends UnaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return Math.floor(coerceToDouble(_operand.evaluate(activator, previous)));
        }
    }

    /**
     * Evaluates an increment expression.
     */
    public static class Increment extends UnaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_operand.evaluate(activator, previous)) + 1.0;
        }
    }

    /**
     * Evaluates a decrement expression.
     */
    public static class Decrement extends UnaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_operand.evaluate(activator, previous)) - 1.0;
        }
    }

    /**
     * Evaluates a negate expression.
     */
    public static class Negate extends UnaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return -coerceToDouble(_operand.evaluate(activator, previous));
        }
    }

    /**
     * Base class for the binary operations.
     */
    public static abstract class BinaryOperation extends ExpressionLogic
    {
        @Override
        protected void didInit ()
        {
            ExpressionConfig.BinaryOperation config = (ExpressionConfig.BinaryOperation)_config;
            _firstOperand = createExpression(config.firstOperand, _source);
            _secondOperand = createExpression(config.secondOperand, _source);
        }

        /** The operand logics. */
        protected ExpressionLogic _firstOperand, _secondOperand;
    }

    /**
     * Evaluates a power expression.
     */
    public static class Power extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return Math.pow(coerceToDouble(_firstOperand.evaluate(activator, previous)),
                coerceToDouble(_secondOperand.evaluate(activator, previous)));
        }
    }

    /**
     * Evaluates an add expression.
     */
    public static class Add extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_firstOperand.evaluate(activator, previous)) +
                coerceToDouble(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates an add expression.
     */
    public static class Subtract extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_firstOperand.evaluate(activator, previous)) -
                coerceToDouble(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates an add expression.
     */
    public static class Multiply extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_firstOperand.evaluate(activator, previous)) *
                coerceToDouble(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates an divide expression.
     */
    public static class Divide extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_firstOperand.evaluate(activator, previous)) /
                coerceToDouble(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates an add expression.
     */
    public static class Remainder extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_firstOperand.evaluate(activator, previous)) %
                coerceToDouble(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates a logical NOT expression.
     */
    public static class Not extends UnaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return !coerceToBoolean(_operand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates a logical AND expression.
     */
    public static class And extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToBoolean(_firstOperand.evaluate(activator, previous)) &&
                coerceToBoolean(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates a logical OR expression.
     */
    public static class Or extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToBoolean(_firstOperand.evaluate(activator, previous)) ||
                coerceToBoolean(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates a logical XOR expression.
     */
    public static class Xor extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToBoolean(_firstOperand.evaluate(activator, previous)) ^
                coerceToBoolean(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates a less than expression.
     */
    public static class Less extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_firstOperand.evaluate(activator, previous)) <
                coerceToDouble(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates a greater than expression.
     */
    public static class Greater extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_firstOperand.evaluate(activator, previous)) >
                coerceToDouble(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates an equal to expression.
     */
    public static class Equals extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return Objects.equal(_firstOperand.evaluate(activator, previous),
                _secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates a less than or equal to expression.
     */
    public static class LessEquals extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_firstOperand.evaluate(activator, previous)) <=
                coerceToDouble(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Evaluates a greater than or equal to expression.
     */
    public static class GreaterEquals extends BinaryOperation
    {
        @Override
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_firstOperand.evaluate(activator, previous)) >=
                coerceToDouble(_secondOperand.evaluate(activator, previous));
        }
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, ExpressionConfig config, Logic source)
    {
        super.init(scenemgr);
        _config = config;
        _source = source;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Evaluates the expression.
     */
    public abstract Object evaluate (Logic activator, Object previous);

    @Override
    public boolean isActive ()
    {
        return _source.isActive();
    }

    @Override
    public EntityKey getEntityKey ()
    {
        return _source.getEntityKey();
    }

    @Override
    public Vector2f getTranslation ()
    {
        return _source.getTranslation();
    }

    @Override
    public float getRotation ()
    {
        return _source.getRotation();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /**
     * Attempts to parse the supplied string into one of our known types.
     */
    protected static Object parseValue (String str)
    {
        if (str.equalsIgnoreCase("null")) {
            return null;
        } else if (str.equalsIgnoreCase("true")) {
            return true;
        } else if (str.equalsIgnoreCase("false")) {
            return false;
        }
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            // fall through
        }
        return str;
    }

    /**
     * Coerces the specified weakly typed value to a boolean.
     */
    protected static boolean coerceToBoolean (Object value)
    {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return ((Boolean)value).booleanValue();
        }
        if (value instanceof Number) {
            return ((Number)value).doubleValue() != 0.0;
        }
        if (value instanceof String) {
            String str = (String)value;
            try {
                return Double.parseDouble(str) != 0.0;
            } catch (NumberFormatException e) {
                return Boolean.parseBoolean(str);
            }
        }
        log.warning("Cannot coerce value to boolean.", "value", value);
        return false;
    }

    /**
     * Coerces the specified weakly typed value to a double.
     */
    protected static double coerceToDouble (Object value)
    {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Boolean) {
            return ((Boolean)value).booleanValue() ? 1.0 : 0.0;
        }
        if (value instanceof Number) {
            return ((Number)value).doubleValue();
        }
        if (value instanceof String) {
            String str = (String)value;
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                return Boolean.parseBoolean(str) ? 1.0 : 0.0;
            }
        }
        log.warning("Cannot coerce value to double.", "value", value);
        return 0.0;
    }

    /** The expression configuration. */
    protected ExpressionConfig _config;

    /** The expression source. */
    protected Logic _source;
}
