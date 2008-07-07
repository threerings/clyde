//
// $Id$

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
    Color4fExpression.Variable.class,
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

        @Override // documentation inherited
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
     * A variable expression.
     */
    public static class Variable extends Color4fExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The default value of the variable. */
        @Editable(mode="alpha")
        public Color4f defvalue = new Color4f();

        @Override // documentation inherited
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

        @Override // documentation inherited
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
    }
}
