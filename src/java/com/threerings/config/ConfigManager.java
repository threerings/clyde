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

import static java.util.logging.Level.*;
import static com.threerings.ClydeLog.*;

/**
 * Manages the set of loaded configurations.
 */
public class ConfigManager
{
    /**
     * Creates a new configuration manager.
     *
     * @param configPath the resource path of the manager configuration.
     */
    public ConfigManager (ResourceManager rsrcmgr, String configPath)
    {
        _rsrcmgr = rsrcmgr;

        // load the manager configuration
        try {
            loadConfig(configPath);
        } catch (IOException e) {
            log.log(WARNING, "Failed to load manager config.", e);
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
     * Loads the manager config from the specified path.
     */
    protected void loadConfig (String path)
        throws IOException
    {
        Properties props = new Properties();
        props.load(_rsrcmgr.getResource(path));

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
        _groups.put(clazz, new ConfigGroup<T>(name, clazz, ids));
    }

    /** The resource manager used to load configurations. */
    protected ResourceManager _rsrcmgr;

    /** Registered configuration groups mapped by config class. */
    protected HashMap<Class, ConfigGroup> _groups = new HashMap<Class, ConfigGroup>();
}
