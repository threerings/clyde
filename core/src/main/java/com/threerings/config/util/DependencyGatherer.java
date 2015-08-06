//
// $Id$

package com.threerings.config.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.nio.Buffer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import com.google.common.primitives.Primitives;

import com.samskivert.util.Logger;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.DerivedConfig;
import com.threerings.config.ManagedConfig;
import com.threerings.config.NoDependency;
import com.threerings.config.Parameter;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.Reference;
import com.threerings.config.util.ConfigId;

import com.threerings.editor.Property;

import static com.threerings.ClydeLog.log;

/**
 * Automatically gathers dependencies from configs.
 */
public abstract class DependencyGatherer
{
    /**
     * Convenience method for gathering the dependencies of a single config.
     */
    public static SetMultimap<Class<? extends ManagedConfig>, ConfigReference<?>> gather (
            ConfigManager cfgmgr, ManagedConfig config)
    {
        return new Default(cfgmgr, config).getGathered();
    }

    /**
     * A default DependencyGatherer for use inside ManagedConfigs.
     */
    public static class Default extends DependencyGatherer
    {
        /**
         * Construct a Default gatherer.
         */
        public Default (ConfigManager cfgmgr, ManagedConfig config)
        {
            _cfgmgr = cfgmgr;
            findReferences(config);
        }

        /**
         * Get the gathered up references.
         */
        public SetMultimap<Class<? extends ManagedConfig>, ConfigReference<?>> getGathered ()
        {
            return Multimaps.unmodifiableSetMultimap(_refs);
        }

        @Override
        protected void add (
                Class<? extends ManagedConfig> clazz, @Nullable ConfigReference<?> ref)
        {
            if (ref != null) {
                _refs.put(clazz, ref);
            }
        }

        @Override
        protected Class<? extends ManagedConfig> getParameterConfigType (ConfigId id, String param)
        {
            ManagedConfig cfg = _cfgmgr.getRawConfig(id.clazz, id.name);
            if (!(cfg instanceof ParameterizedConfig)) {
                log.warning("Expected parameterized config, got none", "id", id, "cfg", cfg);
                return null;
            }
            ParameterizedConfig pcfg = (ParameterizedConfig)cfg;
            Parameter p = pcfg.getParameter(param);
            if (p == null) {
                log.warning("Missing parameter!", "id", id, "param", param);
                return null;
            }

            Property prop = p.getProperty(pcfg);
            if (prop == null) {
                log.warning("No property for parameter?", "id", id, "param", param);
                return null;
            }
            Reference refAnno = prop.getAnnotation(Reference.class);
            if (refAnno != null) {
                return refAnno.value();
            }
            return getConfigReferenceType(prop.getGenericType());
        }

        @Override
        protected void findArgumentReferences (
                ConfigReference<?> ref, Class<? extends ManagedConfig> clazz, Set<Object> seen)
        {
            // don't do that here
        }

//        @Override
//        protected boolean shouldWarn ()
//        {
//            return false;
//        }

        /** Where we look to determine parameter types for config references. */
        protected final ConfigManager _cfgmgr;

        /** Our gathered references. */
        protected final SetMultimap<Class<? extends ManagedConfig>, ConfigReference<?>> _refs =
                HashMultimap.create();
    }

    /**
     * A gatherer that pre-fetches argument information, such that the config manager
     * can be flattened and the configs altered at the time of gathering.
     */
    public abstract static class PreExamined extends DependencyGatherer
    {
        /**
         * Construct a PreExamined gatherer, examining parameter information in the specified mgr.
         */
        public PreExamined (ConfigManager cfgmgr)
        {
            for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                Class<? extends ManagedConfig> clazz = group.getConfigClass();
                for (ManagedConfig cfg : group.getRawConfigs()) {
                    populateParameters(clazz, cfg);
                }
            }
        }

        /**
         * Gather references from another manager, using pre-calculated parameter types.
         */
        public void gather (ConfigManager cfgmgr)
        {
            for (ConfigGroup<?> group : cfgmgr.getGroups()) {
                for (ManagedConfig cfg : group.getRawConfigs()) {
                    findReferences(cfg);
                }
            }
        }

