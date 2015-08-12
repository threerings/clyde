//
// $Id$

package com.threerings.config.tools;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
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
import com.google.common.collect.Ordering;
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
import com.threerings.config.DerivedConfig;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Parameter;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.util.ConfigId;
import com.threerings.config.util.DependencyGatherer;

import com.threerings.editor.Property;
import com.threerings.editor.Strippable;
import com.threerings.editor.util.PropertyUtil;

import com.threerings.export.Exporter;

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
     * Types of suppressable warnings.
     */
    public enum SuppressableWarning
    {
        /** No config was found for a ConfigReference. */
        REFERENCE_NOT_SATISFIED("Reference not satisfied?"),

        ;

        /** The log message associated with this warning. */
        public final String message;

        /**
         * Constructor.
         */
        SuppressableWarning (String msg)
        {
            this.message = msg;
        }
    }

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
     * Called to suppress certain warnings on this flattener.
     */
    public ConfigFlattener suppressWarnings (SuppressableWarning... warnings)
    {
        _suppressedWarnings.addAll(Arrays.asList(warnings));
        return this;
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

        Exporter.Replacer replacer = flatten(ctx.cfgmgr);

        // Save everything!
//        log.info("Saving...");
        // TODO: use the replacer for the "superflat" bundle
        ctx.cfgmgr.saveAll(ctx.destDir, extension, isXML);
//        log.info("Done!");

        // also copy the manager properties over
        copyManagerProperties(
                new File(ctx.configDir, "manager.properties"),
                new File(ctx.destDir, "manager.txt"));
    }

    /**
     * Flatten all the configs in-place in the specified config manager.
     * Stripping is not performed- that is up to the ConfigManager you use..
     */
    public Exporter.Replacer flatten (ConfigManager cfgmgr)
    {
        // prior to losing parameter/derivation information, examine parameters
        FlatDependencyGatherer gatherer = new FlatDependencyGatherer(cfgmgr);
        ReferenceMapper mapper = createReferenceMapper(gatherer);

        // now go through each ref in dependency ordering
//        log.info("Flattening...");
        int count = 0;
        ConfigId id;
        while (null != (id = gatherer.getNextAvailable())) {
            count++;
            @SuppressWarnings("unchecked")
            ConfigGroup<ManagedConfig> group =
                    (ConfigGroup<ManagedConfig>)cfgmgr.getGroup(id.clazz);
            ManagedConfig cfg = group.getRawConfig(id.name);
            List<ConfigReference<?>> list = mapper.getReferences(id);
//            log.info("Checking " + id, "refCount", list.size());
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
                    args.keySet().retainAll(paramNames); // this modifies a ref in-place!
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
                        clearParameters(newCfg);
                        group.addConfig(newCfg, false);
//                        log.info("Mapped new config/argged with name '" + newCfg.getName() + "'");
                        newNames.put(key, newName);
                        mapper.noteFlattenedConfig(newCfg);
                    }

                    // now, copy a new config reference with the new name to the existing ref
                    new ConfigReference<ManagedConfig>(id.name + "~" + newName).copy(ref);
                }
            }

            // also remake the config with no parameters
            ManagedConfig newCfg = cfg.getInstance((ArgumentMap)null);
            clearParameters(newCfg);
            group.addConfig(newCfg, false);
//            log.info("Mapped new config with name '" + newCfg.getName() + "'");
            mapper.noteFlattenedConfig(newCfg);
        }

