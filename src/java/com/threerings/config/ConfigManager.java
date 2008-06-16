//
// $Id$

package com.threerings.config;

import java.io.IOException;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.samskivert.util.PropertiesUtil;
import com.samskivert.util.QuickSort;
import com.samskivert.util.StringUtil;

import com.threerings.resource.ResourceManager;

import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;

import static com.threerings.ClydeLog.*;

/**
 * Manages the set of loaded configurations.
 */
public class ConfigManager
    implements Exportable
{
    /**
     * Creates a new global configuration manager.
     *
     * @param configPath the resource path of the configurations.
     */
    public ConfigManager (ResourceManager rsrcmgr, String configPath)
    {
        _name = "global";
        _rsrcmgr = rsrcmgr;
        _configPath = configPath + (configPath.endsWith("/") ? "" : "/");
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ConfigManager ()
    {
    }

    /**
     * Initialization method for the global configuration manager.
     */
    public void init ()
    {
        // load the manager configuration
        try {
            loadManagerConfig();
        } catch (IOException e) {
            log.warning("Failed to load manager config.", e);
        }
    }

    /**
     * Initialization method for child configuration managers.
     */
    public void init (String name, ConfigManager parent, Class... classes)
    {
        _name = name;
        _parent = parent;
        _rsrcmgr = parent.getResourceManager();

        // copy the groups over (any group not in the list will be silently discarded)
        HashMap<Class, ConfigGroup> ogroups = _groups;
        _groups = new HashMap<Class, ConfigGroup>();
        for (Class clazz : classes) {
            ConfigGroup group = ogroups.get(clazz);
            if (group == null) {
                @SuppressWarnings("unchecked") Class<ManagedConfig> cclass =
                    (Class<ManagedConfig>)clazz;
                group = new ConfigGroup<ManagedConfig>(cclass);
            }
            group.init(this);
            _groups.put(clazz, group);
        }
    }

    /**
     * Returns the name of this manager.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Returns a reference to the parent of this manager, or <code>null<code> if this is the root.
     */
    public ConfigManager getParent ()
    {
        return _parent;
    }

    /**
     * Returns the resource path from which configurations are loaded, or <code>null</code> if
     * configurations aren't loaded directly.
     */
    public String getConfigPath ()
    {
        return _configPath;
    }


    /**
     * Retrieves a configuration by class and name.  If the configuration is not found in this
     * manager, the request will be forwarded to the parent, and so on.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public <T extends ManagedConfig> T getConfig (Class<T> clazz, String name)
    {
        return getConfig(clazz, name, null);
    }

    /**
     * Retrieves a configuration by class and reference.  If the configuration is not found in this
     * manager, the request will be forwarded to the parent, and so on.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public <T extends ManagedConfig> T getConfig (Class<T> clazz, ConfigReference<T> ref)
    {
        return getConfig(clazz, ref.getName(), ref.getArguments());
    }
    
    /**
     * Retrieves a configuration by class, name, and arguments.  If the configuration is not found
     * in this manager, the request will be forwarded to the parent, and so on.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public <T extends ManagedConfig> T getConfig (
        Class<T> clazz, String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        return getConfig(clazz, name, createArgumentMap(firstKey, firstValue, otherArgs));
    }
    
    /**
     * Retrieves a configuration by class, name, and arguments.  If the configuration is not found
     * in this manager, the request will be forwarded to the parent, and so on.
     *
     * @param args the configuration arguments, or <code>null</code> for none.
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public <T extends ManagedConfig> T getConfig (Class<T> clazz, String name, ArgumentMap args)
    {
        ConfigGroup<T> group = getGroup(clazz);
        if (group != null) {
            T config = group.getConfig(name);
            if (config != null) {
                return clazz.cast(config.getInstance(args));
            }
        }
        return (_parent == null) ? null : _parent.getConfig(clazz, name, args);
    }
    
    /**
     * Retrieves a configuration by class and integer identifier.  If the configuration is not
     * found in this manager, the request will be forwarded to the parent, and so on.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public <T extends ManagedConfig> T getConfig (Class<T> clazz, int id)
    {
        return getConfig(clazz, id, null);
    }
    
    /**
     * Retrieves a configuration by class, integer identifier, and arguments.  If the
     * configuration is not found in this manager, the request will be forwarded to the parent,
     * and so on.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public <T extends ManagedConfig> T getConfig (
        Class<T> clazz, int id, String firstKey, Object firstValue, Object... otherArgs)
    {
        return getConfig(clazz, id, createArgumentMap(firstKey, firstValue, otherArgs));
    }
    
    /**
     * Retrieves a configuration by class, integer identifier, and arguments.  If the
     * configuration is not found in this manager, the request will be forwarded to the parent,
     * and so on.
     *
     * @param args the configuration arguments, or <code>null</code> for none.
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public <T extends ManagedConfig> T getConfig (Class<T> clazz, int id, ArgumentMap args)
    {
        ConfigGroup<T> group = getGroup(clazz);
        if (group != null) {
            T config = group.getConfig(id);
            if (config != null) {
                return clazz.cast(config.getInstance(args));
            }
        }
        return (_parent == null) ? null : _parent.getConfig(clazz, id, args);
    }

    /**
     * Retrieves the group registered for the specified class.
     */
    public <T extends ManagedConfig> ConfigGroup<T> getGroup (Class<T> clazz)
    {
        @SuppressWarnings("unchecked") ConfigGroup<T> group = (ConfigGroup<T>)_groups.get(clazz);
        return group;
    }

    /**
     * Returns the collection of all registered groups.
     */
    public Collection<ConfigGroup> getGroups ()
    {
        return _groups.values();
    }

    /**
     * Saves the configurations in all groups.
     */
    public void saveAll ()
    {
        for (ConfigGroup group : _groups.values()) {
            group.save();
        }
    }

    /**
     * Reverts the configurations in all groups to their last saved state.
     */
    public void revertAll ()
    {
        for (ConfigGroup group : _groups.values()) {
            group.revert();
        }
    }

    /**
     * Writes the fields of this object.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        // write out the groups as a sorted array
        ConfigGroup[] groups = _groups.values().toArray(new ConfigGroup[_groups.size()]);
        QuickSort.sort(groups, new Comparator<ConfigGroup>() {
            public int compare (ConfigGroup g1, ConfigGroup g2) {
                return g1.getName().compareTo(g2.getName());
            }
        });
        out.write("groups", groups, null, ConfigGroup[].class);
    }

    /**
     * Reads the fields of this object.
     */
    public void readFields (Importer in)
        throws IOException
    {
        // read in the groups and populate the map
        ConfigGroup[] groups = in.read("groups", null, ConfigGroup[].class);
        for (ConfigGroup group : groups) {
            _groups.put(group.getConfigClass(), group);
        }
    }

    /**
     * Returns a reference to the resource manager used to load configurations.
     */
    protected ResourceManager getResourceManager ()
    {
        return _rsrcmgr;
    }

    /**
     * Loads the manager config from the specified path.
     */
    protected void loadManagerConfig ()
        throws IOException
    {
        Properties props = new Properties();
        props.load(_rsrcmgr.getResource(_configPath + "manager.properties"));

        // initialize the config groups
        String[] classes = StringUtil.parseStringArray(props.getProperty("classes", ""));
        for (String cname : classes) {
            try {
                @SuppressWarnings("unchecked") Class<? extends ManagedConfig> clazz =
                    (Class<? extends ManagedConfig>)Class.forName(cname);
                registerGroup(clazz);

            } catch (ClassNotFoundException e) {
                throw (IOException)new IOException("Error initializing group.").initCause(e);
            }
        }
    }

    /**
     * Registers a new config group.
     */
    protected <T extends ManagedConfig> void registerGroup (Class<T> clazz)
    {
        ConfigGroup<T> group = new ConfigGroup<T>(clazz);
        group.init(this);
        _groups.put(clazz, group);
    }

    /**
     * Creates a new argument map from the supplied parameters.
     */
    protected static ArgumentMap createArgumentMap (
        String firstKey, Object firstValue, Object... otherArgs)
    {
        ArgumentMap args = new ArgumentMap();
        args.put(firstKey, firstValue);
        for (int ii = 0; ii < otherArgs.length; ii += 2) {
            args.put((String)otherArgs[ii], otherArgs[ii + 1]);
        }
        return args;
    }
    
    /** The name of this manager. */
    protected String _name;

    /** The parent of this manager, if any. */
    protected ConfigManager _parent;

    /** The resource manager used to load configurations. */
    protected ResourceManager _rsrcmgr;

    /** The resource path of the managed configurations. */
    protected String _configPath;

    /** Registered configuration groups mapped by config class. */
    protected HashMap<Class, ConfigGroup> _groups = new HashMap<Class, ConfigGroup>();
}
