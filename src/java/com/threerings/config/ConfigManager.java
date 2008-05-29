//
// $Id$

package com.threerings.config;

import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

import com.samskivert.util.PropertiesUtil;
import com.samskivert.util.StringUtil;

import com.threerings.resource.ResourceManager;

import static com.threerings.ClydeLog.*;

/**
 * Manages the set of loaded configurations.
 */
public class ConfigManager
{
    /**
     * Creates a new configuration manager.
     *
     * @param configPath the resource path of the configurations.
     */
    public ConfigManager (ResourceManager rsrcmgr, String configPath)
    {
        _rsrcmgr = rsrcmgr;
        _configPath = configPath + (configPath.endsWith("/") ? "" : "/");
    }

    /**
     * Initializes the configuration manager, loading its configuration groups and initial configs.
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
     * Retrieves a configuration by class and name.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public <T extends ManagedConfig> T getConfig (Class<T> clazz, String name)
    {
        ConfigGroup<T> group = getGroup(clazz);
        return (group == null) ? null : group.getConfig(name);
    }

    /**
     * Retrieves a configuration by class and integer identifier.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public <T extends ManagedConfig> T getConfig (Class<T> clazz, int id)
    {
        ConfigGroup<T> group = getGroup(clazz);
        return (group == null) ? null : group.getConfig(id);
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
     * Returns a reference to the resource manager used to load configurations.
     */
    protected ResourceManager getResourceManager ()
    {
        return _rsrcmgr;
    }

    /**
     * Returns the resource path from which configurations are loaded.
     */
    protected String getConfigPath ()
    {
        return _configPath;
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
        String[] groups = StringUtil.parseStringArray(props.getProperty("groups", ""));
        for (String group : groups) {
            Properties gprops = PropertiesUtil.getSubProperties(props, group);
            try {
                @SuppressWarnings("unchecked") Class<? extends ManagedConfig> clazz =
                    (Class<? extends ManagedConfig>)Class.forName(gprops.getProperty("class"));
                boolean ids = Boolean.parseBoolean(gprops.getProperty("ids"));
                registerGroup(group, clazz, ids);

            } catch (ClassNotFoundException e) {
                throw (IOException)new IOException("Error initializing group.").initCause(e);
            }
        }
    }

    /**
     * Registers a new config group.
     */
    protected <T extends ManagedConfig> void registerGroup (
        String name, Class<T> clazz, boolean ids)
    {
        _groups.put(clazz, new ConfigGroup<T>(this, name, clazz, ids));
    }

    /** The resource manager used to load configurations. */
    protected ResourceManager _rsrcmgr;

    /** The resource path of the managed configurations. */
    protected String _configPath;

    /** Registered configuration groups mapped by config class. */
    protected HashMap<Class, ConfigGroup> _groups = new HashMap<Class, ConfigGroup>();
}
