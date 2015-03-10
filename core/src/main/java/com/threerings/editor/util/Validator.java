//
// $Id$

package com.threerings.editor.util;

import java.io.PrintStream;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.samskivert.util.StringUtil;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import com.threerings.resource.ResourceManager;

import com.threerings.config.ArgumentMap;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.DerivedConfig;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Parameter;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.Reference;
import com.threerings.config.ReferenceConstraints;
import com.threerings.config.util.ConfigId;

import com.threerings.editor.Editable;
import com.threerings.editor.FileConstraints;
import com.threerings.editor.Introspector;
import com.threerings.editor.Property;

/**
 * Validates configs and resources.
 */
public class Validator
{
    /**
     * Create a new validator with a blank "where" to start.
     */
    public Validator (PrintStream out)
    {
        _out = out;
    }

    /**
     * Create a new validator with the specified "where".
     */
    public Validator (String where, PrintStream out)
    {
        this(out);
        pushWhere(where);
    }

    /**
     * Get the current "where" value.
     */
    public String getWhere ()
    {
        return Joiner.on(" : ").join(_wheres);
    }

    /**
     * Get the printstream to which we show errors.
     */
    public PrintStream getPrintStream ()
    {
        return _out;
    }

    /**
     * Output a validation error message.
     */
    public void output (String message)
    {
        _out.println(getWhere() + " " + message);
    }

    /**
     * Push an identifier onto the "where" string.
     * This should be immediately followed by a try block that pops in its finally.
     */
    public void pushWhere (String where)
    {
        _wheres.addLast(where);
    }

    /**
     * Pop a "where" identifier off.
     */
    public void popWhere ()
    {
        _wheres.removeLast();
    }

    /**
     * Gather and validate all config references within the specified object, which may be
     * an array or iterable of objects.
     */
    public boolean validate (ConfigManager cfgmgr, Object object)
    {
        Preconditions.checkNotNull(cfgmgr);
        preValidate();
        try {
            getReferences(cfgmgr, object);
            return validate(cfgmgr);
        } finally {
            postValidate();
        }
    }

    /**
     * A hook for a pre-validate step.
     * Ensure that we're not re-entrantly validating.
     */
    protected void preValidate ()
    {
        Preconditions.checkState(_configs.isEmpty());
        Preconditions.checkState(_resources.isEmpty());
        Preconditions.checkState(_configsByConstraints == null);
    }

    /**
     * Hook for a post-validate step.
     * Clear any internal data structures.
     */
    protected void postValidate ()
    {
        _configs.clear();
        _resources.clear();
        _hasNulls = false;
        _configsByConstraints = null;
    }

    /**
     * Finds all configs and resources in the specified object.
     */
    protected void getReferences (ConfigManager cfgmgr, Object object)
    {
        if (object == null) {
            return;
        }
        if (object instanceof Object[]) {
            for (Object element : (Object[])object) {
                getReferences(cfgmgr, element);
            }
            return;
        }
        if (object instanceof Iterable) {
            for (Object element : ((Iterable<?>)object)) {
                getReferences(cfgmgr, element);
            }
            return;
        }
        for (Property property : Introspector.getProperties(object)) {
            getReferences(cfgmgr, object, property);
        }
    }

