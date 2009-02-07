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
            // first look for a builder reference, then for a variable
            final StringBuilder reference = ScopeUtil.resolve(
                scope, name, null, StringBuilder.class);
            if (reference != null) {
                return new Evaluator<String>() {
                    public String evaluate () {
                        return reference.toString();
                    }
                };
            }
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
