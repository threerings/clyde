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

package com.threerings.tudey.server.logic;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ExpressionConfig;
import com.threerings.tudey.server.TudeySceneManager;

/**
 * Handles the server-side expression evaluation.
 */
public abstract class ExpressionLogic extends Logic
{
    /**
     * Evaluates a constant expression.
     */
    public static class Constant extends ExpressionLogic
    {
        @Override // documentation inherited
        public Object evaluate (Logic activator, Object previous)
        {
            return ((ExpressionConfig.Constant)_config).value;
        }
    }

    /**
     * Evaluates a reference expression.
     */
    public static class Reference extends ExpressionLogic
    {
        @Override // documentation inherited
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

        @Override // documentation inherited
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
     * Evaluates a constant expression.
     */
    public static class Previous extends ExpressionLogic
    {
        @Override // documentation inherited
        public Object evaluate (Logic activator, Object previous)
        {
            return previous;
        }
    }

    /**
     * Evaluates an increment expression.
     */
    public static class Increment extends ExpressionLogic
    {
        @Override // documentation inherited
        public Object evaluate (Logic activator, Object previous)
        {
            return coerceToDouble(_operand.evaluate(activator, previous)) + 1.0;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _operand = createExpression(((ExpressionConfig.Increment)_config).operand, _source);
        }

        /** The operand logic. */
        protected ExpressionLogic _operand;
    }

    /**
     * Base class for the binary operations.
     */
    public static abstract class BinaryOperation extends ExpressionLogic
    {
        @Override // documentation inherited
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
     * Evaluates a greater than or equal to expression.
     */
    public static class GreaterEquals extends BinaryOperation
    {
        @Override // documentation inherited
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

    @Override // documentation inherited
    public boolean isActive ()
    {
        return _source.isActive();
    }

    @Override // documentation inherited
    public Vector2f getTranslation ()
    {
        return _source.getTranslation();
    }

    @Override // documentation inherited
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

    /** The expression configuration. */
    protected ExpressionConfig _config;

    /** The expression source. */
    protected Logic _source;
}
