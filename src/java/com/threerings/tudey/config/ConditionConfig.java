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
    ConditionConfig.Tagged.class, ConditionConfig.InstanceOf.class,
    ConditionConfig.DistanceWithin.class, ConditionConfig.All.class,
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
     * Determines whether an entity('s logic) is an instance of some class.
     */
    public static class InstanceOf extends ConditionConfig
    {
        /** The name of the class to check. */
        @Editable
        public String logicClass = "";

        /** The target to check. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$InstanceOf";
        }
    }

    /**
     * Determines whether the distance between two entities is within a pair of bounds.
     */
    public static class DistanceWithin extends ConditionConfig
    {
        /** The minimum distance. */
        @Editable(min=0.0, step=0.1, hgroup="m")
        public float minimum;

        /** The maximum distance. */
        @Editable(min=0.0, step=0.1, hgroup="m")
        public float maximum;

        /** The first target to check. */
        @Editable
        public TargetConfig first = new TargetConfig.Source();

        /** The second target to check. */
        @Editable
        public TargetConfig second = new TargetConfig.Activator();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$DistanceWithin";
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
