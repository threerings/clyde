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

package com.threerings.opengl.gui.config;

import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.util.DeepObject;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.util.GlContext;

/**
 * Describes a user interface.
 */
public class UserInterfaceConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the interface.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Adds the implementation's update references to the provided set.
         */
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Returns a reference to the config manager to use when resolving references.
         *
         * @param cfgmgr the config manager of the config containing the implementation.
         */
        public abstract ConfigManager getConfigManager (ConfigManager cfgmgr);

        /**
         * Creates or updates a component for this configuration.
         *
         * @param scope the component's expression scope.
         * @param comp an existing component to reuse, if possible.
         * @return either a reference to the existing component (if reused) a new component, or
         * <code>null</code> if no component could be created.
         */
        public abstract Component getComponent (GlContext ctx, Scope scope, Component comp);
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The message bundle to use for translations (or the empty string for the default). */
        @Editable
        public String bundle = "";

        /** The root of the interface. */
        @Editable
        public ComponentConfig root = new ComponentConfig.Spacer();

        @Override // documentation inherited
        public ConfigManager getConfigManager (ConfigManager cfgmgr)
        {
            return cfgmgr;
        }

        @Override // documentation inherited
        public Component getComponent (GlContext ctx, Scope scope, Component comp)
        {
            // resolve the message bundle
            MessageManager msgmgr = ctx.getMessageManager();
            MessageBundle msgs = StringUtil.isBlank(bundle) ?
                ScopeUtil.resolve(scope, "msgs", msgmgr.getBundle("global"), MessageBundle.class) :
                msgmgr.getBundle(bundle);
            return root.getComponent(ctx, scope, msgs, comp);
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The interface reference. */
        @Editable(nullable=true)
        public ConfigReference<UserInterfaceConfig> userInterface;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(UserInterfaceConfig.class, userInterface);
        }

        @Override // documentation inherited
        public ConfigManager getConfigManager (ConfigManager cfgmgr)
        {
            UserInterfaceConfig config = cfgmgr.getConfig(
                UserInterfaceConfig.class, userInterface);
            return (config == null) ? cfgmgr : config.getConfigManager();
        }

        @Override // documentation inherited
        public Component getComponent (GlContext ctx, Scope scope, Component comp)
        {
            UserInterfaceConfig config = ctx.getConfigManager().getConfig(
                UserInterfaceConfig.class, userInterface);
            return (config == null) ? null : config.getComponent(ctx, scope, comp);
        }
    }

    /** The actual interface implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Creates or updates a component for this configuration.
     *
     * @param scope the component's expression scope.
     * @param comp an existing component to reuse, if possible.
     * @return either a reference to the existing component (if reused) a new component, or
     * <code>null</code> if no component could be created.
     */
    public Component getComponent (GlContext ctx, Scope scope, Component comp)
    {
        return implementation.getComponent(ctx, scope, comp);
    }

    @Override // documentation inherited
    public void init (ConfigManager cfgmgr)
    {
        _configs.init("user_interface", cfgmgr);
        super.init(_configs);
    }

    @Override // documentation inherited
    public ConfigManager getConfigManager ()
    {
        return implementation.getConfigManager(_configs);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }

    /** The model's local config library. */
    protected ConfigManager _configs = new ConfigManager();
}
