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

package com.threerings.editor.util;

import java.io.PrintStream;

import java.lang.reflect.Array;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

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
import com.threerings.editor.Editable;
import com.threerings.editor.Introspector;
import com.threerings.editor.Property;
import com.threerings.editor.Strippable;
import com.threerings.export.Exportable;
import com.threerings.export.ObjectMarshaller;

import static com.threerings.editor.Log.log;

/**
 * Some general utility methods relating to editable properties.
 */
// NOTES:
// - It'd be keen we didn't rely on special values to inherit, but could instead add
//   a String[] inherit attribtue to @Editable, which would be the attribute names to
//   allow for inheritance.
// - Perhaps also much of this can be done with reflection?
// --- RJG 2014-01-24
public class PropertyUtil
{
    /**
     * Finds the mode string by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static String getMode (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            String mode = lineage[ii].getMode();
            if (!Editable.INHERIT_STRING.equals(mode)) {
                return mode;
            }
        }
        return "";
    }

    /**
     * Finds the units string by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static String getUnits (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            String units = lineage[ii].getUnits();
            if (!Editable.INHERIT_STRING.equals(units)) {
                return units;
            }
        }
        return "";
    }

    /**
     * Finds the minimum value by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static double getMinimum (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            double min = lineage[ii].getMinimum();
            if (min != Editable.INHERIT_DOUBLE) {
                return min;
            }
        }
        return -Double.MAX_VALUE;
    }

    /**
     * Finds the maximum value by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static double getMaximum (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            double max = lineage[ii].getMaximum();
            if (max != Editable.INHERIT_DOUBLE) {
                return max;
            }
        }
        return +Double.MAX_VALUE;
    }

    /**
     * Finds the step value by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static double getStep (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            double step = lineage[ii].getStep();
            if (step != Editable.INHERIT_DOUBLE) {
                return step;
            }
        }
        return 1.0;
    }

    /**
     * Finds the scale value by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static double getScale (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            double scale = lineage[ii].getScale();
            if (scale != Editable.INHERIT_DOUBLE) {
                return scale;
            }
        }
        return 1.0;
    }

    /**
     * Finds the minimum size by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static int getMinSize (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            int min = lineage[ii].getMinSize();
            if (min != Editable.INHERIT_INTEGER) {
                return min;
            }
        }
        return 0;
    }

    /**
     * Finds the maximum size by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static int getMaxSize (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            int max = lineage[ii].getMaxSize();
            if (max != Editable.INHERIT_INTEGER) {
                return max;
            }
        }
        return +Integer.MAX_VALUE;
    }

    /**
     * Finds if it's fixed size by walking up the lineage.
     *
     * @param lineage the property lineage, with the actual property at the end.
     */
    public static boolean isFixedSize (Property[] lineage)
    {
        for (int ii = lineage.length - 1; ii >= 0; ii--) {
            if (lineage[ii].isFixedSize()) {
                return true;
            }
        }
        return false;
    }


