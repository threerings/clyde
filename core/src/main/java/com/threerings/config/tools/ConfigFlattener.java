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
import com.google.common.collect.Lists;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;

import com.samskivert.util.DependencyGraph;

import com.threerings.util.MessageManager;

import com.threerings.resource.ResourceManager;

import com.threerings.config.ArgumentMap;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.DerivedConfig;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Parameter;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.ConfigToolUtil;
import com.threerings.config.util.ConfigId;

import static com.threerings.ClydeLog.log;

/**
 * Flattens configs for export to clients such that there are no parameters.
 *
 * This is not an ant task because you probably need to pass jvmarg arguments,
 * and Sur-Fucking-PRIZE! You can't do that with a proper ant task.?!?!?!
 *
 * You probably want something like this in your build.xml:
 * <pre>{@code
 *
 * <!-- flatten configs for the client -->
 * <target name="flatten" depends="-preptools">
 *   <copy file="${rsrc.dir}/config/manager.properties"
 *         tofile="${clientResource.dir}/config/manager.txt"/>
 *   <java fork="true" classpathref="classpath" failonerror="true"
 *         classname="com.threerings.config.tools.ConfigFlattener">
 *     <jvmarg value="-Djava.awt.headless=true"/>
 *     <!-- needed unless we want to operate on dist -->
 *     <jvmarg value="-Dresource_dir=${basedir}/${rsrc.dir}"/>
 *     <arg value="${rsrc.dir}/"/>
 *     <arg value="${clientResource.dir}/config/"/>
 *     <arg value=".xml"/>
 *   </java>
 * </target>
 *
 * }</pre>
 */
public class ConfigFlattener
{
    /**
     * Command-line tool entry point.
     */
    @SuppressWarnings("fallthrough")
    public static void main (String[] args)
        throws IOException
    {
        String rsrcDir;
        String outDir;
        boolean isXML = true;
        String ext = ".xml";

        switch (args.length) {
        default:
            errUsageAndExit();
            return;

        case 3:
            ext = args[2];
            isXML = ".xml".equalsIgnoreCase(ext);
            // fall-through

        case 2:
            rsrcDir = args[0];
            outDir = args[1];
            break;
        }

        flatten(rsrcDir, outDir, ext, isXML);
    }

    /**
     * Potential entry point for other tools.
     */
    public static void flatten (String rsrcDir, String outDir)
    {
        flatten(rsrcDir, outDir, ".xml", true);
    }

    /**
     * Potential entry point for other tools.
     */
    public static void flatten (String rsrcDir, String outDir, String extension, boolean isXML)
    {
        ResourceManager rsrcmgr = new ResourceManager(rsrcDir);
        File configDir = rsrcmgr.getResourceFile("config/");

        Preconditions.checkArgument(configDir.isDirectory(), "%s isn't a directory", configDir);
        Preconditions.checkArgument(
                new File(configDir, "manager.properties").exists() ||
                new File(configDir, "manager.txt").exists(), "cannot find manager descriptor");

        File destDir = new File(outDir);
        Preconditions.checkArgument(destDir.isDirectory(), "%s isn't a directory", destDir);

        MessageManager msgmgr = new MessageManager("rsrc.i18n");
        ConfigManager cfgmgr = new ConfigManager(rsrcmgr, msgmgr, "config/");
//        log.info("Starting up...");
        cfgmgr.init();
        flatten(cfgmgr);

        // Save everything!
//        log.info("Saving...");
        cfgmgr.saveAll(destDir, extension, isXML);
//        log.info("Done!");
    }

    /**
     * Dump standalone usage information to stderr and exit with an error code.
     */
    protected static void errUsageAndExit ()
    {
        System.err.println("Args: <rsrcDir> <outDir> [fileExtension (default='.xml')]");
        System.err.println("If the extension is provided and not '.xml', output will be binary.");
        System.exit(1);
    }

