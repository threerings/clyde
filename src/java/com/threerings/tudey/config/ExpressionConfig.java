//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

package com.threerings.tudey.config;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Configurations for (weakly typed) server-side expressions.
 */
@EditorTypes({
    ExpressionConfig.Constant.class, ExpressionConfig.Reference.class,
    ExpressionConfig.Previous.class, ExpressionConfig.Increment.class,
    ExpressionConfig.Decrement.class, ExpressionConfig.Negate.class,
    ExpressionConfig.Add.class, ExpressionConfig.Subtract.class,
    ExpressionConfig.Multiply.class, ExpressionConfig.Divide.class,
    ExpressionConfig.Remainder.class, ExpressionConfig.Not.class,
    ExpressionConfig.And.class, ExpressionConfig.Or.class,
    ExpressionConfig.Xor.class, ExpressionConfig.Less.class,
    ExpressionConfig.Greater.class, ExpressionConfig.Equals.class,
    ExpressionConfig.LessEquals.class, ExpressionConfig.GreaterEquals.class })
public abstract class ExpressionConfig extends DeepObject
    implements Exportable, Streamable
{
    /**
     * A constant expression.
     */
    public static class Constant extends ExpressionConfig
    {
        /** The value of the constant. */
        @Editable
        public String value = "";

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Constant";
        }
    }

    /**
     * A reference to another variable.
     */
    public static class Reference extends ExpressionConfig
    {
        /** The name of the variable. */
        @Editable
        public String name = "";

        /** The target containing the variable. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Reference";
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * The previous value.
     */
    public static class Previous extends ExpressionConfig
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Previous";
        }
    }

    /**
     * Base class for unary operations.
     */
    public static abstract class UnaryOperation extends ExpressionConfig
    {
        /** The operand of the expression. */
        @Editable
        public ExpressionConfig operand = new ExpressionConfig.Constant();

        @Override // documentation inherited
        public void invalidate ()
        {
            operand.invalidate();
        }
    }

    /**
     * Adds one to the operand.
     */
    public static class Increment extends UnaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Increment";
        }
    }

    /**
     * Subtracts one from the operand.
     */
    public static class Decrement extends UnaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Decrement";
        }
    }

    /**
     * Negates the operand.
     */
    public static class Negate extends UnaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Negate";
        }
    }

    /**
     * Base class for binary operations.
     */
    public static abstract class BinaryOperation extends ExpressionConfig
    {
        /** The first operand of the expression. */
        @Editable
        public ExpressionConfig firstOperand = new ExpressionConfig.Constant();

        /** The second operand of the expression. */
        @Editable
        public ExpressionConfig secondOperand = new ExpressionConfig.Constant();

        @Override // documentation inherited
        public void invalidate ()
        {
            firstOperand.invalidate();
            secondOperand.invalidate();
        }
    }

    /**
     * Computes the sum of the operands.
     */
    public static class Add extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Add";
        }
    }

    /**
     * Computes the difference of the operands.
     */
    public static class Subtract extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Subtract";
        }
    }

    /**
     * Computes the product of the operands.
     */
    public static class Multiply extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Multiply";
        }
    }

    /**
     * Computes the quotient of the operands.
     */
    public static class Divide extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Divide";
        }
    }

    /**
     * Computes the remainder of division of the operands.
     */
    public static class Remainder extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Remainder";
        }
    }

    /**
     * Computes the logical NOT of the operand.
     */
    public static class Not extends UnaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Not";
        }
    }

    /**
     * Computes the logical AND of the first and second operands.
     */
    public static class And extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$And";
        }
    }

    /**
     * Computes the logical OR of the first and second operands.
     */
    public static class Or extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Or";
        }
    }

    /**
     * Computes the logical XOR of the first and second operands.
     */
    public static class Xor extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Xor";
        }
    }

    /**
     * Finds out whether the first operand is less then the second.
     */
    public static class Less extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Less";
        }
    }

    /**
     * Finds out whether the first operand is greater than the second.
     */
    public static class Greater extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Greater";
        }
    }

    /**
     * Finds out whether the first operand is equal to the second.
     */
    public static class Equals extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Equals";
        }
    }

    /**
     * Finds out whether the first operand is less than or equal to the second.
     */
    public static class LessEquals extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$LessEquals";
        }
    }

    /**
     * Finds out whether the first operand is greater than or equal to the second.
     */
    public static class GreaterEquals extends BinaryOperation
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$GreaterEquals";
        }
    }

    /**
     * Returns the name of the server-side logic class for this expression.
     */
    public abstract String getLogicClassName ();

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }
}