    /**
     * Get a predicate for filtering raw configs given the specified constraints.
     */
    public static Predicate<ManagedConfig> getRawConfigPredicate (ReferenceConstraints constraints)
    {
        if (constraints == null) {
            return Predicates.alwaysTrue();
        }
        final List<Class<? extends ManagedConfig>> vals = Arrays.asList(constraints.value());
        return new Predicate<ManagedConfig>() {
            public boolean apply (ManagedConfig cfg) {
                if (cfg instanceof DerivedConfig) {
                    cfg = cfg.getInstance((ArgumentMap)null);
                }
                Class<? extends ManagedConfig> clazz = cfg.getClass();
                for (Class<? extends ManagedConfig> listedClass : vals) {
                    if (listedClass.isAssignableFrom(clazz)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Finds all resources referenced by the specified editable object (and any configs referenced
     * by that object, etc.) and places them in the supplied set.
     */
    public static void getResources (ConfigManager cfgmgr, Object object, Set<String> paths)
    {
        getResources(cfgmgr, object, paths, new ReferenceSet());
    }

    /**
     * Similar to DeepUtil.transfer, but operating on properties.
     * Attempt to transfer each property with the same name to the dest object,
     * but recover on any and all errors.
     * This is used in the editor when changing config types.
     */
    public static void transferCompatibleProperties (Object source, Object dest)
    {
        // let NPEs happen.
        Property[] destProps = Introspector.getProperties(dest);
        for (Property srcProp : Introspector.getProperties(source)) {
            String name = srcProp.getName();
            for (Property destProp : destProps) {
                if (name.equals(destProp.getName())) {
                    // require a name match, but then only transfer the value if the
                    // properties are fully compatible
                    if (srcProp.isCompatible(destProp)) {
                        try {
                            destProp.set(dest, srcProp.get(source));
                        } catch (Throwable t) {
                            log.warning("Destination property not compatible after all?", t);
                        }
                    }
                    break;
                }
            }
            // if property not found on dest: ignore
        }
    }

    /**
     * Strips and returns a single object.
     */
    public static Object strip (ConfigManager cfgmgr, Object object)
    {
        if (object == null) {
            return null;
        }
        if (object instanceof Object[]) {
            List<Object> list = Lists.newArrayList();
            Object[] oarray = (Object[])object;
            for (Object element : oarray) {
                if (!isStrippable(element)) {
                    list.add(strip(cfgmgr, element));
                }
            }
            int nsize = list.size();
            return list.toArray(oarray.length == nsize ?
                oarray : (Object[])Array.newInstance(oarray.getClass().getComponentType(), nsize));
        }
        if (object instanceof List) {
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>)object;
            for (int ii = list.size() - 1; ii >= 0; ii--) {
                Object element = list.get(ii);
                if (isStrippable(element)) {
                    list.remove(ii);
                } else {
                    list.set(ii, strip(cfgmgr, element));
                }
            }
            return list;
        }
        if (!(object instanceof Exportable)) {
            return object;
        }
        Class<?> clazz = object.getClass();
        Object prototype = ObjectMarshaller.getObjectMarshaller(clazz).getPrototype();
        for (Property property : Introspector.getProperties(clazz)) {
            Object result = strip(cfgmgr, object, property);
            property.set(object, result == STRIP_OUT ? property.get(prototype) : result);
        }
        return object;
    }

    /**
     * Strips a single property of an object.
     */
    protected static Object strip (ConfigManager cfgmgr, Object object, Property property)
    {
        Object value = property.get(object);
        if (isStrippable(property) || isStrippable(value)) {
            return STRIP_OUT;
        }
        if (value == null) {
            return null;

        } else if (property.getType().equals(ConfigReference.class)) {
            @SuppressWarnings("unchecked") Class<? extends ManagedConfig> cclass =
                (Class<? extends ManagedConfig>)property.getArgumentType(ConfigReference.class);
            @SuppressWarnings("unchecked") ConfigReference<ManagedConfig> ref =
                (ConfigReference<ManagedConfig>)value;
            if (object instanceof DerivedConfig) {
                cclass = ((DerivedConfig)object).cclass;
            }
            ArgumentMap args = ref.getArguments();
            if (args.isEmpty()) {
                return ref;
            }
            ManagedConfig config = cfgmgr.getConfig(cclass, ref.getName());
            if (!(config instanceof ParameterizedConfig)) {
                args.clear();
                return ref;
            }
            ParameterizedConfig pconfig = (ParameterizedConfig)config;
            for (Iterator<Map.Entry<String, Object>> it = args.entrySet().iterator();
                    it.hasNext(); ) {
                Map.Entry<String, Object> entry = it.next();
                Parameter param = pconfig.getParameter(entry.getKey());
                if (param == null) {
                    it.remove();
                    continue;
                }
                Property prop = param.getArgumentProperty(pconfig);
                if (prop == null) {
                    it.remove();
                    continue;
                }
                Object result = strip(cfgmgr, args, prop);
                if (result == STRIP_OUT) {
                    it.remove();
                } else {
                    entry.setValue(result);
                }
            }
            return ref;

        } else {
            return strip(cfgmgr, value);
        }
    }

    /**
     * Checks whether the specified property is strippable.
     */
    protected static boolean isStrippable (Property property)
    {
        Class<?> type = property.getType();
        return property.isAnnotationPresent(Strippable.class) ||
            isStrippable(type) || isStrippable(property.getComponentType()) ||
            ConfigReference.class.isAssignableFrom(type) &&
                isStrippable(property.getArgumentType(ConfigReference.class));
    }

    /**
     * Checks whether the specified object itself is strippable.
     */
    protected static boolean isStrippable (Object object)
    {
        return object != null && isStrippable(object.getClass());
    }

    /**
     * Checks whether the specified class or its component type or any of its superclasses are
     * flagged as strippable.
     */
    protected static boolean isStrippable (Class<?> clazz)
    {
        return clazz != null && (clazz.isAnnotationPresent(Strippable.class) ||
            isStrippable(clazz.getComponentType()) || isStrippable(clazz.getSuperclass()));
    }

    /**
     * Finds all resources referenced by the specified editable object (and any configs referenced
     * by that object, etc.) and places them in the supplied set.
     */
    protected static void getResources (
        ConfigManager cfgmgr, Object object, Set<String> paths, ReferenceSet refs)
    {
        if (object == null) {
            return;
        }
        if (object instanceof Object[]) {
            for (Object element : (Object[])object) {
                getResources(cfgmgr, element, paths, refs);
            }
            return;
        }
        if (object instanceof List) {
            List<?> list = (List<?>)object;
            for (int ii = 0, nn = list.size(); ii < nn; ii++) {
                getResources(cfgmgr, list.get(ii), paths, refs);
            }
            return;
        }
        for (Property property : Introspector.getProperties(object)) {
            Object value = property.get(object);
            if (value == null) {
                continue;
            }
            Editable annotation = property.getAnnotation();
            Reference refAnno = property.getAnnotation(Reference.class);
            String editor = annotation.editor();
            boolean cfg = editor.equals("config") || (refAnno != null);
            if (cfg || property.getType().equals(ConfigReference.class)) {
                Class<ManagedConfig> cclass;
                ConfigReference<ManagedConfig> ref;
                if (cfg) {
                    ConfigGroup<?> group = (refAnno != null)
                            ? cfgmgr.getGroup(refAnno.value()) // new way
                            : cfgmgr.getGroup(annotation.mode()); // old way
                    if (group == null) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Class<ManagedConfig> mclass = (Class<ManagedConfig>)group.getConfigClass();
                    cclass = mclass;
                    ref = new ConfigReference<ManagedConfig>((String)value);
                } else {
                    @SuppressWarnings("unchecked") Class<ManagedConfig> mclass =
                        (Class<ManagedConfig>)property.getArgumentType(ConfigReference.class);
                    @SuppressWarnings("unchecked") ConfigReference<ManagedConfig> mref =
                        (ConfigReference<ManagedConfig>)value;
                    cclass = mclass;
                    ref = mref;
                }
                if (!refs.add(cclass, ref)) {
                    continue;
                }
                if (cfgmgr.isResourceClass(cclass)) {
                    paths.add(ref.getName());
                }
                ManagedConfig config = cfgmgr.getConfig(cclass, ref);
                if (config != null) {
                    getResources(config.getConfigManager(), config, paths, refs);
                }
            } else if (editor.equals("resource")) {
                paths.add((String)value);

            } else {
                getResources(cfgmgr, value, paths, refs);
            }
        }
    }

    /**
     * Tracks which references we've seen.
     */
    protected static class ReferenceSet
    {
        /**
         * Return true if this is a newly-seen reference.
         */
        public boolean add (Class<? extends ManagedConfig> clazz, ConfigReference<?> ref)
        {
            return _set.put(clazz, ref);
        }

        /** The gathered references. */
        protected final SetMultimap<Class<? extends ManagedConfig>, ConfigReference<?>> _set =
                HashMultimap.create();
    }

    /** Signifies that a property should be stripped out completely. */
    protected static final Object STRIP_OUT = new Object();
}
