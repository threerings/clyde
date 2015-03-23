//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
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

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.Strippable;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.probs.FloatVariable;
import com.threerings.util.DeepObject;

/**
 * Configurations for agent behavior.
 */
@Strippable
public class BehaviorConfig extends ParameterizedConfig
{
    /**
      * Contains the actual implementation of the behavior.
      */
    @EditorTypes({
        Original.class, Derived.class, Wander.class, WanderCollision.class, GridWander.class,
        Patrol.class, Follow.class, Random.class, Scripted.class, Combined.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        @Deprecated
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Returns a reference to the config's underlying original implementation.
         */
        public abstract Original getOriginal (ConfigManager cfgmgr);

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
    }

    /**
     * Superclass of the original implementations.
     */
    public static class Original extends Implementation
    {
        /**
         * Returns the name of the server-side logic class to use for the actor.
         */
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Idle";
        }

        @Override
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The actor reference. */
        @Editable(nullable=true)
        public ConfigReference<BehaviorConfig> behavior;

        @Override
        public Original getOriginal (ConfigManager cfgmgr)
        {
            BehaviorConfig config = cfgmgr.getConfig(BehaviorConfig.class, behavior);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }
    }

    /**
     * Base class for behaviors that require periodic (re)evaluation.
     */
    public static abstract class Evaluating extends Original
    {
        /** The variable that determines how long we wait between evaluations. */
        @Editable(min=0.0, step=0.1)
        public FloatVariable evaluationInterval = new FloatVariable.Constant(2f);
    }

    /**
     * Base class for pathing behaviors.
     */
    public static abstract class Pathing extends Evaluating
    {
        /** The variable that determines the facing angle required to start moving. */
        @Editable(min=0, max=360, scale=Math.PI/180.0)
        public float moveFaceRange = 0;
    }

    /**
     * Base for wander behaviors.
     */
    public abstract static class BaseWander extends Evaluating
    {
        /** The amount of time to pause before rotating. */
        @Editable(min=0.0, step=0.1)
        public FloatVariable preRotationPause = new FloatVariable.Constant(0f);

        /** The amount of time to pause after rotating. */
        @Editable(min=0.0, step=0.1)
        public FloatVariable postRotationPause = new FloatVariable.Constant(0f);
    }

    /**
     * Wanders around randomly.
     */
    public static class Wander extends BaseWander
    {
        /** The variable that determines how we change directions. */
        @Editable(min=-180, max=+180, scale=Math.PI/180.0)
        public FloatVariable directionChange =
            new FloatVariable.Uniform(-FloatMath.PI, +FloatMath.PI);

        /** The radius from the origin within which we may wander. */
        @Editable(min=0.0, step=0.1)
        public float radius = 100f;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Wander";
        }
    }

    /**
     * Wanders around randomly, runs an action when it collides.
     */
    public static class WanderCollision extends BaseWander
    {
        @Editable(nullable=true)
        public ActionConfig action;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$WanderCollision";
        }
    }

    public enum GridTurn { REVERSE, LEFT, RIGHT, RANDOM };

    /**
     * Wanders around on the grid.
     */
    public static class GridWander extends BaseWander
    {
        /** If we are restricted to linear directions. */
        @Editable(hgroup="l")
        public GridTurn gridTurn = GridTurn.REVERSE;

        /** If we rotate based on the evaluation period. */
        @Editable(hgroup="l")
        public boolean evaluationRotate;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$GridWander";
        }
    }

    /**
     * Patrols a path, area, etc.
     */
    public static class Patrol extends Pathing
    {
        /** The target to patrol. */
        @Editable
        public TargetConfig target = new TargetConfig.Tagged();

        /** The radius within which we consider branching nodes (or negative for no branching). */
        @Editable(min=-1.0, step=0.1)
        public float branchRadius = -1f;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Patrol";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Follows another actor.
     */
    public static class Follow extends Pathing
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

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Follow";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Chooses a sub-behavior randomly according to their weights for each evaluation.
     */
    public static class Random extends Evaluating
    {
        /** The weighted component behaviors. */
        @Editable
        public WeightedBehavior[] behaviors = new WeightedBehavior[0];

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Random";
        }
    }

    /**
     * A behavior that has a set of scripted actions.
     */
    public static class Scripted extends Original
    {
        /** The script steps to follow. */
        @Editable
        public ScriptConfig[] steps = new ScriptConfig[0];

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Scripted";
        }
    }

    /**
     * A combined behavior.
     */
    public static class Combined extends Original
    {
        /** The first behavior. */
        @Editable(nullable=true)
        public ConfigReference<BehaviorConfig> first;

        /** The second behavior. */
        @Editable(nullable=true)
        public ConfigReference<BehaviorConfig> second;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Combined";
        }
    }

    /**
     * Combines a behavior with its weight.
     */
    public static class WeightedBehavior extends DeepObject
        implements Exportable
    {
        /** The weight of the behavior. */
        @Editable(min=0, step=0.01)
        public float weight = 1f;

        /** The behavior itself. */
        @Editable(nullable=true)
        public ConfigReference<BehaviorConfig> behavior;
    }

    /** The actual behavior implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Returns a reference to the config's underlying original implementation.
     */
    public Original getOriginal (ConfigManager cfgmgr)
    {
        return implementation.getOriginal(cfgmgr);
    }

    @Override
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }
}