    /**
     * Finds all configs and resources referenced by the supplied property of the supplied object
     * and places them in the given sets.  This is not without side effects: when it finds
     * arguments in references that don't correspond to matching parameters, it strips them out.
     */
    protected void getReferences (ConfigManager cfgmgr, Object object, Property property)
    {
        Editable annotation = property.getAnnotation();
        Object value = property.get(object);
        if (value == null) {
            if (isNullValidated() && !annotation.nullable()) {
                output("Non-nullable property is null: " + property.getName());
            }
            return;
        }

        // TODO: it would be keen to validate properties against their Editable annotation
        // to ensure that things like min/max or whatever are still valid. This would probably
        // need to be handled individually by each type of PropertyEditor.

        String editor = annotation.editor();
        if (editor.equals("resource")) {
            addResource((String)value, property, _resources);
            return;
        }
        // see if it's a reference property
        Reference refAnno = property.getAnnotation(Reference.class);
        if (refAnno != null) {
            noteReference(cfgmgr, refAnno.value(), (String)value, property);
            return;
        }
        // also check the old way...
        if (editor.equals("config")) {
            @SuppressWarnings("unchecked")
            ConfigGroup<ManagedConfig> group =
                    (ConfigGroup<ManagedConfig>)cfgmgr.getGroup(annotation.mode());
            if (group != null) {
                noteReference(cfgmgr, group.getConfigClass(), (String)value, property);
            }
            return;
        }

        Class<?> type = property.getType();
        if (ConfigReference.class.equals(type)) {
            Class<?> clazz = property.getArgumentType(ConfigReference.class);
            if (clazz == null && (object instanceof DerivedConfig)) {
                clazz = ((DerivedConfig)object).cclass;
            }
            @SuppressWarnings("unchecked") Class<ManagedConfig> cclass =
                (Class<ManagedConfig>)clazz;
            @SuppressWarnings("unchecked") ConfigReference<ManagedConfig> ref =
                (ConfigReference<ManagedConfig>)value;
            noteReference(cfgmgr, cclass, ref, property);
            return;
        }

        // validate an array or list of config references
        if ((type.isArray() || List.class.isAssignableFrom(type)) &&
                (ConfigReference.class.equals(property.getComponentType()))) {
            @SuppressWarnings("unchecked")
            List<ConfigReference<?>> list = type.isArray()
                ? Arrays.asList((ConfigReference<?>[])value)
                : (List<ConfigReference<?>>)value;
            Type ctype = property.getGenericComponentType();
            if (!(ctype instanceof ParameterizedType)) {
                output("can't determine type of refs");
                return;
            }
            @SuppressWarnings("unchecked")
            Class<ManagedConfig> cclass = (Class<ManagedConfig>)
                    ((ParameterizedType)ctype).getActualTypeArguments()[0];
            for (ConfigReference<?> ref : list) {
                if (ref == null) {
                    _hasNulls = true;
                } else {
                    noteReference(cfgmgr, cclass, ref, property);
                }
            }
            return;
        }

        // otherwise: no special handling
        getReferences(cfgmgr, value);
    }

    /**
     * Note a config reference.
     */
    protected void noteReference (
            ConfigManager cfgmgr, Class<? extends ManagedConfig> clazz, String name,
            Property property)
    {
        ConfigId configId = new ConfigId(clazz, name);
        ReferenceConstraints constraints = property.getAnnotation(ReferenceConstraints.class);
        if (constraints == null) {
            _configs.add(configId);

        } else {
            if (_configsByConstraints == null) {
                _configsByConstraints = ArrayListMultimap.create();
            }
            _configsByConstraints.put(constraints, configId);
        }
    }

    /**
     * Note a config reference and possibly massage the args.
     */
    protected void noteReference (
            ConfigManager cfgmgr, Class<? extends ManagedConfig> clazz, ConfigReference<?> ref,
            Property property)
    {
        noteReference(cfgmgr, clazz, ref.getName(), property);

        ArgumentMap args = ref.getArguments();
        if (args.isEmpty()) {
            return;
        }
        ManagedConfig config = cfgmgr.getRawConfig(clazz, ref.getName());
        if (!(config instanceof ParameterizedConfig)) {
            args.clear();
            return;
        }
        ParameterizedConfig pconfig = (ParameterizedConfig)config;
        for (Iterator<Map.Entry<String, Object>> it = args.entrySet().iterator();
                it.hasNext(); ) {
            Map.Entry<String, Object> entry = it.next();
            Parameter param = pconfig.getParameter(entry.getKey());
            if (param == null) {
                it.remove(); // argument is obsolete; strip it out
                continue;
            }
            Property prop = param.getArgumentProperty(pconfig);
            if (prop != null) {
                getReferences(cfgmgr, args, prop);
            }
        }
    }

