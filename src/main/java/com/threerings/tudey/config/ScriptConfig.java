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

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Configurations for scripted actions in a behavior.
 */
@EditorTypes({
    ScriptConfig.Wait.class, ScriptConfig.Move.class,
    ScriptConfig.Rotate.class, ScriptConfig.Condition.class,
    ScriptConfig.Goto.class })
public abstract class ScriptConfig extends DeepObject
    implements Exportable
{
    /**
     * A script that waits some amount of time before completing.
     */
    public static class Wait extends ScriptConfig
    {
        /** The wait time. */
        @Editable
        public int wait = 0;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ScriptLogic$Wait";
        }
    }

    /**
     * Moves to a target location.
     */
    public static class Move extends ScriptConfig
    {
        /** The target location. */
        @Editable
        public TargetConfig target = new TargetConfig.Source();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ScriptLogic$Move";
        }
    }

    /**
     * Rotates to a target orientation.
     */
    public static class Rotate extends ScriptConfig
    {
        /** The target rotation. */
        @Editable(min=0.0, max=360.0, scale=Math.PI/180.0)
        public float direction = 0;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ScriptLogic$Rotate";
        }
    }

    /**
     * A script that waits until a condition is satisfied before completing.
     */
    public static class Condition extends ScriptConfig
    {
        /** The condition. */
        @Editable
        public ConditionConfig condition = new ConditionConfig.Always();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ScriptLogic$Condition";
        }
    }

    /**
     * Goes to a specific script step.
     */
    public static class Goto extends ScriptConfig
    {
        /** The step to goto. */
        @Editable(min=0)
        public int step = 0;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ScriptLogic$Goto";
        }
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
