//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Configurations for action targets.
 */
@EditorTypes({
    TargetConfig.Source.class, TargetConfig.Activator.class,
    TargetConfig.Tagged.class, TargetConfig.InstanceOf.class,
    TargetConfig.Intersecting.class, TargetConfig.RandomSubset.class,
    TargetConfig.NearestSubset.class, TargetConfig.FarthestSubset.class,
    TargetConfig.Conditional.class, TargetConfig.Compound.class })
public abstract class TargetConfig extends DeepObject
    implements Exportable, Streamable
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
        @Editable
        public String tag = "";

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$Tagged";
        }
    }

    /**
     * Refers to the entity or entities whose logic is an instance of the specified class.
     */
    public static class InstanceOf extends TargetConfig
    {
        /** The class of interest. */
        @Editable
        public String logicClass = "com.threerings.tudey.server.logic.PawnLogic";

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$InstanceOf";
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
        public RegionConfig region = new RegionConfig.Default();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$Intersecting";
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            region.invalidate();
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

        @Override // documentation inherited
        public void invalidate ()
        {
            target.invalidate();
        }
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
     * Superclass of the distance-based subsets.
     */
    public static abstract class DistanceSubset extends Subset
    {
        /** The reference location. */
        @Editable
        public TargetConfig location = new Source();

        @Override // documentation inherited
        public void invalidate ()
        {
            super.invalidate();
            location.invalidate();
        }
    }

    /**
     * Picks the N targets nearest to a reference target.
     */
    public static class NearestSubset extends DistanceSubset
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.TargetLogic$NearestSubset";
        }
    }

    /**
     * Picks the N targets farthest from a reference target.
     */
    public static class FarthestSubset extends DistanceSubset
    {
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

        @Override // documentation inherited
        public void invalidate ()
        {
            condition.invalidate();
            target.invalidate();
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

        @Override // documentation inherited
        public void invalidate ()
        {
            for (TargetConfig target : targets) {
                target.invalidate();
            }
        }
    }

    /**
     * Returns the name of the server-side logic class for this target.
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
