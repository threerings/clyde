//
// $Id$

package com.threerings.config.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

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

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.DerivedConfig;
import com.threerings.config.ManagedConfig;
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
     * A default DependencyGatherer.
     */
    public static class Default extends DependencyGatherer
    {
        public Default (ConfigManager cfgmgr)
        {
            super(cfgmgr);
        }

        public Default (ManagedConfig config)
        {
            super(config);
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

        protected SetMultimap<Class<? extends ManagedConfig>, ConfigReference<?>> _refs =
                HashMultimap.create();
    }

    /**
     * Construct a dependency gatherer for the entire config manager.
     */
    public DependencyGatherer (ConfigManager cfgmgr)
    {
        for (ConfigGroup<?> group : cfgmgr.getGroups()) {
            Class<? extends ManagedConfig> clazz = group.getConfigClass();
            for (ManagedConfig cfg : group.getRawConfigs()) {
                populateParameters(clazz, cfg);
            }
        }
    }

    /**
     * Construct a dependency gatherer for a single config.
     */
    public DependencyGatherer (ManagedConfig config)
    {
        //populateParameters(config.getConfigGroup().getConfigClass(), config);
    }

    public void gather (ManagedConfig config)
    {
        findReferences(config);
    }

    public void gather (ConfigManager cfgmgr)
    {
        for (ConfigGroup<?> group : cfgmgr.getGroups()) {
            Class<? extends ManagedConfig> clazz = group.getConfigClass();
            for (ManagedConfig cfg : group.getRawConfigs()) {
                findReferences(cfg);
            }
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
                !seen.add(val)) {
            return;
        }
        if (val instanceof ConfigManager) {
            // do not recurse inside other config managers...
            // Shit

        } else if (val instanceof ConfigReference<?>) {
            ConfigReference<?> ref = (ConfigReference<?>)val;
            Class<? extends ManagedConfig> clazz = getConfigReferenceType(type);
            if (clazz == null) {
                log.warning("==== Found reference of unknown type: " + ref);
                // TODO: ???
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
                if (o instanceof String) {
                    // this may be a reference! (otherwise: we don't care)
                    Reference refAnno = f.getAnnotation(Reference.class);
                    if (refAnno != null) {
                        // TODO: HERE: for rewriting the field value to a unique String...
                        // add it!
                        add(refAnno.value(), new ConfigReference<ManagedConfig>((String)o));
                    }

                } else {
                    findReferences(o, f.getGenericType(), seen);
                }
            }
        }
    }

    /**
     * Add the specified config reference, and any others contained in its parameters.
     */
    protected void findReferences (
            ConfigReference<?> ref, Class<? extends ManagedConfig> clazz, Set<Object> seen)
    {
        // add the reference
        add(clazz, ref);

        // examine the arguments of the reference...
        ConfigId id = new ConfigId(clazz, ref.getName());
        for (Map.Entry<String, Object> entry : ref.getArguments().entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            Class<? extends ManagedConfig> pclazz = _paramCfgTypes.get(id, entry.getKey());
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
                    log.warning("Expecting reference parameter: " + value);
                }

            } else {
                findReferences(value, value.getClass(), seen);
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
                    continue;
                }
            }
            _paramCfgTypes.put(id, param.name, refClazz);
        }
    }

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

//    /**
//     * Look at the generic type of a parameter to see if it expects ConfigReferences in some way.
//     */
//    protected void populateParameterConfigType (ConfigId id, String name, Type type)
//    {
//        if (type instanceof ParameterizedType) {
//            ParameterizedType ptype = (ParameterizedType)type;
//            Type rawType = ptype.getRawType();
//            if (ConfigReference.class.equals(rawType)) {
//                @SuppressWarnings("unchecked")
//                Class<ManagedConfig> clazz =
//                        (Class<ManagedConfig>)ptype.getActualTypeArguments()[0];
//                _paramCfgTypes.put(id, name, clazz);
//
//            } else if (List.class.equals(rawType)) {
//                populateParameterConfigType(id, name, ptype.getActualTypeArguments()[0]);
//            }
//        }
//        // TODO: fuck arrays.... arrays never worked well because of the generic arguments
//        // and we support Lists now. We love Lists.
//        // Nothing using flattening should have config reference arrays.
//        // And even if they did, we'd have a Class type here
//        // (like ProjectX's ItemConfigReference), and we'd have to dive into that to discover
//        // that it has an array. Fuck that.
//    }

    /** A cache of field information. */
    protected final FieldCache _fieldCache = new FieldCache();

    /** A mapping of config/parameter to the type of config that parameter expects, if any. */
    protected final Table<ConfigId, String, Class<? extends ManagedConfig>> _paramCfgTypes =
                HashBasedTable.create();

}
