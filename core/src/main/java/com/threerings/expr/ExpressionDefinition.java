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

import java.util.ArrayList;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * The superclass of the expression definitions.
 */
@EditorTypes({
    ExpressionDefinition.FloatDefinition.class,
    ExpressionDefinition.IntegerDefinition.class,
    ExpressionDefinition.Color4fDefinition.class,
    ExpressionDefinition.StringDefinition.class,
    ExpressionDefinition.Transform3DDefinition.class })
public abstract class ExpressionDefinition extends DeepObject
    implements Exportable
{
    /**
     * Defines a float variable.
     */
    public static class FloatDefinition extends ExpressionDefinition
    {
        /** The expression that determines the value. */
        @Editable
        public FloatExpression expression = new FloatExpression.Constant();

        @Override
        public Object getValue (Scope scope, ArrayList<Updater> updaters)
        {
            final MutableFloat mutable = new MutableFloat();
            final FloatExpression.Evaluator evaluator = expression.createEvaluator(scope);
            updaters.add(new Updater() {
                public void update () {
                    mutable.value = evaluator.evaluate();
                }
            });
            return mutable;
        }

        @Override
        public void invalidate ()
        {
            expression.invalidate();
        }
    }

    /**
     * Defines an integer variable.
     */
    public static class IntegerDefinition extends ExpressionDefinition
    {
        /** The expression that determines the value. */
        @Editable
        public IntegerExpression expression = new IntegerExpression.Constant();

        @Override
        public Object getValue (Scope scope, ArrayList<Updater> updaters)
        {
            final MutableInteger mutable = new MutableInteger();
            final IntegerExpression.Evaluator evaluator = expression.createEvaluator(scope);
            updaters.add(new Updater() {
                public void update () {
                    mutable.value = evaluator.evaluate();
                }
            });
            return mutable;
        }

        @Override
        public void invalidate ()
        {
            expression.invalidate();
        }
    }

    /**
     * Defines a color variable.
     */
    public static class Color4fDefinition extends ExpressionDefinition
    {
        /** The expression that determines the value. */
        @Editable
        public Color4fExpression expression = new Color4fExpression.Constant();

        @Override
        public Object getValue (Scope scope, ArrayList<Updater> updaters)
        {
            return getValue(scope, updaters, expression);
        }

        @Override
        public void invalidate ()
        {
            expression.invalidate();
        }
    }

    /**
     * Defines a string variable.
     */
    public static class StringDefinition extends ExpressionDefinition
    {
        /** The expression that determines the value. */
        @Editable
        public StringExpression expression = new StringExpression.Constant();

        @Override
        public Object getValue (Scope scope, ArrayList<Updater> updaters)
        {
            final StringBuilder mutable = new StringBuilder();
            final ObjectExpression.Evaluator<String> evaluator = expression.createEvaluator(scope);
            updaters.add(new Updater() {
                public void update () {
                    mutable.replace(0, mutable.length(), evaluator.evaluate());
                }
            });
            return mutable;
        }

        @Override
        public void invalidate ()
        {
            expression.invalidate();
        }
    }

    /**
     * Defines a transform variable.
     */
    public static class Transform3DDefinition extends ExpressionDefinition
    {
        /** The expression that determines the value. */
        @Editable
        public Transform3DExpression expression = new Transform3DExpression.Constant();

        @Override
        public Object getValue (Scope scope, ArrayList<Updater> updaters)
        {
            return getValue(scope, updaters, expression);
        }

        @Override
        public void invalidate ()
        {
            expression.invalidate();
        }
    }

    /** The name of the symbol to define. */
    @Editable
    public String name = "";

    /**
     * Retrieves the value for this definition.
     *
     * @param updaters the list of updaters to contain the value's updater.
     */
    public abstract Object getValue (Scope scope, ArrayList<Updater> updaters);

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }

    /**
     * Retrieves the value for a mutable object expression.
     */
    protected <T> Object getValue (
        Scope scope, ArrayList<Updater> updaters, ObjectExpression<T> expression)
    {
        final ObjectExpression.Evaluator<T> evaluator = expression.createEvaluator(scope);
        updaters.add(new Updater() {
            public void update () {
                evaluator.evaluate();
            }
        });
        return evaluator.evaluate();
    }
}
