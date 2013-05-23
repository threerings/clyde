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
import com.threerings.opengl.renderer.Color4f;

import com.threerings.expr.util.ScopeUtil;

/**
 * A color-valued expression.
 */
@EditorTypes({
    Color4fExpression.Constant.class,
    Color4fExpression.Reference.class,
    Color4fExpression.Blend.class })
public abstract class Color4fExpression extends ObjectExpression<Color4f>
{
    /**
     * A constant expression.
     */
    public static class Constant extends Color4fExpression
    {
        /** The value of the constant. */
        @Editable(mode="alpha")
        public Color4f value = new Color4f();

        @Override
        public Evaluator<Color4f> createEvaluator (Scope scope)
        {
            return new Evaluator<Color4f>() {
                public Color4f evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * A reference expression.
     */
    public static class Reference extends Color4fExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The default value of the variable. */
        @Editable(mode="alpha")
        public Color4f defvalue = new Color4f();

        @Override
        public Evaluator<Color4f> createEvaluator (Scope scope)
        {
            final Color4f value = ScopeUtil.resolve(scope, name, defvalue);
            return new Evaluator<Color4f>() {
                public Color4f evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * An expression that blends between two colors.
     */
    public static class Blend extends Color4fExpression
    {
        /** The first color. */
        @Editable
        public Color4fExpression firstColor = new Constant();

        /** The second color. */
        @Editable
        public Color4fExpression secondColor = new Constant();

        /** The blend factor. */
        @Editable
        public FloatExpression blendFactor = new FloatExpression.Constant();

        @Override
        public Evaluator<Color4f> createEvaluator (Scope scope)
        {
            final Evaluator<Color4f> eval1 = firstColor.createEvaluator(scope);
            final Evaluator<Color4f> eval2 = secondColor.createEvaluator(scope);
            final FloatExpression.Evaluator beval = blendFactor.createEvaluator(scope);
            return new Evaluator<Color4f>() {
                public Color4f evaluate () {
                    return eval1.evaluate().lerp(eval2.evaluate(), beval.evaluate(), _result);
                }
                protected Color4f _result = new Color4f();
            };
        }

        @Override
        public void invalidate ()
        {
            firstColor.invalidate();
            secondColor.invalidate();
            blendFactor.invalidate();
        }
    }
}
