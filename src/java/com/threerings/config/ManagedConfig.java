//
// $Id$

package com.threerings.config;

import com.samskivert.util.ObserverList;
import com.samskivert.util.StringUtil;

import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

/**
 * Represents a configuration managed by the {@link ConfigManager}.
 */
public abstract class ManagedConfig extends DeepObject
    implements Exportable
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
            _listeners = ObserverList.newFastUnsafe();
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
            }
        }
    }

    /**
     * Notes that this configuration has been updated.
     */
    public void wasUpdated ()
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
     * Initializes this config with a reference to the config manager that it should use to resolve
     * references.
     */
    protected void init (ConfigManager cfgmgr)
    {
        // nothing by default
    }

    /** The name of this configuration. */
    protected String _name;

    /** The unique identifier of this configuration. */
    protected int _id;

    /** The list of listeners to notify on change or removal. */
    @DeepOmit
    protected transient ObserverList<ConfigUpdateListener<ManagedConfig>> _listeners;
}
