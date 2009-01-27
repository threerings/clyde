//
// $Id$

package com.threerings.tudey.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Configurations for action targets.
 */
@EditorTypes({
    TargetConfig.Source.class, TargetConfig.Activator.class,
    TargetConfig.Tagged.class, TargetConfig.Intersecting.class,
    TargetConfig.RandomSubset.class, TargetConfig.NearestSubset.class,
    TargetConfig.FarthestSubset.class, TargetConfig.Conditional.class,
    TargetConfig.Compound.class })
public abstract class TargetConfig extends DeepObject
    implements Exportable
{
    /**
     * Refers to the source of the action.
     */
    public static class Source extends TargetConfig
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$Source";
        }
    }

    /**
     * Refers to the entity that triggered the action.
     */
    public static class Activator extends TargetConfig
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$Activator";
        }
    }

    /**
     * Refers to the entity or entities bearing a certain tag.
     */
    public static class Tagged extends TargetConfig
    {
        /** The tag of interest. */
        @Editable(hgroup="t")
        public String tag = "";

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$Tagged";
        }
    }

    /**
     * Refers to all actors intersecting the given region.
     */
    public static class Intersecting extends TargetConfig
    {
        /** Whether or not to include intersecting actors. */
        @Editable(hgroup="a")
        public boolean actors = true;

        /** Whether or not to include intersecting scene entries. */
        @Editable(hgroup="a")
        public boolean entries;

        /** The region of interest. */
        @Editable
        public TargetConfig region = new Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$Intersecting";
        }
    }

    /**
     * Base class for the limiting filters for targets.
     */
    public static abstract class Subset extends TargetConfig
    {
        /** The (maximum) size of the subset. */
        @Editable(min=0)
        public int size = 1;

        /** The contained target. */
        @Editable
        public TargetConfig target = new Source();
    }

    /**
     * Picks a random subset of the targets.
     */
    public static class RandomSubset extends Subset
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$RandomSubset";
        }
    }

    /**
     * Picks the N targets nearest to a reference target.
     */
    public static class NearestSubset extends Subset
    {
        /** The reference location. */
        @Editable
        public TargetConfig location = new Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$NearestSubset";
        }
    }

    /**
     * Picks the N targets farthest from a reference target.
     */
    public static class FarthestSubset extends Subset
    {
        /** The reference location. */
        @Editable
        public TargetConfig location = new Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$FarthestSubset";
        }
    }

    /**
     * Includes only targets that satisfy a condition.
     */
    public static class Conditional extends TargetConfig
    {
        /** The condition that the targets must satisfy. */
        @Editable
        public ConditionConfig condition = new ConditionConfig.Tagged();

        /** The contained target. */
        @Editable
        public TargetConfig target = new Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$Conditional";
        }
    }

    /**
     * Refers to multiple entities.
     */
    public static class Compound extends TargetConfig
    {
        /** The component targets. */
        @Editable
        public TargetConfig[] targets = new TargetConfig[0];

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$Compound";
        }
    }

    /**
     * Returns the name of the server-side logic class for this target.
     */
    public abstract String getLogicClassName ();
}
