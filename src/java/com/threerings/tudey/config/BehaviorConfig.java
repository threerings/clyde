//
// $Id$

package com.threerings.tudey.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.probs.FloatVariable;
import com.threerings.util.DeepObject;

/**
 * Configurations for agent behavior.
 */
@EditorTypes({ BehaviorConfig.Idle.class, BehaviorConfig.Wander.class })
public abstract class BehaviorConfig extends DeepObject
    implements Exportable
{
    /**
     * Stands in one place.
     */
    public static class Idle extends BehaviorConfig
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Idle";
        }
    }

    /**
     * Wanders around randomly.
     */
    public static class Wander extends BehaviorConfig
    {
        /** The variable that determines how long we will travel between direction changes. */
        @Editable
        public FloatVariable directionChangeInterval = new FloatVariable.Uniform(1f, 2f);

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Wander";
        }
    }

    /**
     * Returns the name of the server-side logic class for this behavior.
     */
    public abstract String getLogicClassName ();
}
