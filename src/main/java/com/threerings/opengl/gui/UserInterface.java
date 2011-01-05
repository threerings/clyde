//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
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

package com.threerings.opengl.gui;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.expr.DynamicScope;
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
        for (Component comp : getComponents(tag)) {
            comp.setEnabled(enabled);
        }
    }

    /**
     * Shortcut method for setting the visibility status of all components bearing the specified
     * tag.
     */
    public void setVisible (String tag, boolean visible)
    {
        for (Component comp : getComponents(tag)) {
            comp.setVisible(visible);
        }
    }

    /**
     * Shortcut method for setting the text of all {@link TextComponent}s bearing the specified
     * tag.
     */
    public void setText (String tag, String text)
    {
        for (TextComponent comp : getComponents(tag, TextComponent.class)) {
            comp.setText(text);
        }
    }

    /**
     * Shortcut method for retrieving the text of a tagged {@link TextComponent}.
     */
    public String getText (String tag)
    {
        // first look for an editable field...
        TextComponent comp = getComponent(tag, EditableTextComponent.class);
        if (comp == null) {
            comp = getComponent(tag, TextComponent.class);
            if (comp == null) {
                log.warning("Not a text component.", "tag", tag, "components", getComponents(tag));
                return "";
            }
        }
        return comp.getText();
    }

    /**
     * Shortcut method for retrieving the selected state of a tagged {@link ToggleButton}.
     */
    public boolean isSelected (String tag)
    {
        ToggleButton comp = getComponent(tag, ToggleButton.class);
        if (comp == null) {
            log.warning("Not a toggle button.", "tag", tag, "component", getComponent(tag));
            return false;
        }
        return comp.isSelected();
    }

    /**
     * Shortcut method to add a listener to all components with the specified tag.
     */
    public void addListener (String tag, ComponentListener listener)
    {
        for (Component comp : getComponents(tag)) {
            comp.addListener(listener);
        }
    }

    /**
     * Shortcut method to remove a listener from all components with the specified tag.
     */
    public void removeListener (String tag, ComponentListener listener)
    {
        for (Component comp : getComponents(tag)) {
            comp.removeListener(listener);
        }
    }

    /**
     * Shortcut method to remove all listeners from all components with the specified tag.
     */
    public void removeAllListeners (String tag)
    {
        for (Component comp : getComponents(tag)) {
            comp.removeAllListeners();
        }
    }

    /**
     * Returns a reference to the first component registered with the specified tag, or
     * <code>null</code> if there are no such components.
     */
    public Component getComponent (String tag)
    {
        return Iterables.getFirst(getComponents(tag), null);
    }

    /**
     * Returns a reference to the first component registered with the specified tag and
     * implementing the specified class or interface, or <code>null</code> if there are
     * no such components.
     */
    public <C extends Component> C getComponent (String tag, Class<C> clazz)
    {
        return Iterables.getFirst(getComponents(tag, clazz), null);
    }

    /**
     * Returns the Components registered with the specified tag.
     */
    public <C extends Component> Iterable<C> getComponents (String tag, Class<C> clazz)
    {
        return Iterables.filter(getComponents(tag), clazz);
    }

    /**
     * Returns the Components registered with the specified tag.
     */
    public Iterable<Component> getComponents (String tag)
    {
        // parse simple paths
        int idx = tag.indexOf('/');
        if (idx == -1) {
            //return Iterables.unmodifiableIterable(_tagged.get(tag));
            return _tagged.get(tag);
        }
        // assemble all components in all sub-trees that have the correct path
        final String nextpath = tag.substring(idx + 1);
        tag = tag.substring(0, idx);
        return Iterables.concat(Iterables.transform(getComponents(tag, UserInterface.class),
            new Function<UserInterface, Iterable<Component>>() {
                public Iterable<Component> apply (UserInterface comp) {
                    return comp.getComponents(nextpath);
                }
            }));
    }

    /**
     * Returns a reference to the tagged component map.
     */
    public Map<String, Collection<Component>> getTagged ()
    {
        return _tagged.asMap();
    }

    /**
     * Replaces the component at the tag with a new component.
     */
    public boolean replace (String tag, Component newc)
    {
        List<Component> list = _tagged.get(tag);
        if (!list.isEmpty()) {
            Component oldc = list.get(0);
            if (oldc.getParent().replace(oldc, newc)) {
                list.set(0, newc);
                return true;
            }
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

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        Window window = getWindow();
        _root = (window == null) ? null : window.getRoot();
        if (_root == null) {
            return;
        }

        // play the addition sound, if any
        UserInterfaceConfig.Original original = (_config == null) ? null : _config.getOriginal();
        String sound = (original == null) ? null : original.addSound;
        if (sound != null) {
            _root.playSound(sound);
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        if (_root == null) {
            return;
        }

        // play the removal sound, if any
        UserInterfaceConfig.Original original = (_config == null) ? null : _config.getOriginal();
        String sound = (original == null) ? null : original.removeSound;
        if (sound != null) {
            _root.playSound(sound);
        }
        _root = null;
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
    protected void registerComponents (Map<String, Collection<Component>> tagged)
    {
        for (Map.Entry<String, Collection<Component>> entry : tagged.entrySet()) {
            _tagged.get(entry.getKey()).addAll(entry.getValue());
        }
    }

    /**
     * Registers a component with the specified tag.
     */
    @Scoped
    protected void registerComponent (String tag, Component comp)
    {
        _tagged.put(tag, comp);
    }

    /** The user interface scope. */
    protected DynamicScope _scope = new DynamicScope(this, "interface");

    /** The configuration of this interface. */
    protected UserInterfaceConfig _config;

    /** The sets of components registered under each tag. */
    protected ListMultimap<String, Component> _tagged = ArrayListMultimap.create();

    /** The root to which we were added. */
    protected Root _root;
}
