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

package com.threerings.config;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import com.samskivert.util.ObserverList;
import com.samskivert.util.WeakObserverList;
import com.samskivert.util.Tuple;

import com.threerings.resource.ResourceManager;
import com.threerings.resource.ResourceManager.ModificationObserver;

import com.threerings.editor.util.EditorContext;
import com.threerings.editor.util.PropertyUtil;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

/**
 * Represents a configuration managed by the {@link ConfigManager}.
 */
public abstract class ManagedConfig extends DeepObject
    implements Exportable, ConfigUpdateListener<ManagedConfig>, ModificationObserver
{
    /**
     * Sets the name of this configuration.
     */
    public void setName (String name)
    {
        _name = name;
    }

    /**
     * Returns the name of this configuration.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Returns a reference to this configuration based on its name and arguments.
     */
    public ConfigReference<? extends ManagedConfig> getReference ()
    {
        return new ConfigReference<ManagedConfig>(_name);
    }

    /**
     * Returns a reference to the config manager to use when resolving references within this
     * config.
     */
    public ConfigManager getConfigManager ()
    {
        return _cfgmgr;
    }

    /**
     * Returns the derived instance with the supplied arguments.
     */
    public ManagedConfig getInstance (String firstKey, Object firstValue, Object... otherArgs)
    {
        return getInstance(null, firstKey, firstValue, otherArgs);
    }

    /**
     * Returns the derived instance in the specified scope with the supplied arguments.
     */
    public ManagedConfig getInstance (
        Scope scope, String firstKey, Object firstValue, Object... otherArgs)
    {
        return getInstance(scope, new ArgumentMap(firstKey, firstValue, otherArgs));
    }

    /**
     * Returns the derived instance in the specified scope.
     */
    public ManagedConfig getInstance (Scope scope)
    {
        return getInstance(scope, null);
    }

    /**
     * Returns the derived instance with the supplied arguments.
     */
    public ManagedConfig getInstance (ArgumentMap args)
    {
        return getInstance(null, args);
    }

    /**
     * Returns the derived instance in the specified scope with the supplied arguments.
     */
    public ManagedConfig getInstance (Scope scope, ArgumentMap args)
    {
        return this;
    }

    /**
     * Adds a listener to notify on updates.
     */
    public void addListener (ConfigUpdateListener listener)
    {
        if (_listeners == null) {
            _listeners = WeakObserverList.newList(ObserverList.FAST_UNSAFE_NOTIFY, true);
            addUpdateDependencies();
        }
        @SuppressWarnings("unchecked") ConfigUpdateListener<ManagedConfig> mlistener =
            listener;
        _listeners.add(mlistener);
    }

    /**
     * Removes a listener from the list.
     */
    public void removeListener (ConfigUpdateListener listener)
    {
        if (_listeners != null) {
            _listeners.remove(listener);
            if (_listeners.isEmpty()) {
                _listeners = null;
                clearUpdateDependencies();
            }
        }
    }

    /**
     * Initializes this config with a reference to the config manager that it should use to resolve
     * references.
     */
    public void init (ConfigManager cfgmgr)
    {
        _cfgmgr = cfgmgr;
    }

    /**
     * Updates this configuration from its external source, if any.
     *
     * @param force if true, reload the source data even if it has already been loaded.
     */
    public void updateFromSource (EditorContext ctx, boolean force)
    {
        // nothing by default
    }

    /**
     * Notes that this configuration has been updated.
     */
    public void wasUpdated ()
    {
        // update the dependencies
        if (_updateConfigs != null) {
            clearUpdateDependencies();
            addUpdateDependencies();
        }
        fireConfigUpdated();
    }

    /**
     * Validates the references in this config.
     *
     * @return true if the references are valid
     */
    public boolean validateReferences (String where, PrintStream out)
    {
        Set<Tuple<Class<?>, String>> configs = Sets.newHashSet();
        Set<String> resources = Sets.newHashSet();
        PropertyUtil.getReferences(_cfgmgr, this, configs, resources);
        return PropertyUtil.validateReferences(where, _cfgmgr, configs, resources, out);
    }

    /**
     * Validates the outer object references in this config.
     */
    public void validateOuters (String where)
    {
        // nothing by default
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ManagedConfig> event)
    {
        fireConfigUpdated();
    }

    // documentation inherited from interface ModificationObserver
    public void resourceModified (String path, long lastModified)
    {
        fireConfigUpdated();
    }

    /**
     * Collects all of the references within this config to configs that, when updated, should
     * trigger a call to {@link #fireConfigUpdated}.
     */
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        // nothing by default
    }

    /**
     * Collects the paths of all resources referenced by this config that, when modified, should
     * trigger a call to {@link #fireConfigUpdated}.
     */
    protected void getUpdateResources (HashSet<String> paths)
    {
        // nothing by default
    }

    /**
     * Fires a configuration updated event.
     */
    protected void fireConfigUpdated ()
    {
        if (_listeners != null) {
            final ConfigEvent<ManagedConfig> event = new ConfigEvent<ManagedConfig>(this, this);
            _listeners.apply(new ObserverList.ObserverOp<ConfigUpdateListener<ManagedConfig>>() {
                public boolean apply (ConfigUpdateListener<ManagedConfig> listener) {
                    listener.configUpdated(event);
                    return true;
                }
            });
        }
        maybeFireOnConfigManager();
    }

    /**
     * Fires a configuration updated event on the config manager if appropriate.
     */
    protected void maybeFireOnConfigManager ()
    {
        if (_cfgmgr != null) {
            // use the root config manager for resource classes
            ConfigManager mgr = _cfgmgr.isResourceClass(getClass()) ? _cfgmgr.getRoot() : _cfgmgr;
            mgr.fireConfigUpdated(this);
        }
    }

    /**
     * Resolves the update dependencies and subscribes to them.
     */
    protected void addUpdateDependencies ()
    {
        // add the config dependencies
        ConfigReferenceSet refs = new ConfigReferenceSet();
        getUpdateReferences(refs);
        _updateConfigs = new ArrayList<ManagedConfig>(refs.size());
        for (Tuple<Class<?>, ConfigReference> ref : refs) {
            @SuppressWarnings("unchecked") Class<ManagedConfig> mclass =
                (Class<ManagedConfig>)ref.left;
            @SuppressWarnings("unchecked") ConfigReference<ManagedConfig> mref =
                ref.right;
            ManagedConfig config = _cfgmgr.getConfig(mclass, mref);
            if (config != null) {
                config.addListener(this);
                _updateConfigs.add(config);
            }
        }

        // add the resource dependencies
        getUpdateResources(_updateResources = new HashSet<String>());
        ResourceManager rsrcmgr = _cfgmgr.getResourceManager();
        for (String path : _updateResources) {
            rsrcmgr.addModificationObserver(path, this);
        }
    }

    /**
     * Unsubscribes from the update dependencies.
     */
    protected void clearUpdateDependencies ()
    {
        for (int ii = 0, nn = _updateConfigs.size(); ii < nn; ii++) {
            _updateConfigs.get(ii).removeListener(this);
        }
        _updateConfigs = null;

        ResourceManager rsrcmgr = _cfgmgr.getResourceManager();
        for (String path : _updateResources) {
            rsrcmgr.removeModificationObserver(path, this);
        }
        _updateResources = null;
    }

    /** The name of this configuration. */
    protected String _name;

    /** The config manager that we use to resolve references. */
    @DeepOmit
    protected transient ConfigManager _cfgmgr;

    /** The list of listeners to notify on change or removal. */
    @DeepOmit
    protected transient WeakObserverList<ConfigUpdateListener<ManagedConfig>> _listeners;

    /** The list of configs to which we are listening for updates. */
    @DeepOmit
    protected transient ArrayList<ManagedConfig> _updateConfigs;

    /** The list of resources to which we are listening for modifications. */
    @DeepOmit
    protected transient HashSet<String> _updateResources;
}
