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
import com.threerings.util.DeepOmit;

import com.threerings.expr.util.ScopeUtil;

/**
 * A string-valued expression.
 */
@EditorTypes({
    StringExpression.Parsed.class, StringExpression.Constant.class,
    StringExpression.Reference.class })
public abstract class StringExpression extends ObjectExpression<String>
{
    /**
     * An expression entered as a string to be parsed.
     */
    public static class Parsed extends StringExpression
    {
        /** The expression to parse. */
        @Editable
        public String expression = "";

        @Override
        public Evaluator<String> createEvaluator (Scope scope)
        {
            if (_expr == null) {
                try {
                    _expr = parseExpression(expression);
                } catch (Exception e) {
                    // don't worry about it; it's probably being entered
                }
                if (_expr == null) {
                    _expr = new Constant("");
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
        protected transient StringExpression _expr;
    }

    /**
     * A constant expression.
     */
    public static class Constant extends StringExpression
    {
        /** The value of the constant. */
        @Editable
        public String value = "";

        /**
         * Creates a new constant expression with the supplied value.
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

        @Override
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

    /**
     * Parses the supplied string expression.
     */
    protected static StringExpression parseExpression (String expression)
        throws Exception
    {
        return (StringExpression)new ExpressionParser<Object>(new StringReader(expression)) {
            @Override protected Object handleString (String value) {
                return new Constant(value);
            }
            @Override protected Object handleIdentifier (String name) {
                Reference ref = new Reference();
                ref.name = name;
                return ref;
            }
        }.parse();
    }
}
