//
// $Id$

package com.threerings.config.tools;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.io.Files;
import com.google.common.primitives.Primitives;

import com.samskivert.util.DependencyGraph;
import com.samskivert.util.StringUtil;

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

import com.threerings.editor.Property;
import com.threerings.editor.Strippable;
import com.threerings.editor.util.PropertyUtil;

import static com.threerings.ClydeLog.log;

/**
 * Flattens configs for export to clients such that there are no parameters.
 * <em>Also performs stripping, honoring the Strippable annotation.</em>
 *
 * This is not an ant task because you probably need to pass jvmarg arguments,
 * and Sur-Fucking-PRIZE! You can't do that with a proper ant task.?!?!?!
 *
 * You probably want something like this in your build.xml:
 * <pre>{@code
 *
 * <!-- flatten configs for the client -->
 * <target name="flatten" depends="-preptools">
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

        new ConfigFlattener().flattenAndStrip(rsrcDir, outDir, ext, isXML);
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
     * A handy ConfigManager that strips all its configs just prior to saving.
     */
    public static class StripOnSaveConfigManager extends ConfigManager
    {
        public StripOnSaveConfigManager (
                ResourceManager rsrcmgr, MessageManager msgmgr, String configPath)
        {
            super(rsrcmgr, msgmgr, configPath);
        }

        public StripOnSaveConfigManager () {}

        @Override
        protected ManagedConfig[] toSaveableArray (
                Class<? extends ManagedConfig> groupClass,
                Iterable<? extends ManagedConfig> configs,
                Class<? extends ManagedConfig> arrayElementClass)
        {
            // hide the class outright from the client if the whole thing is strippable
            if (groupClass.isAnnotationPresent(Strippable.class)) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<? extends ManagedConfig> stripList = (List<? extends ManagedConfig>)
                    // the flattener needs a list to work with
                    PropertyUtil.strip(this, Lists.newArrayList(configs));
            return super.toSaveableArray(groupClass, stripList, arrayElementClass);
        }
    }

    /**
     * Potential entry point for other tools.
     */
    public void flattenAndStrip (String rsrcDir, String outDir)
        throws IOException
    {
        flattenAndStrip(rsrcDir, outDir, ".xml", true);
    }

    /**
     * Potential entry point for other tools.
     */
    public void flattenAndStrip (String rsrcDir, String outDir, String extension, boolean isXML)
        throws IOException
    {
        FlattenContext ctx = new FlattenContext(rsrcDir, outDir, true);

        flatten(ctx.cfgmgr);

        // Save everything!
//        log.info("Saving...");
        ctx.cfgmgr.saveAll(ctx.destDir, extension, isXML);
//        log.info("Done!");

        // also copy the manager properties over
        copyManagerProperties(
                "rsrc/config/manager.properties",
                new File(ctx.configDir, "manager.properties"),
                new File(ctx.destDir, "manager.txt"));
    }

    /**
     * Flatten all the configs in-place in the specified config manager.
     * Stripping is not performed- that is up to the ConfigManager you use..
     */
    public void flatten (ConfigManager cfgmgr)
    {
        DependentReferenceSet refSet = new DependentReferenceSet();

        // prior to losing parameter/derivation information, examine parameters
        refSet.examineParameters(cfgmgr);

        // turn all derived configs into their "original" form
        for (ConfigGroup<?> group : cfgmgr.getGroups()) {
            for (DerivedConfig der : Lists.newArrayList(
                    Iterables.filter(group.getRawConfigs(), DerivedConfig.class))) {
                // get the non-raw version and re-store it, overwriting
                group.addConfig(group.getConfig(der.getName()));
            }
        }

        // then populate those configs into the refSet
        refSet.populate(cfgmgr);

        // now go through each ref in dependency ordering
//        log.info("Flattening...");
        int count = 0;
        while (!refSet.graph.isEmpty()) {
            count++;
            ConfigId id = refSet.graph.removeAvailableElement();
            //log.info("Checking " + id);
            @SuppressWarnings("unchecked")
            ConfigGroup<ManagedConfig> group =
                    (ConfigGroup<ManagedConfig>)cfgmgr.getGroup(id.clazz);
            ManagedConfig cfg = group.getConfig(id.name);
            List<ConfigReference<?>> list = refSet.refs.get(id);
            if (!list.isEmpty()) {
                //log.info("References for that: " + list);
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
     * Read the manager properties.
     */
    public Properties getManagerProperties (File source)
        throws IOException
    {
        Properties props = new Properties();
        props.load(new FileReader(source));
        return props;
    }

    /**
     * Copy the manager.properties file, removing config types that are wholly stripped.
     */
    public void copyManagerProperties (String sourceIdentifier, File source, File dest)
        throws IOException
    {
        Files.write(getStrippedManagerProperties(getManagerProperties(source)), dest);
    }

    /**
     * Get a byte[] representing the specified manager properties, stripped.
     */
    public byte[] getStrippedManagerProperties (Properties props)
        throws IOException
    {
        stripManagerProperties(props);

        // output it to a string
        StringWriter writer = new StringWriter();
        props.store(writer, "");
        String value = writer.toString();

        // strip the goddamn comments out of the string
        // Pattern: multiline mode; if line begins with # strip from there to linebreak
        final String PATTERN = "(?m)" + // multiline mode
                "^\\#" + // look for a # at the beginning of a line...
                ".*" + // followed by anything...
                "[\\r\\n]+"; // ending with one or more CR/NL characters. (TODO JDK8: "\\R")
                // (Note that we can't use "$" because the newlines will be outside of the pattern)
        value = value.replaceAll(PATTERN, ""); // replace pattern with nothing

        // encode the string to UTF8
        return value.getBytes(Charsets.UTF_8);
    }

    /**
     * Alter the manager.properties to remove stripped classes.
     */
    public void stripManagerProperties (Properties props)
    {
        List<String> types = Lists.newArrayList(
                StringUtil.parseStringArray(props.getProperty("types", "")));
        types.add("resource");
        for (String type : types) {
            String key = type + ".classes";
            List<String> classes = Lists.newArrayList(StringUtil.parseStringArray(
                        props.getProperty(key, "")));
            boolean changed = false;
            for (Iterator<String> itr = classes.iterator(); itr.hasNext(); ) {
                try {
                    Class<?> clazz = Class.forName(itr.next());
                    if (clazz.isAnnotationPresent(Strippable.class)) {
                        itr.remove();
                        changed = true;
                    }

                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("This shouldn't happen", e);
                }
            }
            if (changed) {
                props.put(key, StringUtil.joinEscaped(Iterables.toArray(classes, String.class)));
            }
        }
    }

    /**
     * Generate a stable hash code for the specified argument map.
     */
    protected int generateHash (ArgumentMap args)
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
    protected int generateHash (Object obj)
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
     * Helper for configuring and validating the source and dest directories.
     */
    protected static class FlattenContext
    {
        /** The config manager. */
        public final ConfigManager cfgmgr;

        /** The directory where the source configs are found. */
        public final File configDir;

        /** The destination directory. */
        public final File destDir;

        /**
         */
        public FlattenContext (String rsrcDir, String outDir, boolean stripOnSave)
            throws IOException
        {
            ResourceManager rsrcmgr = new ResourceManager(rsrcDir);
            this.configDir = rsrcmgr.getResourceFile("config/");

            Preconditions.checkArgument(configDir.isDirectory(), "%s isn't a directory", configDir);
            Preconditions.checkArgument(
                    new File(configDir, "manager.properties").exists() ||
                    new File(configDir, "manager.txt").exists(), "cannot find manager descriptor");

            this.destDir = new File(outDir);
            Preconditions.checkArgument(destDir.isDirectory(), "%s isn't a directory", destDir);

            MessageManager msgmgr = new MessageManager("rsrc.i18n");
            this.cfgmgr = stripOnSave
                ? new StripOnSaveConfigManager(rsrcmgr, msgmgr, "config/")
                : new ConfigManager(rsrcmgr, msgmgr, "config/");
//            log.info("Starting up...");
            cfgmgr.init();
        }
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
         * Examine the parameters for type information.
         */
        public void examineParameters (ConfigManager cfgmgr)
        {
            for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                Class<? extends ManagedConfig> clazz = group.getConfigClass();
                for (ParameterizedConfig cfg :
                        Iterables.filter(group.getRawConfigs(), ParameterizedConfig.class)) {
                    populateParamterConfigTypes(clazz, cfg);
                }
            }
        }

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
                for (ManagedConfig cfg : group.getRawConfigs()) {
                    graph.add(new ConfigId(clazz, cfg.getName()));
//                    if (!_allSeen.add(new ConfigId(clazz, cfg.getName()))) {
//                        log.warning("Already added to allseen: " +
//                                new ConfigId(clazz, cfg.getName()));
//                    }
                    count++;
                }
            }
//            log.info("Populated " + count);

//            log.info("Gathering configs...");
            // then go through again, track refs, and make note of dependencies
            try {
                for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                    Class<? extends ManagedConfig> clazz = group.getConfigClass();
                    for (ManagedConfig cfg : group.getRawConfigs()) {
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
//                if (!_allSeen.add(_current)) {
//                    log.warning("Already added to allseen; " + _current);
//                }
                //System.err.println("Adding refs for " + _current);
                ConfigToolUtil.getUpdateReferences(cfg, this);
                //System.err.println("           done "  + _current);
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
                if (!graph.dependsOn(id, _current)) {
                    graph.addDependency(id, _current);
                }
            } catch (Exception e) {
                log.warning("Oh fugging shit",
                    "id", id,
//                    "seen id?", _allSeen.contains(id),
                    "current", _current,
//                    "seen current?", _allSeen.contains(_current),
                    e);
            }

            // recurse and add any arguments inside the arguments
            ConfigId oldCurrent = _current;
            _current = id;
            try {
                for (Map.Entry<String, Object> entry : ref.getArguments().entrySet()) {
                    Class<ManagedConfig> pclazz = _paramCfgTypes.get(id, entry.getKey());
                    if (pclazz == null) {
                        continue;
                    }
                    Object value = entry.getValue();
                    if (value instanceof ConfigReference<?>) {
                        @SuppressWarnings("unchecked")
                        ConfigReference<ManagedConfig> pref = (ConfigReference<ManagedConfig>)value;
                        add(pclazz, pref);

                    } else if (value instanceof List<?>) {
                        @SuppressWarnings("unchecked")
                        List<ConfigReference<ManagedConfig>> list =
                                (List<ConfigReference<ManagedConfig>>)value;
                        for (ConfigReference<ManagedConfig> pref : list) {
                            add(pclazz, pref);
                        }
                    }
                }
            } finally {
                _current = oldCurrent;
            }

            return true;
        }

        /**
         * Determine the config reference class for parameters in this config.
         */
        protected void populateParamterConfigTypes (
                Class<? extends ManagedConfig> clazz, ParameterizedConfig cfg)
        {
            ConfigId id = new ConfigId(clazz, cfg.getName());
            for (Parameter param : cfg.parameters) {
                Property prop = param.getProperty(cfg);
                addParameterConfigType(id, param.name, prop.getGenericType());
            }
        }

        /**
         * Look at the type of a parameter to see if it expects ConfigReferences in
         * some way.
         */
        protected void addParameterConfigType (ConfigId id, String name, Type type)
        {
            if (type instanceof ParameterizedType) {
                ParameterizedType ptype = (ParameterizedType)type;
                Type rawType = ptype.getRawType();
                if (ConfigReference.class.equals(rawType)) {
                    @SuppressWarnings("unchecked")
                    Class<ManagedConfig> clazz =
                            (Class<ManagedConfig>)ptype.getActualTypeArguments()[0];
                    _paramCfgTypes.put(id, name, clazz);

                } else if (List.class.equals(rawType)) {
                    addParameterConfigType(id, name, ptype.getActualTypeArguments()[0]);

                } else {
                    log.info("What's that?", "rawType", rawType);
                }
            }
            // TODO: fuck arrays.... arrays never worked well because of the generic arguments
            // and we support Lists now. We love Lists.
            // Nothing using flattening should have config reference arrays.
            // And even if they did, we'd have a Class type here
            // (like ProjectX's ItemConfigReference), and we'd have to dive into that to discover
            // that it has an array. Fuck that.
        }

        /** The config we're currently examining while adding dependencies. */
        protected ConfigId _current;

        /** All valid config classes. */
        protected Set<Class<?>> _cfgClasses = Sets.newHashSet();

        /** A mapping of config/parameter to the type of config that parameter expects, if any. */
        public final Table<ConfigId, String, Class<ManagedConfig>> _paramCfgTypes =
                HashBasedTable.create();

//        // TEMP: for debugging when flattening goes awry
//        protected Set<ConfigId> _allSeen = Sets.newHashSet();
    }
}
