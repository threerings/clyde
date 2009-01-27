//
// $Id$

package com.threerings.tudey.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.probs.FloatVariable;
import com.threerings.util.DeepObject;

/**
 * Configurations for agent behavior.
 */
@EditorTypes({
    BehaviorConfig.Idle.class, BehaviorConfig.Wander.class,
    BehaviorConfig.Patrol.class })
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
        @Editable(min=0.0, step=0.1)
        public FloatVariable directionChangeInterval = new FloatVariable.Uniform(1f, 2f);

        /** The variable that determines how we change directions. */
        @Editable(min=-180, max=+180, scale=Math.PI/180.0)
        public FloatVariable directionChange =
            new FloatVariable.Uniform(-FloatMath.PI, +FloatMath.PI);

        /** The radius from the origin within which we may wander. */
        @Editable(min=0.0, step=0.1)
        public float radius = 100f;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Wander";
        }
    }

    /**
     * Patrols a path, area, etc.
     */
    public static class Patrol extends BehaviorConfig
    {
        /** The tag identifying the path (etc.) to patrol. */
        @Editable
        public String tag = "";

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Patrol";
        }
    }

    /**
     * Returns the name of the server-side logic class for this behavior.
     */
    public abstract String getLogicClassName ();
}
