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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ListUtil;
import com.samskivert.util.ObserverList;
import com.samskivert.util.PropertiesUtil;
import com.samskivert.util.StringUtil;

import com.threerings.resource.ResourceManager;

import com.threerings.editor.util.Validator;
import com.threerings.export.BinaryImporter;
import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;
import com.threerings.expr.Scope;
import com.threerings.util.CacheUtil;
import com.threerings.util.Copyable;
import com.threerings.util.MessageManager;

import static com.threerings.ClydeLog.log;

/**
 * Manages the set of loaded configurations.
 */
public class ConfigManager
    implements Copyable, Exportable
{
    // TODO: Replace with java.util.function.Consumer when we got to Java 8
    public interface Consumer<T>
    {
        public void accept (T t);
    }

    /**
     * Creates a new global configuration manager.
     *
     * @param configPath the resource path of the configurations.
     */
    public ConfigManager (ResourceManager rsrcmgr, MessageManager msgmgr, String configPath)
    {
        _type = "global";
        _rsrcmgr = rsrcmgr;
        _msgmgr = msgmgr;
        _configPath = configPath + (configPath.endsWith("/") ? "" : "/");
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ConfigManager ()
    {
    }

    /**
     * Determines whether the config manager has been initialized.
     */
    public boolean isInitialized ()
    {
        return (_resources != null);
    }

    /**
     * Initialization method for the global configuration manager.
     */
    public void init ()
    {
        init(new Consumer<Exception>() {
                public void accept (Exception e) {} // do-nothing
            });
    }

    /**
     * Initialization method for the global configuration manager.
     */
    public void init (Consumer<Exception> exceptionConsumer)
    {
        // load the manager properties
        try {
            loadManagerProperties();
        } catch (IOException e) {
            exceptionConsumer.accept(e);
            log.warning("Failed to load manager properties.", e);
            return;
        }

        // create the resource cache
        _resources = CacheUtil.softValues();

        // register the global groups
        Class<?>[] classes = _classes.get("global");
        if (classes == null) {
            return;
        }
        for (Class<?> clazz : classes) {
            @SuppressWarnings("unchecked") Class<? extends ManagedConfig> cclass =
                    (Class<? extends ManagedConfig>)clazz;
            registerGroup(cclass, exceptionConsumer);
        }
    }

    /**
     * Initialization method for child configuration managers.
     */
    public void init (String type, ConfigManager parent)
    {
        _type = type;
        _parent = parent;
        _rsrcmgr = parent._rsrcmgr;
        _msgmgr = parent._msgmgr;
        _resources = parent._resources;
        _classes = parent._classes;

        // copy the groups over (any group not in the list will be silently discarded)
        HashMap<Class<?>, ConfigGroup<?>> ogroups = _groups;
        _groups = new HashMap<Class<?>, ConfigGroup<?>>();
        Class<?>[] classes = _classes.get(type);
        if (classes == null) {
            return;
        }
        for (Class<?> clazz : classes) {
            ConfigGroup<?> group = ogroups.get(clazz);
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
     * Returns the type of this manager.
     */
    public String getType ()
    {
        return _type;
    }

    /**
     * Returns a reference to the parent of this manager, or <code>null</code> if this is the root.
     */
    public ConfigManager getParent ()
    {
        return _parent;
    }

    /**
     * Returns a reference to the root of the manager hierarchy.
     */
    public ConfigManager getRoot ()
    {
        return (_parent == null) ? this : _parent.getRoot();
    }

    /**
     * Returns a reference to the resource manager used to load configurations.
     */
    public ResourceManager getResourceManager ()
    {
        return _rsrcmgr;
    }

    /**
     * Returns a reference to the message manager used to load configurations.
     */
    public MessageManager getMessageManager ()
    {
        return _msgmgr;
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
     * Determines whether configurations of the specified class are loaded from individual
     * resources.
     */
    public boolean isResourceClass (Class<?> clazz)
    {
        return ListUtil.contains(getResourceClasses(), clazz);
    }

    /**
     * Returns the array of classes representing configurations loaded from individual resources.
     */
    public Class<?>[] getResourceClasses ()
    {
        return _classes.get("resource");
    }

    /**
     * Retrieves a configuration by class and name.  If the configuration is not found in this
     * manager, the request will be forwarded to the parent, and so on.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public final <T extends ManagedConfig> T getConfig (Class<T> clazz, String name)
    {
        return getConfig(clazz, name, null, null);
    }

    /**
     * Retrieves a configuration by class and reference.  If the configuration is not found in this
     * manager, the request will be forwarded to the parent, and so on.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public final <T extends ManagedConfig> T getConfig (Class<T> clazz, ConfigReference<T> ref)
    {
        return getConfig(clazz, ref, null);
    }

    /**
     * Retrieves a configuration by class and reference.  If the configuration is not found in this
     * manager, the request will be forwarded to the parent, and so on.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public final <T extends ManagedConfig> T getConfig (
        Class<T> clazz, ConfigReference<T> ref, Scope scope)
    {
        return (ref == null) ? null : getConfig(clazz, ref.getName(), scope, ref.getArguments());
    }

    /**
     * Retrieves a configuration by class, name, and arguments.  If the configuration is not found
     * in this manager, the request will be forwarded to the parent, and so on.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public final <T extends ManagedConfig> T getConfig (
        Class<T> clazz, String name, String firstKey, Object firstValue, Object... otherArgs)
    {
        return getConfig(clazz, name, null, firstKey, firstValue, otherArgs);
    }

    /**
     * Retrieves a configuration by class, name, and arguments.  If the configuration is not found
     * in this manager, the request will be forwarded to the parent, and so on.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public final <T extends ManagedConfig> T getConfig (
        Class<T> clazz, String name, Scope scope,
        String firstKey, Object firstValue, Object... otherArgs)
    {
        return getConfig(clazz, name, scope, new ArgumentMap(firstKey, firstValue, otherArgs));
    }

    /**
     * Retrieves a configuration by class, name, and scope.  If the configuration is not found in
     * this manager, the request will be forwarded to the parent, and so on.
     *
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public final <T extends ManagedConfig> T getConfig (Class<T> clazz, String name, Scope scope)
    {
        return getConfig(clazz, name, scope, null);
    }

    /**
     * Retrieves a configuration by class, name, and arguments.  If the configuration is not found
     * in this manager, the request will be forwarded to the parent, and so on.
     *
     * @param args the configuration arguments, or <code>null</code> for none.
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public final <T extends ManagedConfig> T getConfig (
        Class<T> clazz, String name, ArgumentMap args)
    {
        return getConfig(clazz, name, null, args);
    }

    /**
     * Retrieves a configuration by class, name, scope, and arguments.  If the configuration is not
     * found in this manager, the request will be forwarded to the parent, and so on.
     *
     * @param scope the scope in which to create the config, or <code>null</code> for none.
     * @param args the configuration arguments, or <code>null</code> for none.
     * @return the requested configuration, or <code>null</code> if not found.
     */
    public <T extends ManagedConfig> T getConfig (
        Class<T> clazz, String name, Scope scope, ArgumentMap args)
    {
        // check for a null name
        if (name == null) {
            return null;
        }

        ManagedConfig cfg;
        // for resource-loaded configs, go through the cache
        if (isResourceClass(clazz)) {
            cfg = getResourceConfig(name);
            if (cfg != null && !clazz.isInstance(cfg)) {
                throw new ClassCastException("[config=" + name + ", expected=" + clazz +
                    ", actual=" + cfg.getClass() + "]");
            }

        } else {
            // otherwise, look for a group of the desired type
            ConfigGroup<T> group = getGroup(clazz);
            cfg = (group == null) ? null : group.getRawConfig(name); // TODO ??
            if (cfg == null) {
                return (_parent == null) ? null : _parent.getConfig(clazz, name, scope, args);
            }
        }

        if (cfg != null) {
            cfg = cfg.getInstance(scope, args);
        }

        return clazz.cast(cfg);
    }

    /**
     * Get the <em>raw</em> config witht the specified class/group and name.
     * This method is for editing and other "configging the configs" usees and may return
     * a DerivedConfig instance and not a config that implements the specified class!
     */
    public ManagedConfig getRawConfig (Class<? extends ManagedConfig> clazz, String name)
    {
        if (name == null || isResourceClass(clazz)) {
            return getConfig(clazz, name);
        }
        ConfigGroup<? extends ManagedConfig> group = getGroup(clazz);
        if (group != null) {
            ManagedConfig config = group.getRawConfig(name);
            if (config != null) {
                return config;
            }
        }
        return (_parent == null) ? null : _parent.getRawConfig(clazz, name);
    }

    /**
     * Attempts to fetch a resource config through the cache.
     */
    public ManagedConfig getResourceConfig (String name)
    {
        ManagedConfig config = _resources.get(name);
        if (config == null) {
            try {
                BinaryImporter in = new BinaryImporter(_rsrcmgr.getResource(name));
                _resources.put(name, config = (ManagedConfig)in.readObject());
                config.setName(name);
                config.init(getRoot());
                in.close();

            } catch (FileNotFoundException fnfe) {
                return null;

            } catch (Exception e) { // IOException, ClassCastException
                log.warning("Failed to load config from resource.", "name", name, e);
                return null;
            }
        }
        return config;
    }

    /**
     * Retrieves the groups registered for the specified class in this manager and all of its
     * ancestors.
     */
    public <T extends ManagedConfig> ConfigGroup<T>[] getGroups (Class<T> clazz)
    {
        ArrayList<ConfigGroup<T>> groups = new ArrayList<ConfigGroup<T>>();
        for (ConfigManager cfgmgr = this; cfgmgr != null; cfgmgr = cfgmgr.getParent()) {
            ConfigGroup<T> group = cfgmgr.getGroup(clazz);
            if (group != null) {
                groups.add(group);
            }
        }
        @SuppressWarnings("unchecked")
        ConfigGroup<T>[] array = (ConfigGroup<T>[])new ConfigGroup<?>[groups.size()];
        return groups.toArray(array);
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
     * Get a group from a managed config instance, if possible.
     */
    public ConfigGroup<ManagedConfig> getGroup (ManagedConfig instance)
    {
        Class<?> clazz = (instance instanceof DerivedConfig)
            ? ((DerivedConfig)instance).cclass
            : instance.getClass();
        for (Class<?> c = clazz; c != ManagedConfig.class; c = c.getSuperclass()) {
            ConfigGroup<?> group = _groups.get(c);
            if (group != null) {
                @SuppressWarnings("unchecked")
                ConfigGroup<ManagedConfig> ret = (ConfigGroup<ManagedConfig>)group;
                return ret;
            }
        }
        return null;
    }

    /**
     * Retrieves the groups with the specified name in this manager and all of its ancestors.
     */
    public ConfigGroup<?>[] getGroups (String name)
    {
        ArrayList<ConfigGroup<?>> groups = new ArrayList<ConfigGroup<?>>();
        for (ConfigManager cfgmgr = this; cfgmgr != null; cfgmgr = cfgmgr.getParent()) {
            ConfigGroup<?> group = cfgmgr.getGroup(name, false);
            if (group != null) {
                groups.add(group);
            }
        }
        return groups.toArray(new ConfigGroup<?>[groups.size()]);
    }

    /**
     * Returns the configuration group with the specified name.  If the group is not found in
     * this manager, the request will be forwarded to the parent, and so on.
     */
    public ConfigGroup<?> getGroup (String name)
    {
        return getGroup(name, true);
    }

    /**
     * Returns the configuration group with the specified name.
     *
     * @param forward if true and there's no such group, forward the request to the parent.
     */
    public ConfigGroup<?> getGroup (String name, boolean forward)
    {
        for (ConfigGroup<?> group : _groups.values()) {
            if (group.getName().equals(name)) {
                return group;
            }
        }
        return (forward && _parent != null) ? _parent.getGroup(name) : null;
    }

    /**
     * Returns the collection of all registered groups.
     */
    public Collection<ConfigGroup<?>> getGroups ()
    {
        return new Ordering<ConfigGroup<?>>() {
                    public int compare (ConfigGroup<?> g1, ConfigGroup<?> g2) {
                        return g1.getName().compareTo(g2.getName());
                    }
                }.immutableSortedCopy(_groups.values());
    }

    /**
     * Adds a listener that will be notified on all config updates.
     */
    public void addUpdateListener (ConfigUpdateListener<?> listener)
    {
        if (_updateListeners == null) {
            _updateListeners = ObserverList.newFastUnsafe();
        }
        @SuppressWarnings("unchecked") ConfigUpdateListener<ManagedConfig> mlistener =
            (ConfigUpdateListener<ManagedConfig>)listener;
        _updateListeners.add(mlistener);
    }

    /**
     * Removes an update listener.
     */
    public void removeUpdateListener (ConfigUpdateListener<?> listener)
    {
        if (_updateListeners != null) {
            @SuppressWarnings("unchecked") ConfigUpdateListener<ManagedConfig> mlistener =
                (ConfigUpdateListener<ManagedConfig>)listener;
            _updateListeners.remove(mlistener);
            if (_updateListeners.isEmpty()) {
                _updateListeners = null;
            }
        }
    }

    /**
     * Saves the configurations in all groups.
     */
    public void saveAll ()
    {
        for (ConfigGroup<?> group : _groups.values()) {
            group.save();
        }
    }

    /**
     * Saves the configurations in all groups.
     *
     * @param dir the directory in which to drop all the files.
     * @param extension the filename extension (including any leading '.')
     * @param xml true to save in XML format, false for binary.
     */
    public void saveAll (File dir, String extension, boolean xml)
    {
        for (ConfigGroup<?> group : _groups.values()) {
            group.save(new File(dir, group.getName() + extension), xml);
        }
    }

    /**
     * Reverts the configurations in all groups to their last saved state.
     */
    public void revertAll ()
    {
        for (ConfigGroup<?> group : _groups.values()) {
            group.revert();
        }
    }

    /**
     * Updates a resource-loaded configuration through the cache.  If the configuration is not in
     * the cache, the provided configuration will be stored under the specified name and returned.
     * Otherwise, the cached version will be updated to reflect the provided configuration and
     * returned.
     */
    public ManagedConfig updateResourceConfig (String name, ManagedConfig config)
    {
        ManagedConfig oconfig = _resources.get(name);
        if (oconfig == null) {
            _resources.put(name, config);
            return config;
        } else {
            config.copy(oconfig);
            oconfig.wasUpdated();
            return oconfig;
        }
    }

    /**
     * Validates the references of all configs managed by this manager.
     *
     * @return true if the references are valid
     */
    public boolean validateReferences (Validator validator)
    {
        boolean result = true;
        for (ConfigGroup<?> group : getGroups()) {
            validator.pushWhere(group.getName());
            try {
                for (ManagedConfig config : group.getRawConfigs()) {
                    validator.pushWhere(config.getName());
                    try {
                        result &= config.validateReferences(validator);
                    } finally {
                        validator.popWhere();
                    }
                }
            } finally {
                validator.popWhere();
            }
        }
        return result;
    }

    /**
     * Refreshes all configurations of the specified class.
     */
    public void refresh (Class<? extends ManagedConfig> clazz)
    {
        // look for groups first
        @SuppressWarnings("unchecked") ConfigGroup<ManagedConfig>[] groups =
            (ConfigGroup<ManagedConfig>[])getGroups(clazz);
        if (groups.length > 0) {
            for (ConfigGroup<ManagedConfig> group : groups) {
                for (ManagedConfig config : group.getRawConfigs()) {
                    refresh(config);
                }
            }
            return;
        }

        // otherwise, refresh the resource configs
        for (ManagedConfig oconfig : Lists.newArrayList(_resources.values())) {
            if (!clazz.isInstance(oconfig)) {
                continue;
            }
            String name = oconfig.getName();
            ManagedConfig nconfig;
            try {
                BinaryImporter in = new BinaryImporter(_rsrcmgr.getResource(name));
                nconfig = (ManagedConfig)in.readObject();
                nconfig.setName(name);
                nconfig.init(getRoot());
                in.close();

            } catch (Exception e) { // IOException, ClassCastException
                log.warning("Failed to refresh config from resource.", "name", name, e);
                continue;
            }
            nconfig.copy(oconfig);
            refresh(oconfig);
        }
    }

    /**
     * Writes the fields of this object.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        // write out the non-empty groups as a sorted array
        List<ConfigGroup<?>> list = Lists.newArrayList();
        for (ConfigGroup<?> group : getGroups()) { // getGroups() sorts
            if (!Iterables.isEmpty(group.getRawConfigs())) {
                list.add(group);
            }
        }
        ConfigGroup<?>[] groups = Iterables.toArray(list, ConfigGroup.class);
        out.write("groups", groups, new ConfigGroup<?>[0], ConfigGroup[].class);
    }

    /**
     * Reads the fields of this object.
     */
    public void readFields (Importer in)
        throws IOException
    {
        // read in the groups and populate the map
        ConfigGroup<?>[] groups = in.read("groups", new ConfigGroup<?>[0], ConfigGroup[].class);
        for (ConfigGroup<?> group : groups) {
            _groups.put(group.getConfigClass(), group);
        }
    }

    // documentation inherited from interface Copyable
    public Object copy (Object dest)
    {
        return copy(dest, null);
    }

    // documentation inherited from interface Copyable
    public Object copy (Object dest, Object outer)
    {
        ConfigManager other = (dest instanceof ConfigManager) ?
            (ConfigManager)dest : new ConfigManager();
        for (ConfigGroup<?> group : _groups.values()) {
            Class<?> clazz = group.getConfigClass();
            ConfigGroup<?> ogroup = other._groups.get(clazz);
            other._groups.put(clazz, (ConfigGroup<?>)group.copy(ogroup));
        }
        return other;
    }

    /**
     * Loads the manager properties.
     */
    protected void loadManagerProperties ()
        throws IOException
    {
        Properties props = new Properties();

        InputStream in = null;
        try {
            in = _rsrcmgr.getResource(_configPath + "manager.properties");
        } catch (IOException ioe) {
            // let's try the .txt version then
            in = _rsrcmgr.getResource(_configPath + "manager.txt");
        }

        props.load(in);

        // initialize the types
        _classes = new HashMap<String, Class<?>[]>();
        String[] types = StringUtil.parseStringArray(props.getProperty("types", ""));
        types = ArrayUtil.append(types, "resource");
        for (String type : types) {
            Properties tprops = PropertiesUtil.getSubProperties(props, type);
            String[] names = StringUtil.parseStringArray(tprops.getProperty("classes", ""));
            Class<?>[] classes = new Class<?>[names.length];
            for (int ii = 0; ii < names.length; ii++) {
                try {
                    classes[ii] = Class.forName(names[ii]);
                } catch (ClassNotFoundException e) {
                    throw (IOException)new IOException("Error initializing manager.").initCause(e);
                }
            }
            _classes.put(type, classes);
        }
    }

    /**
     * Registers a new config group.
     */
    protected <T extends ManagedConfig> void registerGroup (
            Class<T> clazz, Consumer<Exception> exceptionConsumer)
    {
        ConfigGroup<T> group = new ConfigGroup<T>(clazz);
        group.init(this, exceptionConsumer);
        _groups.put(clazz, group);
    }

    /**
     * Refreshes the specified configuration by simulating an update without firing a global
     * update event.
     */
    protected void refresh (ManagedConfig config)
    {
        _ignoreUpdates = true;
        try {
            config.wasUpdated();
        } finally {
            _ignoreUpdates = false;
        }
    }

    /**
     * Fires a configuration updated event.
     */
    protected void fireConfigUpdated (ManagedConfig config)
    {
        if (_updateListeners != null && !_ignoreUpdates) {
            final ConfigEvent<ManagedConfig> event = new ConfigEvent<ManagedConfig>(this, config);
            _updateListeners.apply(
                new ObserverList.ObserverOp<ConfigUpdateListener<ManagedConfig>>() {
                public boolean apply (ConfigUpdateListener<ManagedConfig> listener) {
                    listener.configUpdated(event);
                    return true;
                }
            });
        }
    }

    /**
     * Converts the supplied collection of configs to a sorted array for saving, or returns
     * null to indicate that this group should not actually be saved in this instance.
     *
     * @param groupClass the primary class for the group.
     * @param configs the configs to save
     * @param arrayElementClass the class to use for making the array.
     */
    protected ManagedConfig[] toSaveableArray (
            Class<? extends ManagedConfig> groupClass,
            Iterable<? extends ManagedConfig> configs,
            Class<? extends ManagedConfig> arrayElementClass)
    {
        @SuppressWarnings("unchecked")
        Class<ManagedConfig> clazz = (Class<ManagedConfig>)arrayElementClass;

        return Iterables.toArray(
                new Ordering<ManagedConfig>() {
                    public int compare (ManagedConfig c1, ManagedConfig c2) {
                        return c1.getName().compareTo(c2.getName());
                    }
                }.immutableSortedCopy(configs), clazz);
    }

    /** The type of this manager. */
    protected String _type;

    /** The parent of this manager, if any. */
    protected ConfigManager _parent;

    /** The resource manager used to load configurations. */
    protected ResourceManager _rsrcmgr;

    /** The message manager used to load configurations. */
    protected MessageManager _msgmgr;

    /** The resource path of the managed configurations. */
    protected String _configPath;

    /** Registered configuration groups mapped by config class. */
    protected HashMap<Class<?>, ConfigGroup<?>> _groups = new HashMap<Class<?>, ConfigGroup<?>>();

    /** Resource-loaded configs mapped by path. */
    protected Map<String, ManagedConfig> _resources;

    /** Maps manager types to their classes (as read from the manager properties). */
    protected HashMap<String, Class<?>[]> _classes;

    /** Config update listeners. */
    protected ObserverList<ConfigUpdateListener<ManagedConfig>> _updateListeners;

    /** Set when we should ignore config updates because we're refreshing. */
    protected boolean _ignoreUpdates;
}
