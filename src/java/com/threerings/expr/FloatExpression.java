//
// $Id$

package com.threerings.expr;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.util.DeepObject;

/**
 * A float-valued expression.
 */
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
        
        @Override // documentation inherited
        public Evaluator createEvaluator (ExpressionContext ctx)
        {
            return new Evaluator() {
                public float evaluate () {
                    return value;
                }
            };
        }
    }
    
    /**
     * A variable expression.
     */
    public static class Variable extends FloatExpression
    {
        /** The name of the variable. */
        @Editable
        public String name = "";
        
        @Override // documentation inherited
        public Evaluator createEvaluator (ExpressionContext ctx)
        {
            final MutableFloat variable = ctx.getFloatVariable(name);
            return new Evaluator() {
                public float evaluate () {
                    return variable.value;
                }
            };
        }
    }
    
    /**
     * A clock-based expression.
     */
    public static class Clock extends FloatExpression
    {
        /** The scope of the clock. */
        @Editable
        public String scope = "";
        
        @Override // documentation inherited
        public Evaluator createEvaluator (ExpressionContext ctx)
        {
            final long startTime = ctx.getStartTime(scope);
            return new Evaluator() {
                public float evaluate () {
                    return (System.currentTimeMillis() - startTime) / 1000f;
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
        public Evaluator createEvaluator (ExpressionContext ctx)
        {
            return createEvaluator(operand.createEvaluator(ctx));
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
        public Evaluator createEvaluator (ExpressionContext ctx)
        {
            return createEvaluator(
                firstOperand.createEvaluator(ctx), secondOperand.createEvaluator(ctx));
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
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { Constant.class, Variable.class, Clock.class, Negate.class,
            Add.class, Subtract.class, Multiply.class, Divide.class, Remainder.class, Pow.class,
            Sin.class, Cos.class, Tan.class };
    }
    
    /**
     * Creates an expression evaluator for the supplied context.
     */
    public abstract Evaluator createEvaluator (ExpressionContext ctx);
}
