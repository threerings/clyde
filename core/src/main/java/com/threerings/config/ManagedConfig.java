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

package com.threerings.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import com.samskivert.util.ObserverList;
import com.samskivert.util.WeakObserverList;
import com.samskivert.util.Tuple;

import com.threerings.resource.ResourceManager;
import com.threerings.resource.ResourceManager.ModificationObserver;

import com.threerings.config.util.DependencyGatherer;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.Strippable;
import com.threerings.editor.util.EditorContext;
import com.threerings.editor.util.Validator;
import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import static com.threerings.ClydeLog.log;

/**
 * Represents a configuration managed by the {@link ConfigManager}.
 */
public abstract class ManagedConfig extends DeepObject
    implements Exportable, ConfigUpdateListener<ManagedConfig>, ModificationObserver
{
    /**
     * Set whether to store comments. In a production environment comments can be disabled
     * and they will take up no space. This should be called prior to the config manager
     * starting up.
     */
    public static void setStoreComments (boolean storeComments)
    {
        if (storeComments != (_comments != null)) {
            _comments = storeComments
                ? new MapMaker().concurrencyLevel(1).weakKeys().<ManagedConfig, String>makeMap()
                : null;
        }
    }

    /** The type of this config. */
    @Editable // see setter
    public final Class<?> getConfigType ()
    {
        return getClass();
    }

    /** The type of this config. */
    @Editable(editor="configType", weight=-Double.MAX_VALUE)
    public final void setConfigType (Class<?> val)
    {
        // Do nothing.
        // The 'configType' editor will never actually call this, it will instead make a new
        // config of the new type, and overwrite this config.
    }

    /** A helpful comment explaining what this config is and/or used for. */
    @Editable // see setter for attributes
    @Strippable
    public String getComment ()
    {
        if (_comments != null) {
            String comm = _comments.get(this);
            if (comm != null) {
                return comm;
            }
        }
        return "";
    }

    /** A helpful comment explaining what this config is and/or used for. */
    @Editable(height=3, width=40, collapsible=true, weight=-Double.MAX_VALUE/2)
    @Strippable
    public void setComment (String comment)
    {
        if (_comments != null) {
            if ("".equals(comment)) {
                _comments.remove(this);
            } else {
                _comments.put(this, comment);
            }
        }
    }

    @Override
    public Object copy (Object dest, Object outer)
    {
        Object result = super.copy(dest, outer);
        ((ManagedConfig)result).setComment(getComment());
        return result;
    }

    @Override
    public boolean equals (Object other)
    {
        // factor-in our comment, which is stored externally.
        // Note: this doesn't need to be included in the hashcode.
        return super.equals(other) && getComment().equals(((ManagedConfig)other).getComment());
    }

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
     * Returns a reference to the config group containing this config, or null if it's
     * a resource config.
     */
    public ConfigGroup<ManagedConfig> getConfigGroup ()
    {
        return _cfgmgr.getGroup(this);
    }

    /**
     * Returns the derived instance with the supplied arguments.
     */
    public final ManagedConfig getInstance (String firstKey, Object firstValue, Object... otherArgs)
    {
        return getInstance(null, firstKey, firstValue, otherArgs);
    }

    /**
     * Returns the derived instance in the specified scope with the supplied arguments.
     */
    public final ManagedConfig getInstance (
        Scope scope, String firstKey, Object firstValue, Object... otherArgs)
    {
        return getInstance(scope, new ArgumentMap(firstKey, firstValue, otherArgs));
    }

    /**
     * Returns the derived instance in the specified scope.
     */
    public final ManagedConfig getInstance (Scope scope)
    {
        return getInstance(scope, null);
    }

    /**
     * Returns the derived instance with the supplied arguments.
     */
    public final ManagedConfig getInstance (ArgumentMap args)
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
    public void addListener (ConfigUpdateListener<?> listener)
    {
        if (_listeners == null) {
            // we disable duplicate checking for performance; don't fuck up
            (_listeners = WeakObserverList.newSafeInOrder()).setCheckDuplicates(false);
            addUpdateDependencies();
        }
        @SuppressWarnings("unchecked")
        ConfigUpdateListener<ManagedConfig> mlistener =
                (ConfigUpdateListener<ManagedConfig>)listener;
        _listeners.add(mlistener);
    }

    /**
     * Removes a listener from the list.
     */
    public void removeListener (ConfigUpdateListener<?> listener)
    {
        if (_listeners != null) {
            @SuppressWarnings("unchecked")
            ConfigUpdateListener<ManagedConfig> mlistener =
                    (ConfigUpdateListener<ManagedConfig>)listener;
            _listeners.remove(mlistener);
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
    public boolean validateReferences (Validator validator)
    {
        return validator.validate(_cfgmgr, this);
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
     * Custom export handling.
     * @see Exportable
     */
    public void writeFields (Exporter out)
        throws java.io.IOException
    {
        String comment = getComment();
        if (!"".equals(comment)) {
            out.write("comment", comment);
        }
        out.defaultWriteFields();
    }

    /**
     * Custom import handling.
     * @see Exportable
     */
    public void readFields (Importer in)
        throws java.io.IOException
    {
        in.defaultReadFields();
        setComment(in.read("comment", ""));
    }

    /**
     * Obsolete method that collected references configs within this config.
     * It was a manual process, and couldn't handle config references that were arguments to
     * other configs.
     *
     * @deprecated this is no longer necessary, it is handled reflectively.
     */
    @Deprecated
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        // nothing
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
        // gather up the dependencies
        SetMultimap<Class<? extends ManagedConfig>, ConfigReference<?>> refs =
                DependencyGatherer.gather(_cfgmgr, this);

        _updateConfigs = new ArrayList<ManagedConfig>(refs.size());
        for (Map.Entry<Class<? extends ManagedConfig>, Set<ConfigReference<?>>> entry :
                Multimaps.asMap(refs).entrySet()) {
            @SuppressWarnings("unchecked")
            Class<ManagedConfig> mclass = (Class<ManagedConfig>)entry.getKey();
            for (ConfigReference<?> ref : entry.getValue()) {
                @SuppressWarnings("unchecked")
                ConfigReference<ManagedConfig> mref = (ConfigReference<ManagedConfig>)ref;
                ManagedConfig config = _cfgmgr.getConfig(mclass, mref);
                if (config != null) {
                    config.addListener(this);
                    _updateConfigs.add(config);
                }
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

    /** The list of configs to which we are listening for updates.
     * This is usually null and is typically only used when the client creates a DConfigDirector,
     * in dev environments. */
    @DeepOmit
    protected transient ArrayList<ManagedConfig> _updateConfigs;

    /** The list of resources to which we are listening for modifications.
     * This is usually null and is typically only used when the client creates a DConfigDirector,
     * in dev environments. */
    @DeepOmit
    protected transient HashSet<String> _updateResources;

    /** Storage for comments. Can be replaced by null in a production environment. */
    protected static Map<ManagedConfig, String> _comments;
    static {
        setStoreComments(true);
    }
}
