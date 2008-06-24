//
// $Id$

package com.threerings.expr;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.Property;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

/**
 * The superclass of the dynamic bindings.
 */
@EditorTypes({ Binding.FloatBinding.class })
public abstract class Binding extends DeepObject
    implements Exportable
{
    /**
     * A float binding.
     */
    public static class FloatBinding extends Binding
    {
        /** The expression that determines the value. */
        @Editable
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

    /** The paths of the bound variables. */
    @Editable(width=40)
    public String[] paths = new String[0];

    /** The paths of any dirty flags to set. */
    @Editable(width=40)
    public String[] dirtyFlagPaths = new String[0];

    /**
     * Creates a value updater for the supplied context and target object.
     */
    public abstract Updater createUpdater (ExpressionContext ctx, Object object);

    /** The cached paths. */
    @DeepOmit
    protected transient Property[][] _paths;

    /** The cached dirty flag paths. */
    @DeepOmit
    protected transient Property[][] _dirtyFlagPaths;
}
