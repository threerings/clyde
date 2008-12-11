//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.Scope;

import com.threerings.opengl.gui.config.UserInterfaceConfig;
import com.threerings.opengl.gui.layout.BorderLayout;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.GlContextWrapper;

/**
 * A user interface component configured from a resource.
 */
public class UserInterface extends Container
    implements ConfigUpdateListener<UserInterfaceConfig>
{
    /**
     * Creates a new user interface.
     */
    public UserInterface (GlContext ctx)
    {
        this(ctx, (UserInterfaceConfig)null);
    }

    /**
     * Creates a new interface with the named configuration.
     */
    public UserInterface (GlContext ctx, String name)
    {
        this(ctx, ctx.getConfigManager().getConfig(UserInterfaceConfig.class, name));
    }

    /**
     * Creates a new interface with the named configuration and arguments.
     */
    public UserInterface (
        GlContext ctx, String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        this(ctx, ctx.getConfigManager().getConfig(
            UserInterfaceConfig.class, name, firstKey, firstValue, otherArgs));
    }

    /**
     * Creates a new interface with the referenced configuration.
     */
    public UserInterface (GlContext ctx, ConfigReference<UserInterfaceConfig> ref)
    {
        this(ctx, ctx.getConfigManager().getConfig(UserInterfaceConfig.class, ref));
    }

    /**
     * Creates a new interface with the given configuration.
     */
    public UserInterface (GlContext ctx, UserInterfaceConfig config)
    {
        super(ctx, new BorderLayout());

        _ctx = new GlContextWrapper(ctx) {
            public ConfigManager getConfigManager () {
                return (_config == null) ?
                    _wrapped.getConfigManager() : _config.getConfigManager();
            }
        };
        setConfig(config);
    }

    /**
     * Returns a reference to the interface's scope.
     */
    public DynamicScope getScope ()
    {
        return _scope;
    }

    /**
     * Sets the configuration.
     */
    public void setConfig (String name)
    {
        setConfig(_ctx.getConfigManager().getConfig(UserInterfaceConfig.class, name));
    }

    /**
     * Sets the configuration.
     */
    public void setConfig (ConfigReference<UserInterfaceConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(UserInterfaceConfig.class, ref));
    }

    /**
     * Sets the configuration.
     */
    public void setConfig (
        String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        setConfig(_ctx.getConfigManager().getConfig(
            UserInterfaceConfig.class, name, firstKey, firstValue, otherArgs));
    }

    /**
     * Sets the configuration.
     */
    public void setConfig (UserInterfaceConfig config)
    {
        if (_config == config) {
            return;
        }
        if (_config != null) {
            _config.removeListener(this);
        }
        if ((_config = config) != null) {
            _config.addListener(this);
        }
        updateFromConfig();
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<UserInterfaceConfig> event)
    {
        updateFromConfig();
    }

    /**
     * Updates the interface to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        Component ocomp = (getComponentCount() == 0) ? null : getComponent(0);
        Component ncomp = (_config == null) ? null : _config.getComponent(_ctx, _scope, ocomp);
        removeAll();
        if (ncomp != null) {
            add(ncomp, BorderLayout.CENTER);
        }
    }

    /** The user interface scope. */
    protected DynamicScope _scope = new DynamicScope(this, "interface");

    /** The configuration of this interface. */
    protected UserInterfaceConfig _config;
}
