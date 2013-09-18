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
import com.threerings.math.Vector3f;

import com.threerings.expr.util.ScopeUtil;

/**
 * A 3D vector-valued expression.
 */
@EditorTypes({
    Vector3fExpression.Constant.class,
    Vector3fExpression.Reference.class,
    Vector3fExpression.Cartesian.class })
public abstract class Vector3fExpression extends ObjectExpression<Vector3f>
{
    /**
     * A constant expression.
     */
    public static class Constant extends Vector3fExpression
    {
        /** The value of the constant. */
        @Editable(step=0.01)
        public Vector3f value = new Vector3f();

        /**
         * Creates a new constant expression with the specified value.
         */
        public Constant (Vector3f value)
        {
            this.value.set(value);
        }

        /**
         * Creates a new constant expression with a value of zero.
         */
        public Constant ()
        {
        }

        @Override
        public Evaluator<Vector3f> createEvaluator (Scope scope)
        {
            return new Evaluator<Vector3f>() {
                public Vector3f evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * A reference expression.
     */
    public static class Reference extends Vector3fExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The default value of the variable. */
        @Editable(step=0.01)
        public Vector3f defvalue = new Vector3f();

        @Override
        public Evaluator<Vector3f> createEvaluator (Scope scope)
        {
            final Vector3f value = ScopeUtil.resolve(scope, name, defvalue);
            return new Evaluator<Vector3f>() {
                public Vector3f evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * An expression consisting of separate expressions for each component.
     */
    public static class Cartesian extends Vector3fExpression
    {
        /** The x component. */
        @Editable
        public FloatExpression x = new FloatExpression.Constant();

        /** The y component. */
        @Editable
        public FloatExpression y = new FloatExpression.Constant();

        /** The z component. */
        @Editable
        public FloatExpression z = new FloatExpression.Constant();

        @Override
        public Evaluator<Vector3f> createEvaluator (Scope scope)
        {
            final FloatExpression.Evaluator xeval = x.createEvaluator(scope);
            final FloatExpression.Evaluator yeval = y.createEvaluator(scope);
            final FloatExpression.Evaluator zeval = z.createEvaluator(scope);
            return new Evaluator<Vector3f>() {
                public Vector3f evaluate () {
                    return _result.set(xeval.evaluate(), yeval.evaluate(), zeval.evaluate());
                }
                protected Vector3f _result = new Vector3f();
            };
        }

        @Override
        public void invalidate ()
        {
            x.invalidate();
            y.invalidate();
            z.invalidate();
        }
    }
}
