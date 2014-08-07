//
// $Id$

package com.threerings.config.tools;

import java.io.IOException;
import java.io.File;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import com.samskivert.util.DependencyGraph;

import com.threerings.util.MessageManager;

import com.threerings.resource.ResourceManager;

import com.threerings.config.ArgumentMap;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Parameter;
import com.threerings.config.ParameterizedConfig;

import static com.threerings.ClydeLog.log;

/**
 */
public class ConfigFlattener
{
    /**
     * Tool entry point.
     */
    public static void main (String[] args)
        throws IOException
    {
        String rsrcDir;
        String outDir;
        switch (args.length) {
        default:
            System.err.println("Args: <rsrcdir> <outconfigdir>");
            System.exit(1);
            return;

        case 2:
            rsrcDir = args[0];
            outDir = args[1];
            break;
        }

        ResourceManager rsrcmgr = new ResourceManager(rsrcDir);
        File configDir = rsrcmgr.getResourceFile("config/");

        Preconditions.checkArgument(configDir.isDirectory(), "%s isn't a directory", configDir);
        Preconditions.checkArgument(
                new File(configDir, "manager.properties").exists() ||
                new File(configDir, "manager.txt").exists(), "cannot find manager descriptor");
        File destDir = rsrcmgr.getResourceFile(outDir);
        Preconditions.checkArgument(destDir.isDirectory(), "%s isn't a directory", destDir);

        MessageManager msgmgr = new MessageManager("rsrc.i18n");
        RePathableConfigManager cfgmgr = new RePathableConfigManager(rsrcmgr, msgmgr, "config/");
        log.info("Starting up...");
        cfgmgr.init();
        flatten(cfgmgr);

        // Save everything!
        log.info("Saving...");
        cfgmgr.resetConfigPath(outDir);
        cfgmgr.saveAll();
        log.info("Done!");
    }

    /**
     * Flatten all the configs in-place in the specified config manager.
     */
    public static void flatten (ConfigManager cfgmgr)
        throws IOException
    {
        DependentReferenceSet refSet = new DependentReferenceSet();
        refSet.populate(cfgmgr);

        // now go through each ref in dependency ordering
        log.info("Flattening...");
        int count = 0;
        while (!refSet.graph.isEmpty()) {
            count++;
            ConfigId id = refSet.graph.removeAvailableElement();
            @SuppressWarnings("unchecked")
            ConfigGroup<ManagedConfig> group =
                (ConfigGroup<ManagedConfig>)cfgmgr.getGroup(id.clazz);
            ManagedConfig cfg = group.getConfig(id.name);
            List<ConfigReference<?>> list = refSet.refs.get(id);
            if (!list.isEmpty()) {
                Set<String> paramNames;
                Map<ArgumentMap, String> newNames;
                if (cfg instanceof ParameterizedConfig) {
                    paramNames = Sets.newHashSet();
                    for (Parameter p : ((ParameterizedConfig)cfg).parameters) {
                        paramNames.add(p.name);
                    }
                    newNames = Maps.newHashMap();

                } else {
                    paramNames = ImmutableSet.of();
                    newNames = null; // won't get used- just you watch and see
                }

                // jog through our refs and twiddle them
                for (ConfigReference<?> ref : list) {
                    ArgumentMap args = ref.getArguments();
                    args.keySet().retainAll(paramNames);
                    if (args.isEmpty()) {
                        // we're done with this one!
                        continue;
                    }
                    // otherwise, map it to a new config with the args baked-in
                    String newName = newNames.get(args);
                    if (newName == null) {
                        ArgumentMap key = args.clone();
                        int code = generateHash(key);
                        newName = Integer.toHexString(code);
                        // avoid hash collisions
                        while (newNames.containsValue(newName)) {
                            newName = Integer.toHexString(++code);
                        }
                        // create the new config
                        ManagedConfig newCfg = cfg.getInstance(key);
                        newCfg.setName(id.name + "~" + newName);
                        group.addConfig(newCfg);
                        newNames.put(key, newName);
                    }

                    // now, copy a new config reference with the new name to the existing ref
                    new ConfigReference<ManagedConfig>(id.name + "~" + newName).copy(ref);
                }
            }
            if (cfg instanceof ParameterizedConfig) {
                ((ParameterizedConfig)cfg).parameters = Parameter.EMPTY_ARRAY;
            }
        }
        log.info("Flattened " + count);
    }

//    /**
//     * Gather up every config into the specified reference set.
//     */
//    protected static void gatherConfigs (ConfigManager cfgmgr, ConfigReferenceSet refSet)
//    {
//        try {
//            // this fucking method is protected... Consider making it public or moving
//            // the code that accesses it into the /config/ package.
//            Method method = ManagedConfig.class.getDeclaredMethod(
//                    "getUpdateReferences", ConfigReferenceSet.class);
//            method.setAccessible(true);
//            Object[] args = new Object[] { refSet };
//
//            // gather up all the referenced configs
//            for (ConfigGroup<?> group : cfgmgr.getGroups()) {
//                for (ManagedConfig cfg : group.getConfigs()) {
//                    try {
//                        method.invoke(cfg, args);
//                    } catch (RuntimeException e) {
//                        log.warning("Exception gathering configs", "group", group, "cfg", cfg, e);
//                    }
//                }
//            }
//        } catch (NoSuchMethodException nsme) {
//            log.warning("Did getUpdateReferences() change?", nsme);
//
//        } catch (IllegalAccessException iae) {
//            log.warning("Unable to call getUpdateReferences()?", iae);
//
//        } catch (InvocationTargetException ite) {
//            log.warning("Unable to call getUpdateReferences()?", ite);
//        }
//    }