    /**
     * Flatten all the configs in-place in the specified config manager.
     */
    public static void flatten (ConfigManager cfgmgr)
    {
        DependentReferenceSet refSet = new DependentReferenceSet();

        // turn all derived configs into their "original" form
        for (ConfigGroup group : cfgmgr.getGroups()) {
            for (DerivedConfig der : Lists.newArrayList(
                    Iterables.filter(group.getRawConfigs(), DerivedConfig.class))) {
                // get the non-raw version and re-store it, overwriting
                group.addConfig(group.getConfig(der.getName()));
            }
        }

        refSet.populate(cfgmgr);

        // now go through each ref in dependency ordering
//        log.info("Flattening...");
        int count = 0;
        while (!refSet.graph.isEmpty()) {
            count++;
            ConfigId id = refSet.graph.removeAvailableElement();
            //log.info("Checking " + id.name);
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
                        refSet.addNewConfig(id.clazz, newCfg);
                    }

                    // now, copy a new config reference with the new name to the existing ref
                    new ConfigReference<ManagedConfig>(id.name + "~" + newName).copy(ref);
                }
            }
            if (cfg instanceof ParameterizedConfig) {
                ((ParameterizedConfig)cfg).parameters = Parameter.EMPTY_ARRAY;
            }
        }
//        log.info("Flattened " + count);
    }

    /**
     * Generate a stable hash code for the specified argument map.
     */
    protected static int generateHash (ArgumentMap args)
    {
        // Note: ArgumentMap stores args in a sorted order, but let's assume we don't know that.

        int hash = 0;
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            hash += entry.getKey().hashCode() ^ generateHash(entry.getValue());
        }
        return hash;
    }

    /**
     * Return a stable hash code for the specified object if we can, or simply return 0.
     */
    protected static int generateHash (Object obj)
    {
        if (obj != null) {
            if (obj instanceof String || Primitives.isWrapperType(obj.getClass())) {
                return obj.hashCode();

            } else if (obj instanceof ArgumentMap) {
                return generateHash((ArgumentMap)obj);
            }
        }
        return 0;
    }

    /**
     * A ConfigReferenceSet that gathers dependencies between config references
     * as well as collects each ConfigReference uniquely for later rewriting.
     */
    protected static class DependentReferenceSet extends ConfigReferenceSet
    {
        /** The dependency graph. */
        public final DependencyGraph<ConfigId> graph = new DependencyGraph<ConfigId>();

        /** The references that point to a particular config, indexed by config. */
        public final ListMultimap<ConfigId, ConfigReference<?>> refs = ArrayListMultimap.create();

        /**
         * Populate this reference set with all the configs in the specified cfgmgr.
         */
        public void populate (ConfigManager cfgmgr)
        {
//            log.info("Populating graph...");
            // first populate all configs into the graph
            int count = 0;
            for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                Class<? extends ManagedConfig> clazz = group.getConfigClass();
                _cfgClasses.add(clazz);
                for (ManagedConfig cfg : group.getConfigs()) {
                    graph.add(new ConfigId(clazz, cfg.getName()));
                    _allSeen.add(new ConfigId(clazz, cfg.getName()));
                    count++;
                }
            }
//            log.info("Populated " + count);

//            log.info("Gathering configs...");
            // then go through again, track refs, and make note of dependencies
            try {
                for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                    Class<? extends ManagedConfig> clazz = group.getConfigClass();
                    for (ManagedConfig cfg : group.getConfigs()) {
                        _current = new ConfigId(clazz, cfg.getName());
                        ConfigToolUtil.getUpdateReferences(cfg, this);
                    }
                }
            } finally {
                _current = null;
            }
//            log.info("Gathered configs!");
        }

        /**
         * Add a newly-created config to the reference set.
         */
        public void addNewConfig (Class<? extends ManagedConfig> clazz, ManagedConfig cfg)
        {
            _current = new ConfigId(clazz, cfg.getName());
            try {
                graph.add(_current);
                _allSeen.add(_current);
                ConfigToolUtil.getUpdateReferences(cfg, this);
            } finally {
                _current = null;
            }
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
            try {
                graph.addDependency(id, _current);
            } catch (Exception e) {
                log.warning("Oh fugging shit",
                    "id", id, "seen id?", _allSeen.contains(id),
                    "current", _current, "seen current?", _allSeen.contains(_current),
                    e);
            }
            return true;
        }

        /** The config we're currently examining while adding dependencies. */
        protected ConfigId _current;

        /** All valid config classes. */
        protected Set<Class<?>> _cfgClasses = Sets.newHashSet();

        // TEMP: for debugging when flattening goes awry
        protected Set<ConfigId> _allSeen = Sets.newHashSet();
    }
}
