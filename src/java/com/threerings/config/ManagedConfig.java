//
// $Id$

package com.threerings.config;

import java.util.ArrayList;

import com.samskivert.util.ObserverList;
import com.samskivert.util.WeakObserverList;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.editor.util.EditorContext;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

/**
 * Represents a configuration managed by the {@link ConfigManager}.
 */
public abstract class ManagedConfig extends DeepObject
    implements Exportable, ConfigUpdateListener<ManagedConfig>
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
            addDependencies();
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
                clearDependencies();
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
        if (_dependencies != null) {
            clearDependencies();
            addDependencies();
        }
        fireConfigUpdated();
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ManagedConfig> event)
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
     * Resolves the dependent configs and subscribes to them.
     */
    protected void addDependencies ()
    {
        ConfigReferenceSet refs = new ConfigReferenceSet();
        getUpdateReferences(refs);
        _dependencies = new ArrayList<ManagedConfig>();
        for (Tuple<Class, ConfigReference> ref : refs) {
            @SuppressWarnings("unchecked") Class<ManagedConfig> mclass =
                (Class<ManagedConfig>)ref.left;
            @SuppressWarnings("unchecked") ConfigReference<ManagedConfig> mref =
                (ConfigReference<ManagedConfig>)ref.right;
            ManagedConfig config = _cfgmgr.getConfig(mclass, mref);
            if (config != null) {
                config.addListener(this);
                _dependencies.add(config);
            }
        }
    }

    /**
     * Unsubscribes from the dependent configs.
     */
    protected void clearDependencies ()
    {
        for (int ii = 0, nn = _dependencies.size(); ii < nn; ii++) {
            _dependencies.get(ii).removeListener(this);
        }
        _dependencies = null;
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
    protected transient ArrayList<ManagedConfig> _dependencies;
}
