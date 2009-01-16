//
// $Id$

package com.threerings.tudey.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Configurations for handler conditions.
 */
@EditorTypes({
    ConditionConfig.Tagged.class, ConditionConfig.All.class,
    ConditionConfig.Any.class })
public abstract class ConditionConfig extends DeepObject
    implements Exportable
{
    /**
     * Determines whether an entity has a tag.
     */
    public static class Tagged extends ConditionConfig
    {
        /** The tag of interest. */
        @Editable
        public String tag = "";

        /** The target to check. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Tagged";
        }
    }

    /**
     * Satisfied if all of the component conditions are satisfied.
     */
    public static class All extends ConditionConfig
    {
        /** The component conditions. */
        @Editable
        public ConditionConfig[] conditions = new ConditionConfig[0];

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$All";
        }
    }

    /**
     * Satisfied if any of the component conditions are satisfied.
     */
    public static class Any extends ConditionConfig
    {
        /** The component conditions. */
        @Editable
        public ConditionConfig[] conditions = new ConditionConfig[0];

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Any";
        }
    }

    /**
     * Returns the name of the server-side logic class for this condition.
     */
    public abstract String getLogicClassName ();
}
