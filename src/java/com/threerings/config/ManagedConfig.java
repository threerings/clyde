//
// $Id$

package com.threerings.config;

import java.util.ArrayList;
import java.util.HashSet;

import com.samskivert.util.ObserverList;
import com.samskivert.util.WeakObserverList;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.resource.ResourceManager;
import com.threerings.resource.ResourceManager.ModificationObserver;

import com.threerings.editor.util.EditorContext;
import com.threerings.export.Exportable;
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
     * Sets the unique identifier of this configuration.
     */
    public void setId (int id)
    {
        _id = id;
    }

    /**
     * Returns the unique identifier of this configuration.
     */
    public int getId ()
    {
        return _id;
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
        return getInstance(new ArgumentMap(firstKey, firstValue, otherArgs));
    }

    /**
     * Returns the derived instance with the supplied arguments.
     */
    public ManagedConfig getInstance (ArgumentMap args)
    {
        return this;
    }

    /**
     * Adds a listener to notify on updates.
     */
    public void addListener (ConfigUpdateListener listener)
    {
        if (_listeners == null) {
            _listeners = WeakObserverList.newFastUnsafe();
            addUpdateDependencies();
        }
        @SuppressWarnings("unchecked") ConfigUpdateListener<ManagedConfig> mlistener =
            (ConfigUpdateListener<ManagedConfig>)listener;
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
    }

    /**
     * Resolves the update dependencies and subscribes to them.
     */
    protected void addUpdateDependencies ()
    {
        // add the config dependencies
        ConfigReferenceSet refs = new ConfigReferenceSet();
        getUpdateReferences(refs);
        _updateConfigs = new ArrayList<ManagedConfig>();
        for (Tuple<Class, ConfigReference> ref : refs) {
            @SuppressWarnings("unchecked") Class<ManagedConfig> mclass =
                (Class<ManagedConfig>)ref.left;
            @SuppressWarnings("unchecked") ConfigReference<ManagedConfig> mref =
                (ConfigReference<ManagedConfig>)ref.right;
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

    /** The unique identifier of this configuration. */
    protected int _id;

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