        /**
         * Figure out if any of the parameters of the specified config expect ConfigReferences,
         * and record their type.
         */
        protected void populateParameters (Class<? extends ManagedConfig> clazz, ManagedConfig cfg)
        {
            if (!(cfg instanceof ParameterizedConfig)) {
                return;
            }
            ParameterizedConfig pcfg = (ParameterizedConfig)cfg;
            ConfigId id = new ConfigId(clazz, cfg.getName());
            for (Parameter param : pcfg.parameters) {
                Property prop = param.getProperty(pcfg);
                if (prop == null) {
                    log.warning("No property for parameter?",
                            "config", cfg.getName(), "class", clazz, "param", param.name);
                    continue;
                }
                Reference refAnno = prop.getAnnotation(Reference.class);
                Class<? extends ManagedConfig> refClazz;
                if (refAnno != null) {
                    refClazz = refAnno.value();

                } else {
                    refClazz = getConfigReferenceType(prop.getGenericType());
                    if (refClazz == null) {
                        refClazz = UnknownParameterTypeMarker.class;
                    }
                }
                _paramCfgTypes.put(id, param.name, refClazz);
            }
        }

        @Override
        protected Class<? extends ManagedConfig> getParameterConfigType (ConfigId id, String param)
        {
            Class<? extends ManagedConfig> value = _paramCfgTypes.get(id, param);
            if (value == null) {
//                throw new RuntimeException(Logger.format("Bogus config and/or parameter!",
//                        "config", id, "parameter", param));
                log.warning("Bogus config and/or parameter! Ignoring.",
                        "config", id, "parameter", param);
                return null;
            }
            return (value == UnknownParameterTypeMarker.class)
                    ? null
                    : value;
        }

        /** A mapping of config/parameter to the type of config that parameter expects, if any. */
        protected final Table<ConfigId, String, Class<? extends ManagedConfig>> _paramCfgTypes =
                    HashBasedTable.create();

