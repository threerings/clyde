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

package com.threerings.opengl.gui.config;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * A script used to control a user interface.
 */
public class InterfaceScriptConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the script.
     */
    @EditorTypes({ Original.class, Derived.class })
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
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The loop duration, or zero for unlooped. */
        @Editable(min=0.0, step=0.01)
        public float loopDuration;

        /** The actions of which the script is composed. */
        @Editable
        public TimedAction[] actions = new TimedAction[0];

        @Override
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        @Override
        public void invalidate ()
        {
            for (TimedAction taction : actions) {
                taction.action.invalidate();
            }
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The script reference. */
        @Editable(nullable=true)
        public ConfigReference<InterfaceScriptConfig> interfaceScript;

        @Override
        public Original getOriginal (ConfigManager cfgmgr)
        {
            InterfaceScriptConfig config = cfgmgr.getConfig(
                InterfaceScriptConfig.class, interfaceScript);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }
    }

    /**
     * An action to perform after a specific time interval.
     */
    public static class TimedAction extends DeepObject
        implements Exportable
    {
        /** The time at which to perform the action. */
        @Editable(min=0, step=0.01)
        public float time;

        /** The action to perform. */
        @Editable
        public ActionConfig action = new ActionConfig.CallFunction();
    }

    /** The actual script implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Retrieves a reference to the underlying original implementation.
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