    /**
     * Generate a stable hash code for the specified argument map.
     */
    protected static int generateHash (ArgumentMap args)
    {
        // TODO: we want to create a hashcode that is the same every time we run...
        // Perhaps I can just hand-calculate one, only using keys/values that have known hashcodes?
        // (String, numeric types...)
        // The problem also is that collisions need to be dealt with in a known order.
        // Perhaps I'll sort the config refs??
        return args.hashCode();
    }

    /**
     * A ResourceManager that can load from one spot and save to another.
     */
    protected static class RePathableConfigManager extends ConfigManager
    {
        public RePathableConfigManager (ResourceManager rsrcmgr, MessageManager msgmgr, String path)
        {
            super(rsrcmgr, msgmgr, path);
        }

        public void resetConfigPath (String configPath)
        {
            _configPath = configPath;
            if (!configPath.endsWith("/")) {
                _configPath += "/";
            }
        }
    }

    /**
     * The bare minimum needed to identify a config.
     */
    protected static class ConfigId
    {
        /** The type of the referenced config. */
        public final Class<? extends ManagedConfig> clazz;

        /** The name of the referenced config. */
        public final String name;

        /** Constructor. */
        public ConfigId (Class<? extends ManagedConfig> clazz, String name)
        {
            this.clazz = clazz;
            this.name = name;
        }

        @Override
        public int hashCode ()
        {
            return clazz.hashCode() * 31 + name.hashCode();
        }

        @Override
        public boolean equals (Object other)
        {
            if (!(other instanceof ConfigId)) {
                return false;
            }
            ConfigId that = (ConfigId)other;
            return (this.clazz == that.clazz) && this.name.equals(that.name);
        }

        @Override
        public String toString ()
        {
            return "ConfigId[" + clazz + ", " + name + "]";
        }
    }

    /**
     * A reference set that gathers dependencies between references and... more.
     * TODO doc
     */
    protected static class DependentReferenceSet extends ConfigReferenceSet
    {
        /** The dependency graph. */
        public final DependencyGraph<ConfigId> graph = new DependencyGraph<ConfigId>();

        /** The references that point to a particular config, indexed by config. */
        public final ListMultimap<ConfigId, ConfigReference<?>> refs = ArrayListMultimap.create();

        public void populate (ConfigManager cfgmgr)
        {
            log.info("Populating graph...");
            // first populate all configs into the graph
            int count = 0;
            for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                Class<? extends ManagedConfig> clazz = group.getConfigClass();
                _cfgClasses.add(clazz);
                for (ManagedConfig cfg : group.getConfigs()) {
                    graph.add(new ConfigId(clazz, cfg.getName()));
                    count++;
                }
            }
            log.info("Populated " + count);

            log.info("Gathering configs...");
            // then go through again, track refs, and make note of dependencies
            for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                Class<? extends ManagedConfig> clazz = group.getConfigClass();
                for (ManagedConfig cfg : group.getConfigs()) {
                    _current = new ConfigId(clazz, cfg.getName());
                    cfg.gatherRefs(this);
                }
            }
            log.info("Gathered configs!");
        }

        @Override
        public <T extends ManagedConfig> boolean add (Class<T> clazz, String name)
        {
            // Anything added with a name only *has no parameters*, and
            // therefore we don't care about it.
            return false;
        }

        @Override
        public <T extends ManagedConfig> boolean add (Class<T> clazz, ConfigReference<T> ref)
        {
            // omit config refs with no args: we don't care
            if (ref == null || ref.getArguments().isEmpty() || !_cfgClasses.contains(clazz)) {
                return false;
            }

            // track the ref...
            ConfigId id = new ConfigId(clazz, ref.getName());
            refs.put(id, ref);

            // and add the dependency
            graph.addDependency(_current, id);
            return true;
        }

        /** The config we're currently examining while adding dependencies. */
        protected ConfigId _current;

        /** All valid config classes. */
        protected Set<Class<?>> _cfgClasses = Sets.newHashSet();
    }
}
