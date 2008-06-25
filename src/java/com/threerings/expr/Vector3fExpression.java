//
// $Id$

package com.threerings.expr;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;

import com.threerings.math.Vector3f;

/**
 * A vector-valued expression.
 */
@EditorTypes({
    Vector3fExpression.Constant.class,
    Vector3fExpression.Variable.class,
    Vector3fExpression.Cartesian.class })
public abstract class Vector3fExpression extends ObjectExpression<Vector3f>
{
    /**
     * A constant expression.
     */
    public static class Constant extends Vector3fExpression
    {
        /** The value of the constant. */
        @Editable
        public Vector3f value = new Vector3f();

        @Override // documentation inherited
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
     * A variable expression.
     */
    public static class Variable extends Vector3fExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The default value of the variable. */
        @Editable
        public Vector3f defvalue = new Vector3f();

        @Override // documentation inherited
        public Evaluator<Vector3f> createEvaluator (Scope scope)
        {
            final Vector3f value = scope.getVariable(name, defvalue);
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

        @Override // documentation inherited
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
    }
}
