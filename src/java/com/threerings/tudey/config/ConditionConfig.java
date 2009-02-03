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

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Configurations for handler conditions.
 */
@EditorTypes({
    ConditionConfig.Tagged.class, ConditionConfig.InstanceOf.class,
    ConditionConfig.Intersecting.class, ConditionConfig.DistanceWithin.class,
    ConditionConfig.All.class, ConditionConfig.Any.class })
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
        public String logicClass = "com.threerings.tudey.server.logic.PawnLogic";

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
     * Determines whether two regions intersect.
     */
    public static class Intersecting extends ConditionConfig
    {
        /** The first region to check. */
        @Editable
        public RegionConfig first = new RegionConfig.Default();

        /** The second region to check. */
        @Editable
        public RegionConfig second = new RegionConfig.Default();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ConditionLogic$Intersecting";
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