        /**
         * A marker class so we can distinguish between a missing parameter and an unknown type.
         */
        protected static class UnknownParameterTypeMarker extends ManagedConfig
        {
            // nothing
        }
    }

    /**
     * Add a reference to the set.
     */
    protected abstract void add (
            Class<? extends ManagedConfig> clazz, @Nullable ConfigReference<?> ref);

    /**
     * Find references inside the specified object.
     */
    protected void findReferences (ManagedConfig cfg)
    {
        Set<Object> seen = Sets.newIdentityHashSet();
        findReferences(cfg, cfg.getClass(), seen);
    }

    /**
     * Find references inside the specified object.
     */
    protected void findReferences (Object val, Type type, Set<Object> seen)
    {
        if (val == null) {
            return;
        }

        Class<?> c = val.getClass();
        if ((c == String.class) || c.isPrimitive() || Primitives.isWrapperType(c) ||
                (val instanceof ConfigManager) || (val instanceof Buffer) ||
                !seen.add(val)) {
            return;
        }
        if (val instanceof ConfigReference<?>) {
            ConfigReference<?> ref = (ConfigReference<?>)val;
            Class<? extends ManagedConfig> clazz = getConfigReferenceType(type);
            if (clazz == null) {
                if (shouldWarn()) {
                    log.warning("==== Found reference of unknown type: " + ref);
                }
                return;
            }
            // defer to special handling for ConfigReference
            findReferences(ref, clazz, seen);

        } else if (c.isArray()) {
            Type cType = c.getComponentType();
            for (int ii = 0, nn = Array.getLength(val); ii < nn; ii++) {
                findReferences(Array.get(val, ii), cType, seen);
            }

        } else if (val instanceof Collection) {
            Type cType = (type instanceof ParameterizedType)
                    ? ((ParameterizedType)type).getActualTypeArguments()[0]
                    : null;
            for (Object o : ((Collection)val)) {
                if (o == null) {
                    continue;
                }
                Type eType = (cType != null)
                    ? cType
                    : o.getClass();
                findReferences(o, eType, seen);
            }

        } else if (val instanceof DerivedConfig) {
            DerivedConfig dCfg = (DerivedConfig)val;
            findReferences(dCfg.base, dCfg.cclass, seen);

        } else {
            // the reflective solution
            for (Field f : _fieldCache.getFields(c)) {
                Object o;
                try {
                    o = f.get(val);
                } catch (IllegalAccessException iae) {
                    continue; // don't worry about it
                }

                if ((o instanceof ConfigReference<?>) &&
                        (f.getAnnotation(NoDependency.class) != null)) {
                    noteNoDependency((ConfigReference<?>)o, f);
                    continue;
                }

                if (o instanceof String) {
                    // this may be a reference! (otherwise: we don't care)
                    Reference refAnno = f.getAnnotation(Reference.class);
                    if (refAnno != null) {
                        // add it!
                        String newValue = addBareReference(refAnno.value(), (String)o);
                        if (newValue != null) {
                            try {
                                f.set(val, newValue);

                            } catch (IllegalAccessException iae) {
                                throw new RuntimeException(iae); // shouldn't happen
                            }
                        }
                    }

                } else {
                    findReferences(o, f.getGenericType(), seen);
                }
            }
        }
    }

    /**
     * Note that we found a ConfigReference tagged with @NoDependency.
     */
    protected void noteNoDependency (ConfigReference<?> ref, Field f)
    {
        // nothing by default
    }

    /**
     * Add the specified config reference, and any others contained in its parameters.
     */
    protected void findReferences (
            ConfigReference<?> ref, Class<? extends ManagedConfig> clazz, Set<Object> seen)
    {
        // add the reference
        add(clazz, ref);

        findArgumentReferences(ref, clazz, seen);
    }

    /**
     * Add any config references that are arguments in the specified reference.
     */
    protected void findArgumentReferences (
            ConfigReference<?> ref, Class<? extends ManagedConfig> clazz, Set<Object> seen)
    {
        // examine the arguments of the reference...
        ConfigId id = new ConfigId(clazz, ref.getName());
        for (Map.Entry<String, Object> entry : ref.getArguments().entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            Class<? extends ManagedConfig> pclazz = getParameterConfigType(id, entry.getKey());
            if (pclazz != null) {
                if (value instanceof ConfigReference<?>) {
                    findReferences((ConfigReference<?>)value, pclazz, seen);

                } else if (value instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<ConfigReference<ManagedConfig>> list =
                            (List<ConfigReference<ManagedConfig>>)value;
                    for (ConfigReference<ManagedConfig> pref : list) {
                        findReferences(pref, pclazz, seen);
                    }

                } else {
                    log.warning("Expected reference parameter but got something else.",
                            "ref", ref, "clazz", clazz, "valueType", value.getClass(),
                            "value", value);
                }

            } else {
                findReferences(value, value.getClass(), seen);
            }
        }
    }

    /**
     * Add a bare string reference.
     *
     * @return a new String if you want that value set back in the field, or null.
     */
    protected String addBareReference (Class<? extends ManagedConfig> clazz, String cfgName)
    {
        add(clazz, new ConfigReference<ManagedConfig>(cfgName));
        return null;
    }

    /**
     * Get the config type of the specified parameter, or null if unknown or
     * it's not a config-ish parameter.
     */
    protected abstract Class<? extends ManagedConfig> getParameterConfigType (
            ConfigId id, String param);

    /**
     * Should we warn about unusual things?
     */
    protected boolean shouldWarn ()
    {
        return true;
    }

    /**
     * Figure out the config type for the specified type, as best we can.
     *
     * This will accept a type like List<ConfigReference<ActorConfig>> and return ActorConfig.
     */
    protected Class<? extends ManagedConfig> getConfigReferenceType (Type type)
    {
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType)type;
            Type rawType = ptype.getRawType();
            if (ConfigReference.class.equals(rawType)) {
                @SuppressWarnings("unchecked")
                Class<? extends ManagedConfig> clazz =
                        (Class<? extends ManagedConfig>)ptype.getActualTypeArguments()[0];
                return clazz;

            } else if (List.class.equals(rawType)) {
                return getConfigReferenceType(ptype.getActualTypeArguments()[0]);
            }
            // TODO: other collection types?
        }

        // TODO: figure out arrays?
        return null;
    }

    /** A cache of field information. */
    protected final FieldCache _fieldCache = new FieldCache(Predicates.and(
                FieldCache.getDefaultPredicate(),
                new Predicate<Field>() {
                    public boolean apply (Field f) {
                        return !f.isSynthetic();
                    }
                }));
}