//        log.info("Flattened " + count);
        return mapper.getReplacer(cfgmgr);
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
    public void copyManagerProperties (File source, File dest)
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
     * Clear any parameter information on the specified config.
     */
    protected void clearParameters (ManagedConfig cfg)
    {
        if (cfg instanceof ParameterizedConfig) {
            ((ParameterizedConfig)cfg).parameters = Parameter.EMPTY_ARRAY;
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
     * Create the reference mapper to use.
     */
    protected ReferenceMapper createReferenceMapper (FlatDependencyGatherer gatherer)
    {
        return new ReferenceMapper(gatherer);
    }

    /**
     * Issue a suppressable warning, maybe.
     */
    protected void warn (SuppressableWarning type, Object... details)
    {
        if (!_suppressedWarnings.contains(type)) {
            log.warning(type.message, details);
        }
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

            this.cfgmgr = stripOnSave
                ? new StripOnSaveConfigManager(rsrcmgr, null, "config/")
                : new ConfigManager(rsrcmgr, null, "config/");
//            log.info("Starting up...");
            cfgmgr.init();
        }
    }

    /**
     * A gatherer that gathers dependencies between config references
     * as well as collects each ConfigReference uniquely for later rewriting.
     */
    protected class FlatDependencyGatherer extends DependencyGatherer.PreExamined
    {
        public FlatDependencyGatherer (ConfigManager cfgmgr)
        {
            super(cfgmgr);
            for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                Class<? extends ManagedConfig> clazz = group.getConfigClass();
                _cfgClasses.add(clazz);
                for (ManagedConfig cfg : group.getRawConfigs()) {
                    _graph.add(new ConfigId(clazz, cfg.getName()));
                }
            }
            gather(cfgmgr);
        }

        /**
         * Get the next not-depended-on config id, or null if we're finished.
         */
        public ConfigId getNextAvailable ()
        {
            return _graph.isEmpty()
                    ? null
                    : _graph.removeAvailableElement();
        }

        @Override
        protected void findReferences (ManagedConfig cfg)
        {
            Class<? extends ManagedConfig> clazz = cfg.getConfigGroup().getConfigClass();
            _current = new ConfigId(clazz, cfg.getName());
            try {
                super.findReferences(cfg);
            } finally {
                _current = null;
            }
        }

        @Override
        public void add (Class<? extends ManagedConfig> clazz, ConfigReference<?> ref)
        {
            // omit config refs with no args: we don't care
            if (ref == null || ref.getArguments().isEmpty() || !_cfgClasses.contains(clazz)) {
                return;
            }

            // add the dependency
            ConfigId id = new ConfigId(clazz, ref.getName());
            try {
                if (!_graph.dependsOn(id, _current)) {
                    _graph.addDependency(id, _current);
                }
            } catch (Exception e) {
                log.warning("Oh fugging shit",
                    "id", id,
//                        "seen id?", _allSeen.contains(id),
                    "current", _current,
//                        "seen current?", _allSeen.contains(_current),
                    e);
            }
        }

        @Override
        public Class<? extends ManagedConfig> getParameterConfigType (ConfigId id, String param)
        {
            // We are making this method public, nothing more.
            return super.getParameterConfigType(id, param);
        }

        @Override
        protected void findArgumentReferences (
                ConfigReference<?> ref, Class<? extends ManagedConfig> clazz, Set<Object> seen)
        {
            ConfigId oldCurrent = _current;
            _current = new ConfigId(clazz, ref.getName());
            try {
                super.findArgumentReferences(ref, clazz, seen);

            } finally {
                _current = oldCurrent;
            }
        }

        /** The config we're currently examining while adding dependencies. */
        protected ConfigId _current;

        /** All valid config classes. */
        protected final Set<Class<?>> _cfgClasses = Sets.newHashSet();

        /** The dependency graph. */
        protected final DependencyGraph<ConfigId> _graph = new DependencyGraph<ConfigId>();
    }

    /**
     * A gatherer that gathers dependencies between config references
     * as well as collects each ConfigReference uniquely for later rewriting.
     */
    protected class ReferenceMapper extends DependencyGatherer
    {
        public ReferenceMapper (FlatDependencyGatherer flatGatherer)
        {
            _flatGatherer = flatGatherer;
        }

        /**
         * Note a newly-flattened config.
         */
        public void noteFlattenedConfig (ManagedConfig cfg)
        {
            findReferences(cfg);
        }

        /**
         * Get all the references that point at the specified id.
         */
        public List<ConfigReference<?>> getReferences (ConfigId id)
        {
            return Collections.unmodifiableList(_refs.get(id));
        }

        /**
         * Get a replacer for exporting the bare configs instead of their references!
         */
        public Exporter.Replacer getReplacer (final ConfigManager cfgmgr)
        {
            return null;
        }

        @Override
        public void add (Class<? extends ManagedConfig> clazz, ConfigReference<?> ref)
        {
            if (ref != null) {
                mapRefToClass(ref, clazz);
            }

            // omit config refs with no args: we don't care
            if (ref == null || ref.getArguments().isEmpty() ||
                    !_flatGatherer._cfgClasses.contains(clazz)) {
                return;
            }

            // track the ref...
            ConfigId id = new ConfigId(clazz, ref.getName());
            _refs.put(id, ref);
        }

        @Override
        protected Class<? extends ManagedConfig> getParameterConfigType (ConfigId id, String param)
        {
            return _flatGatherer.getParameterConfigType(id, param);
        }

        @Override
        protected String addBareReference (Class<? extends ManagedConfig> clazz, String cfgName)
        {
            // make a new String that we can identify uniquely later
            cfgName = new String(cfgName);
            _bareToClass.put(cfgName, clazz);
            // tell super about it...
            super.addBareReference(clazz, cfgName);
            // return it so that it's re-set into the Field
            return cfgName;
        }

        @Override
        protected void noteNoDependency (ConfigReference<?> ref, Field f)
        {
            Class<? extends ManagedConfig> clazz = getConfigReferenceType(f.getGenericType());
            if (clazz == null) {
                log.warning("I can't figure out the type of a @NoDependency config",
                       "field name", f.getName());
                return;
            }
//
//            // to have it write as a config reference
//            _writeNoReplace.add(ref);
        }

        /**
         * Map the specified reference as refering to configs of the specified class.
         */
        protected void mapRefToClass (ConfigReference<?> ref, Class<? extends ManagedConfig> clazz)
        {
            Class<?> oldValue = _refToClass.put(ref, clazz);
            if (oldValue != null && oldValue != clazz) {
                log.warning("Holy shnikes, the same config ref for two types?",
                        "ref", ref, "clazz", clazz, "oldClass", oldValue);
            }
        }

        /** Our allied gatherer. */
        protected final FlatDependencyGatherer _flatGatherer;

        /** The references that point to a particular config, indexed by config. */
        protected final ListMultimap<ConfigId, ConfigReference<?>> _refs =
                ArrayListMultimap.create();

        /** Maps all config references to their class. */
        protected final Map<ConfigReference<?>, Class<?>> _refToClass = Maps.newIdentityHashMap();

        /** Maps bare references to their class. */
        protected final Map<String, Class<?>> _bareToClass = Maps.newIdentityHashMap();

//        /** Write the config reference as a config reference. */
//        // POSSIBLY TEMP: once superflattening is done right and Coaxing works on the client
//        protected final Set<Object> _writeNoReplace = Sets.newIdentityHashSet();
    }

    /** The suppressable warnings that are currently suppressed. */
    protected Set<SuppressableWarning> _suppressedWarnings =
            EnumSet.noneOf(SuppressableWarning.class);
}
