//
// $Id$

package com.threerings.expr;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.util.DeepObject;

import com.threerings.expr.util.ScopeUtil;

/**
 * A float-valued expression.
 */
@EditorTypes({
    FloatExpression.Constant.class, FloatExpression.Reference.class,
    FloatExpression.Clock.class, FloatExpression.Negate.class,
    FloatExpression.Add.class, FloatExpression.Subtract.class,
    FloatExpression.Multiply.class, FloatExpression.Divide.class,
    FloatExpression.Remainder.class, FloatExpression.Pow.class,
    FloatExpression.Sin.class, FloatExpression.Cos.class,
    FloatExpression.Tan.class })
public abstract class FloatExpression extends DeepObject
    implements Exportable
{
    /**
     * A constant expression.
     */
    public static class Constant extends FloatExpression
    {
        /** The value of the constant. */
        @Editable(step=0.01)
        public float value;

        /**
         * Creates a new constant expression with the specified value.
         */
        public Constant (float value)
        {
            this.value = value;
        }

        /**
         * Creates a new constant expression with a value of zero.
         */
        public Constant ()
        {
        }

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            return new Evaluator() {
                public float evaluate () {
                    return value;
                }
            };
        }
    }

    /**
     * A reference expression.
     */
    public static class Reference extends FloatExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The default value of the variable. */
        @Editable(step=0.01)
        public float defvalue;

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            final MutableFloat reference = ScopeUtil.resolve(
                scope, name, new MutableFloat(defvalue));
            return new Evaluator() {
                public float evaluate () {
                    return reference.value;
                }
            };
        }
    }

    /**
     * A clock-based expression.
     */
    public static class Clock extends FloatExpression
    {
        /** The scope of the epoch reference. */
        @Editable
        public String scope = "";

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            String name = this.scope.trim();
            name = (name.length() > 0) ? (name + ":" + Scope.EPOCH) : Scope.EPOCH;
            MutableLong defvalue = new MutableLong(System.currentTimeMillis());
            final MutableLong epoch = ScopeUtil.resolve(scope, name, defvalue);
            final MutableLong now = ScopeUtil.resolve(scope, Scope.NOW, defvalue);
            return new Evaluator() {
                public float evaluate () {
                    return (now.value - epoch.value) / 1000f;
                }
            };
        }
    }

    /**
     * The superclass of the unary operations.
     */
    public static abstract class UnaryOperation extends FloatExpression
    {
        /** The operand expression. */
        @Editable
        public FloatExpression operand = new Constant();

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            return createEvaluator(operand.createEvaluator(scope));
        }

        /**
         * Creates the evaluator for this expression, given the evaluator for its operand.
         */
        protected abstract Evaluator createEvaluator (Evaluator eval);
    }

    /**
     * Negates its operand.
     */
    public static class Negate extends UnaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval)
        {
            return new Evaluator() {
                public float evaluate () {
                    return -eval.evaluate();
                }
            };
        }
    }

    /**
     * Computes the sine of its operand.
     */
    public static class Sin extends UnaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval)
        {
            return new Evaluator() {
                public float evaluate () {
                    return FloatMath.sin(eval.evaluate());
                }
            };
        }
    }

    /**
     * Computes the cosine of its operand.
     */
    public static class Cos extends UnaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval)
        {
            return new Evaluator() {
                public float evaluate () {
                    return FloatMath.cos(eval.evaluate());
                }
            };
        }
    }

    /**
     * Computes the tangent of its operand.
     */
    public static class Tan extends UnaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval)
        {
            return new Evaluator() {
                public float evaluate () {
                    return FloatMath.tan(eval.evaluate());
                }
            };
        }
    }

    /**
     * The superclass of the binary operations.
     */
    public static abstract class BinaryOperation extends FloatExpression
    {
        /** The first operand expression. */
        @Editable
        public FloatExpression firstOperand = new Constant();

        /** The second operand expression. */
        @Editable
        public FloatExpression secondOperand = new Constant();

        @Override // documentation inherited
        public Evaluator createEvaluator (Scope scope)
        {
            return createEvaluator(
                firstOperand.createEvaluator(scope), secondOperand.createEvaluator(scope));
        }

        /**
         * Creates the evaluator for this expression, given the evaluators for its operands.
         */
        protected abstract Evaluator createEvaluator (Evaluator eval1, Evaluator eval2);
    }

    /**
     * Adds its operands.
     */
    public static class Add extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval1, final Evaluator eval2)
        {
            return new Evaluator() {
                public float evaluate () {
                    return eval1.evaluate() + eval2.evaluate();
                }
            };
        }
    }

    /**
     * Subtracts the second operand from the first.
     */
    public static class Subtract extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval1, final Evaluator eval2)
        {
            return new Evaluator() {
                public float evaluate () {
                    return eval1.evaluate() - eval2.evaluate();
                }
            };
        }
    }

    /**
     * Multiplies its operands.
     */
    public static class Multiply extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval1, final Evaluator eval2)
        {
            return new Evaluator() {
                public float evaluate () {
                    return eval1.evaluate() * eval2.evaluate();
                }
            };
        }
    }

    /**
     * Divides the first operand by the second.
     */
    public static class Divide extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval1, final Evaluator eval2)
        {
            return new Evaluator() {
                public float evaluate () {
                    return eval1.evaluate() / eval2.evaluate();
                }
            };
        }
    }

    /**
     * Computes the floating point remainder when the first operand is divided by the second.
     */
    public static class Remainder extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval1, final Evaluator eval2)
        {
            return new Evaluator() {
                public float evaluate () {
                    return FloatMath.IEEEremainder(eval1.evaluate(), eval2.evaluate());
                }
            };
        }
    }

    /**
     * Raises the first operand to the power of the second.
     */
    public static class Pow extends BinaryOperation
    {
        @Override // documentation inherited
        protected Evaluator createEvaluator (final Evaluator eval1, final Evaluator eval2)
        {
            return new Evaluator() {
                public float evaluate () {
                    return FloatMath.pow(eval1.evaluate(), eval2.evaluate());
                }
            };
        }
    }

    /**
     * Performs the actual evaluation of the expression.
     */
    public static abstract class Evaluator
    {
        /**
         * Evaluates and returns the current value of the expression.
         */
        public abstract float evaluate ();
    }

    /**
     * Creates an expression evaluator for the supplied context.
     */
    public abstract Evaluator createEvaluator (Scope scope);
}
