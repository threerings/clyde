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

        @Override
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

        @Override
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

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }
}
