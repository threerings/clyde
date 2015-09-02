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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Array;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.io.Closer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.samskivert.util.ObserverList;
import com.samskivert.util.StringUtil;

import com.threerings.editor.EditorTypes;

import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;
import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;
import com.threerings.export.XMLExporter;
import com.threerings.export.XMLImporter;
import com.threerings.export.util.LazyOutputStream;
import com.threerings.util.Copyable;

import static com.threerings.ClydeLog.log;

/**
 * Contains a group of managed configurations, all of the same class.
 */
public class ConfigGroup<T extends ManagedConfig>
    implements Copyable, Exportable
{
    /**
     * Returns the group name for the specified config class.
     */
    public static String getName (Class<?> clazz)
    {
        String cstr = clazz.getName();
        cstr = cstr.substring(Math.max(cstr.lastIndexOf('.'), cstr.lastIndexOf('$')) + 1);
        cstr = cstr.endsWith("Config") ? cstr.substring(0, cstr.length() - 6) : cstr;
        return StringUtil.toUSLowerCase(StringUtil.unStudlyName(cstr));
    }

    /**
     * Creates a new config group for the specified class.
     */
    public ConfigGroup (Class<T> clazz)
    {
        initConfigClass(clazz);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ConfigGroup ()
    {
    }

    /**
     * Initializes this group.
     */
    public void init (ConfigManager cfgmgr)
    {
        init(cfgmgr, new ConfigManager.Consumer<Exception>() {
                public void accept (Exception e) {} // do nothing
            });
    }

    /**
     * Initializes this group.
     */
    public void init (ConfigManager cfgmgr, ConfigManager.Consumer<Exception> exceptionConsumer)
    {
        _cfgmgr = cfgmgr;

        // load the existing configurations (first checking for an xml file, then a binary file)
        if (_cfgmgr.getConfigPath() != null &&
                (readConfigs(true, exceptionConsumer) || readConfigs(false, exceptionConsumer))) {
            log.debug("Read configurations for group " + _name + ".");
        }

        // provide the configurations with a reference to the manager
        for (ManagedConfig config : getRawConfigs()) {
            initConfig(config);
        }
    }

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
     * Get the classes of possible raw config types that we can use in this group.
     */
    public List<Class<?>> getRawConfigClasses ()
    {
        // TODO: pre-cache? it's nice to ignore this for non-editing contexts..
        EditorTypes anno = _cclass.getAnnotation(EditorTypes.class);
        return (anno == null)
            ? ImmutableList.<Class<?>>of(_cclass)
            : ImmutableList.copyOf(anno.value());
    }

    /**
     * Retrieves a configuration by name.
     */
    public T getConfig (String name)
    {
        return actualize(getRawConfig(name));
    }

    /**
     * Get the <em>raw</em> configuration by name.
     * This method is for editing and other "configging the configs" uses and may
     * return a DerivedConfig instance.
     */
    public ManagedConfig getRawConfig (String name)
    {
        return _configsByName.get(name);
    }

    /**
     * Returns the collection of all registered configurations.
     */
    public Iterable<T> getConfigs ()
    {
        return Iterables.transform(getRawConfigs(),
                new Function<ManagedConfig, T>() {
                    public T apply (ManagedConfig cfg) {
                        return actualize(cfg);
                    }
                });
    }

    /**
     * Return all the <em>raw</em> configurations.
     * This method is for editing and other "configging the configs" uses and may
     * return DerivedConfig instances.
     */
    public Iterable<ManagedConfig> getRawConfigs ()
    {
        return _configsByName.values();
    }

    /**
     * Adds a listener for configuration events.
     */
    public void addListener (ConfigGroupListener listener)
    {
        if (_listeners == null) {
            _listeners = ObserverList.newFastUnsafe();
        }
        _listeners.add(listener);
    }

    /**
     * Removes a configuration event listener.
     */
    public void removeListener (ConfigGroupListener listener)
    {
        if (_listeners != null) {
            _listeners.remove(listener);
            if (_listeners.isEmpty()) {
                _listeners = null;
            }
        }
    }

    /**
     * Adds all of the supplied configurations to the set.
     */
    public void addConfigs (Collection<? extends ManagedConfig> configs)
    {
        for (ManagedConfig config : configs) {
            addConfig(config);
        }
    }

    /**
     * Adds a configuration to the set.
     */
    public void addConfig (ManagedConfig config)
    {
        addConfig(config, true);
    }

    /**
     * Adds a configuration to the set, avoiding the normal event firing.
     *
     * Don't use this unless you know what you're doing.
     */
    public void addConfig (ManagedConfig config, boolean fireEvents)
    {
        if (!_cclass.isInstance(config) && !(config instanceof DerivedConfig)) {
            Class<?> clazz = (config == null) ? null : config.getClass();
            throw new IllegalArgumentException(clazz + " is not of type " + _cclass);
        }
        ManagedConfig oldCfg = _configsByName.put(config.getName(), config);
        initConfig(config);
        if (fireEvents) {
            if (oldCfg != null) {
                // tell any listeners that the old one has changed. They should in turn end up
                // listening on the new one...
                oldCfg.wasUpdated();
                fireConfigRemoved(oldCfg);
            }
            fireConfigAdded(config);
        }
    }

    /**
     * Removes a configuration from the set.
     */
    public void removeConfig (ManagedConfig config)
    {
        ManagedConfig oldCfg = _configsByName.remove(config.getName());
        if (oldCfg != null) {
            // notify listeners that the config has "changed" and then remove it
            oldCfg.wasUpdated();
            fireConfigRemoved(oldCfg);
        }
    }

    /**
     * Saves this group's configurations.
     */
    public final void save ()
    {
        save(getConfigFile(true));
    }

    /**
     * Saves this group's configurations to the specified file.
     */
    public final void save (File file)
    {
        save(file, true);
    }

    /**
     * Saves this group's configurations to the specified file.
     */
    public final void save (File file, boolean xml)
    {
        save(getRawConfigs(), file, xml);
    }

    /**
     * Saves this group's configurations to the specified file.
     */
    public final void save (Iterable<? extends ManagedConfig> rawConfigs, File file)
    {
        save(rawConfigs, file, true);
    }

    /**
     * Save the specified configs to the specified file.
     */
    public void save (Iterable<? extends ManagedConfig> rawConfigs, File file, boolean xml)
    {
        ManagedConfig[] array = toSaveableArray(rawConfigs);
        if (array == null) {
            return; // nothing to do
        }
        try {
            Closer closer = Closer.create();
            try {
                LazyOutputStream stream = closer.register(new LazyOutputStream(file));
                Exporter xport = closer.register(
                        xml ? new XMLExporter(stream) : new BinaryExporter(stream));
                xport.writeObject(array);

            } finally {
                closer.close();
            }

        } catch (IOException e) {
            log.warning("Error writing configurations [file=" + file + "].", e);
        }
    }

    /**
     * Return the raw configs as they should be saved, which may be an empty array,
     * or null if the entire group is stripped.
     */
    public ManagedConfig[] toSaveableArray ()
    {
        return toSaveableArray(getRawConfigs());
    }

    /**
     * Return the raw configs as they should be saved, which may be an empty array,
     * or null if the entire group is stripped.
     */
    protected ManagedConfig[] toSaveableArray (Iterable<? extends ManagedConfig> rawConfigs)
    {
        Class<? extends ManagedConfig> clazz =
                Iterables.any(rawConfigs, Predicates.instanceOf(DerivedConfig.class))
            ? ManagedConfig.class
            : _cclass;
        return _cfgmgr.toSaveableArray(_cclass, rawConfigs, clazz);
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
    public final void load (File file)
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
        ManagedConfig[] array;
        try {
            Importer in = new XMLImporter(new FileInputStream(file));
            array = (ManagedConfig[])in.readObject();
            in.close();

        } catch (IOException e) {
            log.warning("Error reading configurations [file=" + file + "].", e);
            return;
        }
        validateOuters(array);
        load(Arrays.asList(array), merge, false);
    }

    /**
     * Writes the fields of this object.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        ManagedConfig[] array;
        if (_cfgmgr != null) {
            array = _cfgmgr.toSaveableArray(_cclass, getRawConfigs(), ManagedConfig.class);

        } else {
            // This code path seems to be necessary for saving sub-groups in a UI for PX, when
            // doing something like dat2xml...
            // I'm not sure why, it's very possible that it just never needed the cfgmgr
            // before and that particular thing never got tested when I made _cfgmgr
            // responsible for saving the array?
            array = Iterables.toArray(getRawConfigs(), ManagedConfig.class);
        }

        // write the sorted configs out as a raw object
        out.write("configs", array, null, Object.class);
        out.write("class", String.valueOf(_cclass.getName()));
    }

    /**
     * Reads the fields of this object.
     */
    public void readFields (Importer in)
        throws IOException
    {
        // read in the configs and determine the type
        ManagedConfig[] configs = (ManagedConfig[])in.read("configs", null, Object.class);
        String classname = in.read("class", (String)null);
        Class<?> clazz;
        if (classname != null) {
            try {
                clazz = Class.forName(classname);
            } catch (Exception e) {
                throw (IOException)new IOException("Unknown class: " + classname).initCause(e);
            }
        } else {
            // oldstyle- determine class from array element type
            clazz = configs.getClass().getComponentType();
        }

        @SuppressWarnings("unchecked")
        Class<T> tclazz = (Class<T>)clazz;
        initConfigClass(tclazz);

        // populate the maps
        initConfigs(configs);
    }

    // documentation inherited from interface Copyable
    public Object copy (Object dest)
    {
        return copy(dest, null);
    }

    // documentation inherited from interface Copyable
    public Object copy (Object dest, Object outer)
    {
        @SuppressWarnings("unchecked") ConfigGroup<T> other =
            (dest instanceof ConfigGroup) ? (ConfigGroup<T>)dest : new ConfigGroup<T>(_cclass);
        other.load(getRawConfigs(), false, true);
        return other;
    }

    /**
     * Initializes the configuration class immediately after construction or deserialization.
     */
    protected void initConfigClass (Class<T> clazz)
    {
        _cclass = clazz;
        _name = getName(clazz);
    }

    /**
     * Attempts to read the initial set of configurations.
     *
     * @return true if successful, false otherwise.
     */
    protected boolean readConfigs (boolean xml, ConfigManager.Consumer<Exception> exceptionConsumer)
    {
        InputStream stream = getConfigStream(xml);
        if (stream == null) {
            return false;
        }
        ManagedConfig[] configs;
        try {
            Importer in = xml ? new XMLImporter(stream) : new BinaryImporter(stream);
            configs = (ManagedConfig[])in.readObject();
            in.close();

        } catch (Exception e) { // IOException, ClassCastException
            exceptionConsumer.accept(e);
            log.warning("Error reading configurations.", "group", _name, e);
            return false;
        }

        if (xml) {
            validateOuters(configs);
        }
        initConfigs(configs);
        return true;
    }

    /**
     * Returns the configuration stream, or <code>null</code> if it doesn't exist.
     */
    protected InputStream getConfigStream (boolean xml)
    {
        try {
            return _cfgmgr.getResourceManager().getResource(getConfigPath(xml));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the configuration file.
     */
    protected File getConfigFile (boolean xml)
    {
        return _cfgmgr.getResourceManager().getResourceFile(getConfigPath(xml));
    }

    /**
     * Returns the path of the config resource associated with this group.
     */
    protected String getConfigPath (boolean xml)
    {
        return _cfgmgr.getConfigPath() + _name + (xml ? ".xml" : ".dat");
    }

    /**
     * Validates the outer object references of the supplied configs.
     */
    protected void validateOuters (ManagedConfig[] configs)
    {
        for (ManagedConfig config : configs) {
            if (!(config instanceof DerivedConfig)) {
                config.validateOuters(_name + ":" + config.getName());
            }
        }
    }

    /**
     * Sets the initial set of configs.
     */
    protected void initConfigs (ManagedConfig[] configs)
    {
        for (ManagedConfig config : configs) {
            initConfig(config);
            _configsByName.put(config.getName(), config);
        }
    }

    /**
     * Initialize a config.
     * Lame.
     */
    protected void initConfig (ManagedConfig config)
    {
        config.init(_cfgmgr);
        if (config instanceof DerivedConfig) {
            ((DerivedConfig)config).cclass = _cclass;
        }
    }

    /**
     * Loads the specified configurations.
     *
     * @param merge if true, merge with the existing configurations; do not delete configurations
     * that do not exist in the collection.
     * @param clone if true, we must clone configurations that do not yet exist in the group.
     */
    protected void load (Iterable<ManagedConfig> nconfigs, boolean merge, boolean clone)
    {
        // add any configurations that don't already exist and update those that do
        HashSet<String> names = new HashSet<String>();
        for (ManagedConfig nconfig : nconfigs) {
            String name = nconfig.getName();
            names.add(name);

            ManagedConfig oconfig = getRawConfig(name);
            if (oconfig == null) {
                addConfig(clone ? (ManagedConfig)nconfig.clone() : nconfig);

            } else if (!nconfig.equals(oconfig)) {
                if (nconfig instanceof DerivedConfig) {
                    ((DerivedConfig)nconfig).cclass = _cclass;
                }
                ManagedConfig copied = (ManagedConfig)nconfig.copy(oconfig);
                if (copied == oconfig) {
                    oconfig.wasUpdated();
                } else {
                    removeConfig(oconfig);
                    addConfig(copied);
                }
            }
        }

        if (merge) {
            return;
        }

        // remove any configurations not present in the array (if not merging)
        for (ManagedConfig cfg : Lists.newArrayList(getRawConfigs())) {
            if (!names.contains(cfg.getName())) {
                removeConfig(cfg);
            }
        }
    }

    /**
     * Utility to cast/promote a config value to our class type.
     */
    protected T actualize (ManagedConfig val)
    {
        if (val instanceof DerivedConfig) {
            val = val.getInstance((ArgumentMap)null);
        }
        return _cclass.cast(val);
    }

    /**
     * Fires a configuration added event.
     */
    protected void fireConfigAdded (ManagedConfig config)
    {
        if (_listeners == null) {
            return;
        }
        final ConfigEvent<ManagedConfig> event = new ConfigEvent<ManagedConfig>(this, config);
        _listeners.apply(new ObserverList.ObserverOp<ConfigGroupListener>() {
            public boolean apply (ConfigGroupListener listener) {
                listener.configAdded(event);
                return true;
            }
        });
    }

    /**
     * Fires a configuration removed event.
     */
    protected void fireConfigRemoved (ManagedConfig config)
    {
        if (_listeners == null) {
            return;
        }
        final ConfigEvent<ManagedConfig> event = new ConfigEvent<ManagedConfig>(this, config);
        _listeners.apply(new ObserverList.ObserverOp<ConfigGroupListener>() {
            public boolean apply (ConfigGroupListener listener) {
                listener.configRemoved(event);
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
    protected HashMap<String, ManagedConfig> _configsByName = new HashMap<String, ManagedConfig>();

    /** Configuration event listeners. */
    protected ObserverList<ConfigGroupListener> _listeners;
}
