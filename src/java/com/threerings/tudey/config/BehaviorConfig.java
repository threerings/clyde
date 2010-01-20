//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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
    BehaviorConfig.Idle.class, BehaviorConfig.Wander.class, BehaviorConfig.Patrol.class,
    BehaviorConfig.Follow.class, BehaviorConfig.Random.class })
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

        /** The amount of time to pause before rotating. */
        @Editable(min=0.0, step=0.1)
        public FloatVariable preRotationPause = new FloatVariable.Constant(0f);

        /** The amount of time to pause after rotating. */
        @Editable(min=0.0, step=0.1)
        public FloatVariable postRotationPause = new FloatVariable.Constant(0f);

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

        @Override // documentation inherited
        public void invalidate ()
        {
            target.invalidate();
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

        @Override // documentation inherited
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

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.BehaviorLogic$Random";
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            for (WeightedBehavior wbehavior : behaviors) {
                wbehavior.behavior.invalidate();
            }
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
        @Editable
        public BehaviorConfig behavior = new BehaviorConfig.Idle();
    }

    /**
     * Returns the name of the server-side logic class for this behavior.
     */
    public abstract String getLogicClassName ();

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }
}
