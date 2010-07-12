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

package com.threerings.opengl.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.expr.DynamicScope;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;

import com.threerings.opengl.gui.config.UserInterfaceConfig;
import com.threerings.opengl.gui.event.ComponentListener;
import com.threerings.opengl.gui.layout.BorderLayout;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.GlContextWrapper;

import static com.threerings.opengl.gui.Log.*;

/**
 * A user interface component configured from a resource.
 */
public class UserInterface extends Container
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
        _scope.setParentScope(ctx.getScope());

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

    /**
     * Returns a reference to the configuration.
     */
    public UserInterfaceConfig getConfig ()
    {
        return _config;
    }

    /**
     * Shortcut method for setting the enabled status of all components bearing the specified tag.
     */
    public void setEnabled (String tag, boolean enabled)
    {
        List<Component> comps = getComponents(tag);
        if (comps != null) {
            for (int ii = 0, nn = comps.size(); ii < nn; ii++) {
                comps.get(ii).setEnabled(enabled);
            }
        }
    }

    /**
     * Shortcut method for setting the visibility status of all components bearing the specified
     * tag.
     */
    public void setVisible (String tag, boolean visible)
    {
        List<Component> comps = getComponents(tag);
        if (comps != null) {
            for (int ii = 0, nn = comps.size(); ii < nn; ii++) {
                comps.get(ii).setVisible(visible);
            }
        }
    }

    /**
     * Shortcut method for setting the text of all {@link TextComponent}s bearing the specified
     * tag.
     */
    public void setText (String tag, String text)
    {
        List<Component> comps = getComponents(tag);
        if (comps != null) {
            for (int ii = 0, nn = comps.size(); ii < nn; ii++) {
                Component comp = comps.get(ii);
                if (comp instanceof TextComponent) {
                    ((TextComponent)comp).setText(text);
                }
            }
        }
    }

    /**
     * Shortcut method for retrieving the text of a tagged {@link TextComponent}.
     */
    public String getText (String tag)
    {
        Component comp = getComponent(tag);
        if (comp instanceof TextComponent) {
            return ((TextComponent)comp).getText();
        } else {
            log.warning("Not a text component.", "tag", tag, "component", comp);
            return "";
        }
    }

    /**
     * Shortcut method for retrieving the selected state of a tagged {@link ToggleButton}.
     */
    public boolean isSelected (String tag)
    {
        Component comp = getComponent(tag);
        if (comp instanceof ToggleButton) {
            return ((ToggleButton)comp).isSelected();
        } else {
            log.warning("Not a toggle button.", "tag", tag, "component", comp);
            return false;
        }
    }

    /**
     * Shortcut method to add a listener to all components with the specified tag.
     */
    public void addListener (String tag, ComponentListener listener)
    {
        List<Component> comps = getComponents(tag);
        if (comps != null) {
            for (int ii = 0, nn = comps.size(); ii < nn; ii++) {
                comps.get(ii).addListener(listener);
            }
        }
    }

    /**
     * Shortcut method to remove a listener from all components with the specified tag.
     */
    public void removeListener (String tag, ComponentListener listener)
    {
        List<Component> comps = getComponents(tag);
        if (comps != null) {
            for (int ii = 0, nn = comps.size(); ii < nn; ii++) {
                comps.get(ii).removeListener(listener);
            }
        }
    }

    /**
     * Returns a reference to the first component registered with the specified tag, or
     * <code>null</code> if there are no such components.
     */
    public Component getComponent (String tag)
    {
        List<Component> comps = getComponents(tag);
        return (comps == null) ? null : comps.get(0);
    }

    /**
     * Returns a reference to the list of components registered with the specified tag,
     * or <code>null</code> if there are no such components.
     */
    public List<Component> getComponents (String tag)
    {
        // parse simple paths
        int idx = tag.indexOf('/');
        if (idx == -1) {
            return _tagged.get(tag);
        }
        Component first = getComponent(tag.substring(0, idx));
        return (first instanceof UserInterface) ?
            ((UserInterface)first).getComponents(tag.substring(idx + 1)) : null;
    }

    /**
     * Returns a reference to the tagged component map.
     */
    public Map<String, List<Component>> getTagged ()
    {
        return _tagged;
    }

    /**
     * Replaces the component at the tag with a new component.
     */
    public boolean replace (String tag, Component newc)
    {
        Component oldc = getComponent(tag);
        if (oldc.getParent().replace(oldc, newc)) {
            List<Component> list = _tagged.get(tag);
            for (int ii = 0, ll = list.size(); ii < ll; ii++) {
                if (oldc == list.get(ii)) {
                    list.set(ii, newc);
                    break;
                }
            }
            return true;
        }
        return false;
    }

    @Override // documentation inherited
    public void configUpdated (ConfigEvent<ManagedConfig> event)
    {
        if (event.getConfig() instanceof UserInterfaceConfig) {
            updateFromConfig();
        } else {
            super.configUpdated(event);
        }
    }

    /**
     * Updates the interface to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        _tagged.clear();
        Component ocomp = (getComponentCount() == 0) ? null : getComponent(0);
        Component ncomp = (_config == null) ? null : _config.getComponent(_ctx, _scope, ocomp);
        removeAll();
        if (ncomp != null) {
            add(ncomp, BorderLayout.CENTER);
        }
    }

    /**
     * Registers a group of components mapped by tag.
     */
    @Scoped
    protected void registerComponents (Map<String, List<Component>> tagged)
    {
        for (Map.Entry<String, List<Component>> entry : tagged.entrySet()) {
            String tag = entry.getKey();
            List<Component> comps = _tagged.get(tag);
            if (comps == null) {
                _tagged.put(tag, comps = new ArrayList<Component>());
            }
            comps.addAll(entry.getValue());
        }
    }

    /**
     * Registers a component with the specified tag.
     */
    @Scoped
    protected void registerComponent (String tag, Component comp)
    {
        List<Component> comps = _tagged.get(tag);
        if (comps == null) {
            _tagged.put(tag, comps = new ArrayList<Component>());
        }
        comps.add(comp);
    }

    /** The user interface scope. */
    protected DynamicScope _scope = new DynamicScope(this, "interface");

    /** The configuration of this interface. */
    protected UserInterfaceConfig _config;

    /** The sets of components registered under each tag. */
    protected Map<String, List<Component>> _tagged = Maps.newHashMap();
}