    /**
     * Validate the internal data structures against the supplied config manager.
     */
    protected boolean validate (ConfigManager cfgmgr)
    {
        boolean result = true;
        if (_hasNulls) {
            output("has null config references");
            result = false;
        }
        for (ConfigId configId : _configs) {
            if (cfgmgr.getConfig(configId.clazz, configId.name) == null) {
                noteMissing(configId);
                result = false;
            }
        }
        if (_configsByConstraints != null) {
            for (Map.Entry<ReferenceConstraints, List<ConfigId>> entry :
                    Multimaps.asMap(_configsByConstraints).entrySet()) {
                Predicate<ManagedConfig> pred = PropertyUtil.getRawConfigPredicate(entry.getKey());
                for (ConfigId configId : entry.getValue()) {
                    ManagedConfig cfg = cfgmgr.getRawConfig(configId.clazz, configId.name);
                    if (cfg == null) {
                        noteMissing(configId);
                        result = false;
                    } else if (!pred.apply(cfg)) {
                        output("references invalid config: " + configId.name);
                        result = false;
                    }
                }
            }
        }
        for (Resource resource : _resources) {
            if (!resource.validate(cfgmgr)) {
                result = false;
                output("references missing resource: " + resource);
            }
        }
        return result;
    }

    /**
     * Note a missing config.
     */
    protected void noteMissing (ConfigId configId)
    {
        output("references missing config of type " +
                ConfigGroup.getName(configId.clazz) + ": " + configId.name);
    }

    /**
     * Adds a resource to the resources set.  If the resource has a stripped extension, it will
     * postfix an extension if a there's a single valid extension available.
     */
    protected void addResource (String value, Property property, Set<Resource> resources)
    {
        FileConstraints constraints = property.getAnnotation(FileConstraints.class);
        if (constraints != null && constraints.stripExtension()) {
            String[] exts = constraints.extensions();
            resources.add(createResource(value, exts));
            return;
        }

        resources.add(createResource(value, null));
    }

    /**
     * Create resource object.
     */
    protected Resource createResource (String path, String[] extensions)
    {
        return new Resource(path, extensions);
    }

    /**
     * Do we validate properties that are null?
     */
    protected boolean isNullValidated ()
    {
        // legacy: we never used to, so the default is not to. This is overridden in trinity!
        return false;
    }

    /**
     * Wrapper class for resource path and extension. Used for validating the resource.
     */
    protected class Resource
    {
        /** Resource path. */
        public String path;

        /** Resource potential extensions. If null, extension will be in path variable. */
        public String[] extensions;

        /** Default constructor. */
        public Resource (String path, String[] extensions)
        {
            this.path = path;
            this.extensions = extensions;
            if (this.extensions != null) {
                Arrays.sort(this.extensions);
            }
        }

        /**
         * Validate the resource.
         */
        public boolean validate (ConfigManager cfgmgr)
        {
            if (extensions == null) {
                return validateFullPath(cfgmgr, path);
            }

            boolean result = false;
            for (String extension : extensions) {
                result |= validateFullPath(cfgmgr, path + extension);
            }

            return result;
        }

        /**
         * Validate the specified path with extension.
         */
        protected boolean validateFullPath (ConfigManager cfgmgr, String path)
        {
            return cfgmgr.getResourceManager().getResourceFile(path).exists();
        }

        @Override
        public boolean equals (Object o)
        {
            return (o instanceof Resource) && ((Resource)o).path.equals(this.path)
                && Arrays.equals(((Resource)o).extensions, this.extensions);
        }

        @Override
        public int hashCode ()
        {
            return Objects.hashCode(path, this.extensions);
        }

        @Override
        public String toString ()
        {
            return path + ", " + Arrays.toString(extensions);
        }
    }

    /** Our output stream. */
    protected final PrintStream _out;

    /** The current location context. */
    protected Deque<String> _wheres = new ArrayDeque<String>();

    /** The gathered resources while validating the current object. */
    protected Set<Resource> _resources = Sets.newHashSet();

    /** The gathered configs while validating the current object. */
    protected Set<ConfigId> _configs = Sets.newHashSet();

    /** If we have null config references. */
    protected boolean _hasNulls;

    /** Configs organized by their constraints. Initialized only when needed. */
    protected ListMultimap<ReferenceConstraints, ConfigId> _configsByConstraints;
}
