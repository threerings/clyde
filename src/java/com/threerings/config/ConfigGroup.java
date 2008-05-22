//
// $Id$

package com.threerings.config;

import java.io.IOException;
import java.io.InputStream;

import java.util.Collection;
import java.util.HashMap;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.ObserverList;
import com.samskivert.util.StringUtil;

import com.threerings.export.BinaryImporter;

/**
 * Contains a group of managed configurations, all of the same class.
 */
public class ConfigGroup<T extends ManagedConfig>
{
    /**
     * Returns the name of this group.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Returns the class of the configurations in this group.
     */
    public Class<T> getConfigClass ()
    {
        return _cclass;
    }

    /**
     * Retrieves a configuration by name.
     */
    public T getConfig (String name)
    {
        return _configsByName.get(name);
    }

    /**
     * Retrieves a configuration by integer identifier.
     *
     */
    public T getConfig (int id)
    {
        return _configsById.get(id);
    }

    /**
     * Returns the collection of all registered configurations.
     */
    public Collection<T> getConfigs ()
    {
        return _configsByName.values();
    }

    /**
     * Adds a listener for configuration events.
     */
    public void addListener (ConfigListener<T> listener)
    {
        _listeners.add(listener);
    }

    /**
     * Removes a configuration event listener.
     */
    public void removeListener (ConfigListener<T> listener)
    {
        _listeners.remove(listener);
    }

    /**
     * Adds all of the supplied configurations to the set.
     */
    public void addConfigs (Collection<T> configs)
    {
        for (T config : configs) {
            addConfig(config);
        }
    }

    /**
     * Adds a configuration to the set.
     */
    public void addConfig (T config)
    {
        _configsByName.put(config.getName(), config);
        if (_configsById != null) {
            _configsById.put(config.getId(), config);
        }
        fireConfigAdded(config);
    }

    /**
     * Removes a configuration from the set.
     */
    public void removeConfig (T config)
    {
        _configsByName.remove(config.getName());
        if (_configsById != null) {
            _configsById.remove(config.getId());
        }
        fireConfigRemoved(config);
    }

    /**
     * Creates a new configuration group.
     */
    protected ConfigGroup (String name, Class<T> cclass, boolean ids)
    {
        _name = name;
        _cclass = cclass;

        // create the id map if specified
        if (ids) {
            _configsById = new HashIntMap<T>();
        }
    }

    /**
     * Fires a configuration added event.
     */
    protected void fireConfigAdded (T config)
    {
        final ConfigEvent<T> event = new ConfigEvent<T>(this, ConfigEvent.Type.ADDED, config);
        _listeners.apply(new ObserverList.ObserverOp<ConfigListener<T>>() {
            public boolean apply (ConfigListener<T> listener) {
                listener.configAdded(event);
                return true;
            }
        });
    }

    /**
     * Fires a configuration removed event.
     */
    protected void fireConfigRemoved (T config)
    {
        final ConfigEvent<T> event = new ConfigEvent<T>(this, ConfigEvent.Type.REMOVED, config);
        _listeners.apply(new ObserverList.ObserverOp<ConfigListener<T>>() {
            public boolean apply (ConfigListener<T> listener) {
                listener.configRemoved(event);
                return true;
            }
        });
    }

    /**
     * Fires a configuration updated event.
     */
    protected void fireConfigUpdated (T config)
    {
        final ConfigEvent<T> event = new ConfigEvent<T>(this, ConfigEvent.Type.UPDATED, config);
        _listeners.apply(new ObserverList.ObserverOp<ConfigListener<T>>() {
            public boolean apply (ConfigListener<T> listener) {
                listener.configUpdated(event);
                return true;
            }
        });
    }

    /** The name of this group. */
    protected String _name;

    /** The configuration class. */
    protected Class<T> _cclass;

    /** Configurations mapped by name. */
    protected HashMap<String, T> _configsByName = new HashMap<String, T>();

    /** Configurations mapped by integer identifier. */
    protected HashIntMap<T> _configsById;

    /** Configuration event listeners. */
    protected ObserverList<ConfigListener<T>> _listeners =
        new ObserverList<ConfigListener<T>>(ObserverList.FAST_UNSAFE_NOTIFY);
}
