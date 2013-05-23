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
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.expr.util.ScopeUtil;

/**
 * A 2D transform-valued expression.
 */
@EditorTypes({
    Transform2DExpression.Constant.class, Transform2DExpression.Reference.class,
    Transform2DExpression.Uniform.class })
public abstract class Transform2DExpression extends ObjectExpression<Transform2D>
{
    /**
     * A constant expression.
     */
    public static class Constant extends Transform2DExpression
    {
        /** The value of the constant. */
        @Editable(step=0.01)
        public Transform2D value = new Transform2D();

        @Override
        public Evaluator<Transform2D> createEvaluator (Scope scope)
        {
            return new Evaluator<Transform2D>() {
                public Transform2D evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * A reference expression.
     */
    public static class Reference extends Transform2DExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The default value of the variable. */
        @Editable(step=0.01)
        public Transform2D defvalue = new Transform2D();

        @Override
        public Evaluator<Transform2D> createEvaluator (Scope scope)
        {
            final Transform2D value = ScopeUtil.resolve(scope, name, defvalue);
            return new Evaluator<Transform2D>() {
                public Transform2D evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * An expression consisting of separate expressions for translation, rotation, and scale.
     */
    public static class Uniform extends Transform2DExpression
    {
        /** The translation component. */
        @Editable
        public Vector2fExpression translation = new Vector2fExpression.Constant();

        /** The rotation component. */
        @Editable
        public FloatExpression rotation = new FloatExpression.Constant();

        /** The scale component. */
        @Editable
        public FloatExpression scale = new FloatExpression.Constant(1f);

        @Override
        public Evaluator<Transform2D> createEvaluator (Scope scope)
        {
            final Evaluator<Vector2f> teval = translation.createEvaluator(scope);
            final FloatExpression.Evaluator reval = rotation.createEvaluator(scope);
            final FloatExpression.Evaluator seval = scale.createEvaluator(scope);
            return new Evaluator<Transform2D>() {
                public Transform2D evaluate () {
                    return _result.set(teval.evaluate(), reval.evaluate(), seval.evaluate());
                }
                protected Transform2D _result = new Transform2D();
            };
        }

        @Override
        public void invalidate ()
        {
            translation.invalidate();
            rotation.invalidate();
            scale.invalidate();
        }
    }
}
