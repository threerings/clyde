//
// $Id$

package com.threerings.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.StringUtil;

import static java.util.logging.Level.*;
import static com.threerings.ClydeLog.*;

/**
 * Superclass for configurations derived from property files.  The property files are distributed
 * throughout the resource folder with names corresponding to their type (e.g.,
 * "pickup.properties").  The build process compiles lists of all files with a given type,
 * and assigns persistent integer ids for the types that require them.
 */
public abstract class PropertyConfig
{
    /** The name of this configuration. */
    public String name;

    /** The configuration's integer identifier, if it has one. */
    public int id;

    /** The raw properties of the config (used to fetch type-specific parameters). */
    public Properties props;

    /**
     * Retrieves a boolean property from the supplied collection, substituting the given default
     * value if the property doesn't exist.
     */
    public static boolean getProperty (Properties props, String key, boolean defvalue)
    {
        String value = props.getProperty(key);
        return (value == null) ? defvalue : Boolean.parseBoolean(value);
    }

    /**
     * Retrieves an integer property from the supplied collection, substituting the given default
     * value if the property doesn't exist or doesn't parse correctly.
     */
    public static int getProperty (Properties props, String key, int defvalue)
    {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warning("Invalid integer property value [key=" + key +
                    ", value=" + value + "].");
            }
        }
        return defvalue;
    }

    /**
     * Retrieves a float property from the supplied collection, substituting the given default
     * value if the property doesn't exist or doesn't parse correctly.
     */
    public static float getProperty (Properties props, String key, float defvalue)
    {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                log.warning("Invalid float property value [key=" + key +
                    ", value=" + value + "].");
            }
        }
        return defvalue;
    }

    /**
     * Retrieves a boolean array property from the supplied collection, substituting the given
     * default if the property doesn't exist or doesn't parse correctly.
     */
    public static boolean[] getProperty (Properties props, String key, boolean[] defvalue)
    {
        String value = props.getProperty(key);
        if (value != null) {
            boolean[] array = StringUtil.parseBooleanArray(value);
            if (array != null) {
                return array;
            }
        }
        return defvalue;
    }

    /**
     * Retrieves a string array property from the supplied collection, substituting the given
     * default if the property doesn't exist or doesn't parse correctly.
     */
    public static String[] getProperty (Properties props, String key, String[] defvalue)
    {
        String value = props.getProperty(key);
        if (value != null) {
            String[] array = StringUtil.parseStringArray(value);
            if (array != null) {
                return array;
            }
        }
        return defvalue;
    }

    /**
     * Fetches a string property from the configuration.
     */
    public String getProperty (String key)
    {
        return getProperty(key, (String)null);
    }

    /**
     * Fetches a string property from the configuration.
     */
    public String getProperty (String key, String defvalue)
    {
        return props.getProperty(key, defvalue);
    }

    /**
     * Fetches a boolean property from the configuration.
     */
    public boolean getProperty (String key, boolean defvalue)
    {
        return getProperty(props, key, defvalue);
    }

    /**
     * Fetches an integer property from the configuration.
     */
    public int getProperty (String key, int defvalue)
    {
        return getProperty(props, key, defvalue);
    }

    /**
     * Fetches a float property from the configuration.
     */
    public float getProperty (String key, float defvalue)
    {
        return getProperty(props, key, defvalue);
    }

    /**
     * Fetches a string array property from the configuration.
     */
    public String[] getProperty (String key, String[] defvalue)
    {
        return getProperty(props, key, defvalue);
    }

    /**
     * Fetches a boolean array property from the configuration.
     */
    public boolean[] getProperty (String key, boolean[] defvalue)
    {
        return getProperty(props, key, defvalue);
    }

    /**
     * Initializes this configuration with its name, (optional) id, and properties.
     */
    protected void init (String name, int id, Properties props)
    {
        this.name = name;
        this.id = id;
        this.props = props;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Called during initialization to give subclasses a chance to do their thing.
     */
    protected void didInit ()
    {
    }

    /**
     * Loads a list of configurations from the supplied path and returns them in a name map.
     */
    protected static <T extends PropertyConfig> HashMap<String, T> loadConfigs (
        Class<T> clazz, String paths)
    {
        HashMap<String, T> map = new HashMap<String, T>();
        for (String path : ResourceUtil.loadStrings(paths)) {
            loadConfig(clazz, path, map);
        }
        return map;
    }

    /**
     * Loads a list of configurations from the supplied path and their ids from the identified
     * id properties and returns them in an id map.
     */
    protected static <T extends PropertyConfig> HashIntMap<T> loadConfigs (
        Class<T> clazz, String paths, String ids)
    {
        HashIntMap<T> map = new HashIntMap<T>();
        Properties idprops = ResourceUtil.loadProperties(ids);
        for (String path : ResourceUtil.loadStrings(paths)) {
            loadConfig(clazz, path, getProperty(idprops, path, 0), map);
        }
        return map;
    }

    /**
     * Loads a single configuration and stores it in the supplied name map.
     */
    protected static <T extends PropertyConfig> void loadConfig (
        Class<T> clazz, String path, Map<String, T> map)
    {
        T config = loadConfig(clazz, path);
        map.put(config.name, config);
    }

    /**
     * Loads a single configuration and stores it in the supplied id map.
     */
    protected static <T extends PropertyConfig> void loadConfig (
        Class<T> clazz, String path, int id, HashIntMap<T> map)
    {
        T config = loadConfig(clazz, path, id);
        map.put(id, config);
    }

    /**
     * Loads and returns a single configuration.
     */
    protected static <T extends PropertyConfig> T loadConfig (Class<T> clazz, String path)
    {
        return loadConfig(clazz, path, 0);
    }

    /**
     * Loads and returns a single configuration.
     */
    protected static <T extends PropertyConfig> T loadConfig (Class<T> clazz, String path, int id)
    {
        Properties props = ResourceUtil.loadProperties(path);
        T config = null;
        try {
            config = clazz.newInstance();
            config.init(getName(path), id, props);
        } catch (Exception e) {
            log.log(WARNING, "Failed to load config [path=" + path + "].", e);
        }
        return config;
    }

    /**
     * Returns the name from within a path (everything between the first and last slashes).
     */
    protected static String getName (String path)
    {
        int idx0 = path.indexOf('/'), idx1 = path.lastIndexOf('/');
        return path.substring(0, idx1).substring(idx0 + 1);
    }
}
