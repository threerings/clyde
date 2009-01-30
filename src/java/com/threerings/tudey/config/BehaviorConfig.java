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
    BehaviorConfig.Patrol.class, BehaviorConfig.Follow.class })
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
     * Base class for behaviors that require periodic (re)evaluation.
     */
    public static abstract class Evaluating extends BehaviorConfig
    {
        /** The variable that determines how long we wait between evaluations. */
        @Editable(min=0.0, step=0.1)
        public FloatVariable evaluationInterval = new FloatVariable.Constant(2f);
    }

    /**
     * Wanders around randomly.
     */
    public static class Wander extends Evaluating
    {
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
    public static class Patrol extends Evaluating
    {
        /** The target to patrol. */
        @Editable
        public TargetConfig target = new TargetConfig.Tagged();

        /** The radius within which we consider branching nodes (or negative for no branching). */
        @Editable(min=-1.0, step=0.1)
        public float branchRadius = -1f;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Patrol";
        }
    }

    /**
     * Follows another actor.
     */
    public static class Follow extends Evaluating
    {
        /** The target to follow. */
        @Editable
        public TargetConfig target = new TargetConfig.Tagged();

        /** The minimum distance to maintain from the target. */
        @Editable(min=0.0, step=0.1, hgroup="d")
        public float minimumDistance = 1f;

        /** The maximum distance to maintain. */
        @Editable(min=0.0, step=0.1, hgroup="d")
        public float maximumDistance = 5f;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Follow";
        }
    }

    /**
     * Returns the name of the server-side logic class for this behavior.
     */
    public abstract String getLogicClassName ();
}
