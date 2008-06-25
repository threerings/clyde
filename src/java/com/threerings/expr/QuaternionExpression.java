//
// $Id$

package com.threerings.expr;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;

import com.threerings.math.Quaternion;

/**
 * A color-valued expression.
 */
@EditorTypes({
    QuaternionExpression.Constant.class,
    QuaternionExpression.Variable.class,
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

        @Override // documentation inherited
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
     * A variable expression.
     */
    public static class Variable extends QuaternionExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The default value of the variable. */
        @Editable
        public Quaternion defvalue = new Quaternion();

        @Override // documentation inherited
        public Evaluator<Quaternion> createEvaluator (Scope scope)
        {
            final Quaternion value = scope.getVariable(name, defvalue);
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

        @Override // documentation inherited
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
    }
}
