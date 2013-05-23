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

import com.threerings.io.Streamable;

import com.threerings.config.ConfigManager;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.Strippable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.util.PreloadableSet;

/**
 * Configurations for handler conditions.
 */
@EditorTypes({
    ConditionConfig.Tagged.class, ConditionConfig.InstanceOf.class,
    ConditionConfig.Intersecting.class, ConditionConfig.IntersectsScene.class,
    ConditionConfig.DistanceWithin.class, ConditionConfig.Random.class,
    ConditionConfig.Limit.class, ConditionConfig.All.class,
    ConditionConfig.Any.class, ConditionConfig.FlagSet.class,
    ConditionConfig.Cooldown.class, ConditionConfig.Not.class,
    ConditionConfig.Always.class, ConditionConfig.Evaluate.class,
    ConditionConfig.Action.class, ConditionConfig.Is.class,
    ConditionConfig.DateRange.class })
@Strippable
public abstract class ConditionConfig extends DeepObject
    implements Exportable, Streamable
{
    /**
     * Determines whether an entity has a tag.
     */
    public static class Tagged extends ConditionConfig
    {
        /** The tag of interest. */
        @Editable(hgroup="t")
        public String tag = "";

        /** Whether or not all targets must match the condition (as opposed to any). */
        @Editable(hgroup="t")
        public boolean all;

        /** The target to check. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Tagged";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Determines whether an entity('s logic) is an instance of some class.
     */
    public static class InstanceOf extends ConditionConfig
    {
        /** The name of the class to check. */
        @Editable(hgroup="c")
        public String logicClass = "com.threerings.tudey.server.logic.PawnLogic";

        /** Whether or not all targets must match the condition (as opposed to any). */
        @Editable(hgroup="c")
        public boolean all;

        /** The target to check. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$InstanceOf";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Determines whether two regions intersect.
     */
    public static class Intersecting extends ConditionConfig
    {
        /** Whether or not to require all targets in the first region. */
        @Editable(hgroup="a")
        public boolean allFirst;

        /** Whether or not to require all targets in the second region. */
        @Editable(hgroup="a")
        public boolean allSecond;

        /** The first region to check. */
        @Editable
        public RegionConfig first = new RegionConfig.Default();

        /** The second region to check. */
        @Editable
        public RegionConfig second = new RegionConfig.Default();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Intersecting";
        }

        @Override
        public void invalidate ()
        {
            first.invalidate();
            second.invalidate();
        }
    }

    /**
     * Determines if a region intersects the scene based on a collision mask.
     */
    public static class IntersectsScene extends ConditionConfig
    {
        /** The region. */
        @Editable
        public RegionConfig region = new RegionConfig.Default();

        /** The collision mask. */
        @Editable(editor="mask", mode="collision")
        public int collisionMask;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$IntersectsScene";
        }

        @Override
        public void invalidate ()
        {
            region.invalidate();
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

        /** Whether or not to require all targets in the first region. */
        @Editable(hgroup="a")
        public boolean allFirst;

        /** Whether or not to require all targets in the second region. */
        @Editable(hgroup="a")
        public boolean allSecond;

        /** The first target to check. */
        @Editable
        public TargetConfig first = new TargetConfig.Source();

        /** The second target to check. */
        @Editable
        public TargetConfig second = new TargetConfig.Activator();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$DistanceWithin";
        }

        @Override
        public void invalidate ()
        {
            first.invalidate();
            second.invalidate();
        }
    }

    /**
     * Satisfied with a fixed probability.
     */
    public static class Random extends ConditionConfig
    {
        /** The probability that the condition is satisfied. */
        @Editable(min=0, max=1, step=0.01)
        public float probability = 0.5f;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Random";
        }
    }

    /**
     * Will be satisfied a fixed number of times.
     */
    public static class Limit extends ConditionConfig
    {
        /** The number of times this condition is satisfied. */
        @Editable
        public int limit = 1;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Limit";
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

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$All";
        }

        @Override
        public void invalidate ()
        {
            for (ConditionConfig condition : conditions) {
                condition.invalidate();
            }
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

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Any";
        }

        @Override
        public void invalidate ()
        {
            for (ConditionConfig condition : conditions) {
                condition.invalidate();
            }
        }
    }

    /**
     * Determines whether an actor's flag is set.
     */
    public static class FlagSet extends ConditionConfig
    {
        /** The name of the flag definition. */
        @Editable(hgroup="f")
        public String flagName = "WARP";

        /** If we're checking for set or not set. */
        @Editable(hgroup="f")
        public boolean set = true;

        /** The target to check. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$FlagSet";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
        }
    }

    /**
     * Ensures a cooldown time is met between satisfied conditions.
     */
    public static class Cooldown extends ConditionConfig
    {
        /** The amount of cooldown time. */
        @Editable
        public int time = 0;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Cooldown";
        }
    }

    /**
     * Satisfied if the component condition is not satisfied.
     */
    public static class Not extends ConditionConfig
    {
        /** The component condition. */
        @Editable
        public ConditionConfig condition = new ConditionConfig.Tagged();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Not";
        }

        @Override
        public void invalidate ()
        {
            condition.invalidate();
        }
    }

    /**
     * Satisfied always.
     */
    public static class Always extends ConditionConfig
    {
        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Always";
        }
    }

    /**
     * Satisfied if the expression evaluates to true.
     */
    public static class Evaluate extends ConditionConfig
    {
        /** The expression to evaluate. */
        @Editable
        public ExpressionConfig expression = new ExpressionConfig.Constant();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Evaluate";
        }

        @Override
        public void invalidate ()
        {
            expression.invalidate();
        }
    }

    /**
     * Satisfied if executing the aciton returns true.
     */
    public static class Action extends ConditionConfig
    {
        /** The action to perform. */
        @Editable
        public ActionConfig action = new ActionConfig.SpawnActor();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Action";
        }

        @Override
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            action.getPreloads(cfgmgr, preloads);
        }

        @Override
        public void invalidate ()
        {
            action.invalidate();
        }
    }

    /**
     * Determines whether two entities are the same
     */
    public static class Is extends ConditionConfig
    {
        /** Whether or not all targets must match the condition (as opposed to any). */
        @Editable(hgroup="t")
        public boolean all;

        /** The target to check. */
        @Editable
        public TargetConfig target = new TargetConfig.Tagged();

        /** The target to check. */
        @Editable
        public TargetConfig source = new TargetConfig.Source();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Is";
        }

        @Override
        public void invalidate ()
        {
            target.invalidate();
            source.invalidate();
        }
    }

    /**
     * Determines if the current date is within the range.
     */
    public static class DateRange extends ConditionConfig
    {
        /** The starting date. */
        @Editable(editor="datetime", nullable=true, hgroup="a")
        public Long start;

        /** The ending date. */
        @Editable(editor="datetime", nullable=true, hgroup="a")
        public Long end;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$DateRange";
        }
    }

    /**
     * Returns the name of the server-side logic class for this condition.
     */
    public abstract String getLogicClassName ();

    /**
     * Adds the resources to preload for this action into the provided set.
     */
    public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
    {
        // nothing by default
    }

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }
}
