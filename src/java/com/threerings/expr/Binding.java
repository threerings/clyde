//
// $Id$

package com.threerings.expr;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * The superclass of the dynamic bindings.
 */
public abstract class Binding extends DeepObject
    implements Exportable
{
    /**
     * A float binding.
     */
    public static class FloatBinding extends Binding
    {
        /** The expression that determines the value. */
        @Editable(nullable=false)
        public FloatExpression expression = new FloatExpression.Constant();
        
        @Override // documentation inherited
        public Updater createUpdater (ExpressionContext ctx, Object object)
        {
            final FloatExpression.Evaluator evaluator = expression.createEvaluator(ctx);
            return new Updater() {
                public void update () {
                    float value = evaluator.evaluate();
                    
                }
            };
        }
    }
    
    /**
     * Updates the values of the bound variables.
     */
    public static abstract class Updater
    {
        /**
         * Updates the bound variables.
         */
        public abstract void update ();
    }
    
    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { FloatBinding.class };
    }
    
    /** The paths of the bound variables. */
    @Editable(width=40)
    public String[] paths = new String[0];
    
    /**
     * Creates a value updater for the supplied context and target object.
     */
    public abstract Updater createUpdater (ExpressionContext ctx, Object object);
}
