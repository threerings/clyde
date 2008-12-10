//
// $Id$

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
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }

    /** The model's local config library. */
    protected ConfigManager _configs = new ConfigManager();
}
