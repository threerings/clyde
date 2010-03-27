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
    ExpressionConfig.GreaterEquals.class })
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
     * Adds one to the result of the sub-expression.
     */
    public static class Increment extends ExpressionConfig
    {
        /** The operand of the expression. */
        @Editable
        public ExpressionConfig operand = new ExpressionConfig.Constant();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ExpressionLogic$Increment";
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            operand.invalidate();
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
