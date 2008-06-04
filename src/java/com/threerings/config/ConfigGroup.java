//
// $Id$

package com.threerings.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.ObserverList;
import com.samskivert.util.StringUtil;
import com.samskivert.util.QuickSort;

import com.threerings.export.BinaryImporter;
import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;

import static com.threerings.ClydeLog.*;

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
            int id = config.getId();
            if (id == 0 || _configsById.containsKey(id)) {
                // no id or id in use; assign new id
                config.setId(id = assignId());
            } else {
                // make sure we don't assign this id
                _highestId = Math.max(_highestId, id);
                _freeIds.remove(Integer.valueOf(id));
            }
            _configsById.put(id, config);
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
     * Notes that a configuration has changed.
     */
    public void updateConfig (T config)
    {
        fireConfigUpdated(config);
    }

    /**
     * Saves this group's configurations.
     */
    public void save ()
    {
        save(getConfigFile(true));
    }

    /**
     * Saves this group's configurations to the specified file.
     */
    public void save (File file)
    {
        save(_configsByName.values(), file);
    }

    /**
     * Saves the provided collection of configurations to a file.
     */
    public void save (Collection<T> configs, File file)
    {
        // put all the configs into an array and sort them by name
        @SuppressWarnings("unchecked") T[] array =
            (T[])Array.newInstance(_cclass, configs.size());
        configs.toArray(array);
        QuickSort.sort(array, new Comparator<T>() {
            public int compare (T c1, T c2) {
                return c1.getName().compareTo(c2.getName());
            }
        });

        try {
            Exporter out = new XMLExporter(new FileOutputStream(file));
            out.writeObject(array);
            out.close();

        } catch (IOException e) {
            log.warning("Error writing configurations [file=" + file + "].", e);
        }
    }

    /**
     * Reverts to the last saved configurations.
     */
    public void revert ()
    {
        load(getConfigFile(true));
    }

    /**
     * Loads the configurations from the specified file.
     */
    public void load (File file)
    {
        load(file, false);
    }

    /**
     * Loads the configurations from the specified file.
     *
     * @param merge if true, merge with the existing configurations; do not delete configurations
     * that do not exist in the file.
     */
    public void load (File file, boolean merge)
    {
        // read in the array of configurations
        Object array;
        try {
            Importer in = new XMLImporter(new FileInputStream(file));
            array = in.readObject();
            in.close();

        } catch (IOException e) {
            log.warning("Error reading configurations [file=" + file + "].", e);
            return;
        }
        @SuppressWarnings("unchecked") T[] nconfigs = (T[])array;

        // add any configurations that don't already exist and update those that do
        HashSet<String> names = new HashSet<String>();
        for (T nconfig : nconfigs) {
            String name = nconfig.getName();
            names.add(name);
            T oconfig = _configsByName.get(name);
            if (oconfig == null) {
                addConfig(nconfig);
            } else if (!nconfig.equals(oconfig)) {
                nconfig.copy(oconfig);
                updateConfig(oconfig);
            }
        }
        if (merge) {
            return;
        }

        // remove any configurations not present in the array (if not merging)
        @SuppressWarnings("unchecked") T[] oconfigs =
            (T[])Array.newInstance(_cclass, _configsByName.size());
        _configsByName.values().toArray(oconfigs);
        for (T oconfig : oconfigs) {
            if (!names.contains(oconfig.getName())) {
                removeConfig(oconfig);
            }
        }
    }

    /**
     * Creates a new configuration group.
     */
    protected ConfigGroup (ConfigManager cfgmgr, String name, Class<T> cclass, boolean ids)
    {
        _cfgmgr = cfgmgr;
        _name = name;
        _cclass = cclass;

        // create the id map if specified
        if (ids) {
            _configsById = new HashIntMap<T>();
            _freeIds = new ArrayList<Integer>();
        }

        // load the existing configurations (first checking for a binary file, then an xml file)
        if (readConfigs(false) || readConfigs(true)) {
            log.debug("Read configurations for group " + _name + ".");
        }
    }

    /**
     * Attempts to read the initial set of configurations.
     *
     * @return true if successful, false otherwise.
     */
    protected boolean readConfigs (boolean xml)
    {
        File file = getConfigFile(xml);
        if (!file.exists()) {
            return false;
        }
        try {
            FileInputStream fin = new FileInputStream(file);
            Importer in = xml ? new XMLImporter(fin) : new BinaryImporter(fin);
            @SuppressWarnings("unchecked") T[] configs = (T[])in.readObject();
            for (T config : configs) {
                _configsByName.put(config.getName(), config);
                if (_configsById != null) {
                    int id = config.getId();
                    _highestId = Math.max(_highestId, id);
                    _configsById.put(id, config);
                }
            }
            for (int id = _highestId - 1; id >= 1; id--) {
                if (!_configsById.containsKey(id)) {
                    _freeIds.add(id);
                }
            }
            in.close();
            return true;

        } catch (Exception e) { // IOException, ClassCastException
            log.warning("Error reading configurations [file=" + file + "].", e);
            return false;
        }
    }

    /**
     * Returns the configuration file.
     */
    protected File getConfigFile (boolean xml)
    {
        String name = _cfgmgr.getConfigPath() + _name + (xml ? ".xml" : ".dat");
        return _cfgmgr.getResourceManager().getResourceFile(name);
    }

    /**
     * Assigns and returns a new id.
     */
    protected int assignId ()
    {
        int size = _freeIds.size();
        if (size > 0) {
            return _freeIds.remove(size - 1);
        }
        return ++_highestId;
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

    /** The configuration manager that created this group. */
    protected ConfigManager _cfgmgr;

    /** The name of this group. */
    protected String _name;

    /** The configuration class. */
    protected Class<T> _cclass;

    /** Configurations mapped by name. */
    protected HashMap<String, T> _configsByName = new HashMap<String, T>();

    /** Configurations mapped by integer identifier. */
    protected HashIntMap<T> _configsById;

    /** The highest id in use.  The next id is guaranteed to be available. */
    protected int _highestId;

    /** Free ids below the highest. */
    protected ArrayList<Integer> _freeIds;

    /** Configuration event listeners. */
    protected ObserverList<ConfigListener<T>> _listeners =
        new ObserverList<ConfigListener<T>>(ObserverList.FAST_UNSAFE_NOTIFY);
}
