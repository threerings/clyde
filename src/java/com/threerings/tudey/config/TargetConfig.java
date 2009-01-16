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
    TargetConfig.Tagged.class, TargetConfig.Compound.class })
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

        /** The maximum number of tagged entities to affect. */
        @Editable(min=1, hgroup="t")
        public int limit = 1;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$Tagged";
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
     * Returns the name of the server-side logic class for this action.
     */
    public abstract String getLogicClassName ();
}
