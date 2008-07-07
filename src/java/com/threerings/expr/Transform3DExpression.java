//
// $Id$

package com.threerings.expr;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.expr.util.ScopeUtil;

/**
 * A transform-valued expression.
 */
@EditorTypes({
    Transform3DExpression.Constant.class,
    Transform3DExpression.Reference.class,
    Transform3DExpression.Uniform.class })
public abstract class Transform3DExpression extends ObjectExpression<Transform3D>
{
    /**
     * A constant expression.
     */
    public static class Constant extends Transform3DExpression
    {
        /** The value of the constant. */
        @Editable(step=0.01)
        public Transform3D value = new Transform3D();

        @Override // documentation inherited
        public Evaluator<Transform3D> createEvaluator (Scope scope)
        {
            return new Evaluator<Transform3D>() {
                public Transform3D evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * A reference expression.
     */
    public static class Reference extends Transform3DExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The default value of the variable. */
        @Editable(step=0.01)
        public Transform3D defvalue = new Transform3D();

        @Override // documentation inherited
        public Evaluator<Transform3D> createEvaluator (Scope scope)
        {
            final Transform3D value = ScopeUtil.resolve(scope, name, defvalue);
            return new Evaluator<Transform3D>() {
                public Transform3D evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * An expression consisting of separate expressions for translation, rotation, and scale.
     */
    public static class Uniform extends Transform3DExpression
    {
        /** The translation component. */
        @Editable
        public Vector3fExpression translation = new Vector3fExpression.Constant();

        /** The rotation component. */
        @Editable
        public QuaternionExpression rotation = new QuaternionExpression.Constant();

        /** The scale component. */
        @Editable
        public FloatExpression scale = new FloatExpression.Constant();

        @Override // documentation inherited
        public Evaluator<Transform3D> createEvaluator (Scope scope)
        {
            final Evaluator<Vector3f> teval = translation.createEvaluator(scope);
            final Evaluator<Quaternion> reval = rotation.createEvaluator(scope);
            final FloatExpression.Evaluator seval = scale.createEvaluator(scope);
            return new Evaluator<Transform3D>() {
                public Transform3D evaluate () {
                    return _result.set(teval.evaluate(), reval.evaluate(), seval.evaluate());
                }
                protected Transform3D _result = new Transform3D();
            };
        }
    }
}
