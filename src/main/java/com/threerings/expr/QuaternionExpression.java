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
import com.threerings.math.Quaternion;

import com.threerings.expr.util.ScopeUtil;

/**
 * A color-valued expression.
 */
@EditorTypes({
    QuaternionExpression.Constant.class,
    QuaternionExpression.Reference.class,
    QuaternionExpression.Angles.class })
public abstract class QuaternionExpression extends ObjectExpression<Quaternion>
{
    /**
     * A constant expression.
     */
    public static class Constant extends QuaternionExpression
    {
        /** The value of the constant. */
        @Editable
        public Quaternion value = new Quaternion();

        @Override
        public Evaluator<Quaternion> createEvaluator (Scope scope)
        {
            return new Evaluator<Quaternion>() {
                public Quaternion evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * A reference expression.
     */
    public static class Reference extends QuaternionExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The default value of the variable. */
        @Editable
        public Quaternion defvalue = new Quaternion();

        @Override
        public Evaluator<Quaternion> createEvaluator (Scope scope)
        {
            final Quaternion value = ScopeUtil.resolve(scope, name, defvalue);
            return new Evaluator<Quaternion>() {
                public Quaternion evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * Contains the angles of rotation as separate expressions.
     */
    public static class Angles extends QuaternionExpression
    {
        /** The rotation about the x axis. */
        @Editable
        public FloatExpression x = new FloatExpression.Constant();

        /** The rotation about the y axis. */
        @Editable
        public FloatExpression y = new FloatExpression.Constant();

        /** The rotation about the z axis. */
        @Editable
        public FloatExpression z = new FloatExpression.Constant();

        @Override
        public Evaluator<Quaternion> createEvaluator (Scope scope)
        {
            final FloatExpression.Evaluator xeval = x.createEvaluator(scope);
            final FloatExpression.Evaluator yeval = y.createEvaluator(scope);
            final FloatExpression.Evaluator zeval = z.createEvaluator(scope);
            return new Evaluator<Quaternion>() {
                public Quaternion evaluate () {
                    return _result.fromAngles(
                        xeval.evaluate(), yeval.evaluate(), zeval.evaluate());
                }
                protected Quaternion _result = new Quaternion();
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
