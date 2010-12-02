//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import com.samskivert.util.Tuple;

import com.threerings.resource.ResourceManager;

import com.threerings.config.ArgumentMap;
import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ManagedConfig;
import com.threerings.config.Parameter;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.Introspector;
import com.threerings.editor.Property;

/**
 * Some general utility methods relating to editable properties.
 */
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
     * Finds all configs and resources referenced in the supplied editable object and places them
     * in the supplied sets.
     */
    public static void getReferences (
        ConfigManager cfgmgr, Object object,
        Set<Tuple<Class, String>> configs, Set<String> resources)
    {
        if (object == null) {
            return;
        }
        if (object instanceof Object[]) {
            for (Object element : (Object[])object) {
                getReferences(cfgmgr, element, configs, resources);
            }
            return;
        }
        if (object instanceof List) {
            List list = (List)object;
            for (int ii = 0, nn = list.size(); ii < nn; ii++) {
                getReferences(cfgmgr, list.get(ii), configs, resources);
            }
            return;
        }
        for (Property property : Introspector.getProperties(object)) {
            getReferences(cfgmgr, object, property, configs, resources);
        }
    }

    /**
     * Valides the supplied sets of configs and resources.
     *
     * @return true if the references are valid
     */
    public static boolean validateReferences (
        String where, ConfigManager cfgmgr, Set<Tuple<Class, String>> configs,
        Set<String> resources, PrintStream out)
    {
        boolean result = true;
        for (Tuple<Class, String> tuple : configs) {
            @SuppressWarnings("unchecked") Class<ManagedConfig> cclass =
                (Class<ManagedConfig>)tuple.left;
            if (cfgmgr.getConfig(cclass, tuple.right) == null) {
                out.println(where + " references missing config of type " +
                    ConfigGroup.getName(cclass) + ": " + tuple.right);
                result = false;
            }
        }
        ResourceManager rsrcmgr = cfgmgr.getResourceManager();
        for (String resource : resources) {
            if (!rsrcmgr.getResourceFile(resource).exists()) {
                out.println(where + " references missing resource: " + resource);
                result = false;
            }
        }
        return result;
    }

    /**
     * Finds all resources referenced by the specified editable object (and any configs referenced
     * by that object, etc.) and places them in the supplied set.
     */
    public static void getResources (ConfigManager cfgmgr, Object object, Set<String> paths)
    {
        getResources(cfgmgr, object, paths, new ConfigReferenceSet());
    }

    /**
     * Finds all configs and resources referenced by the supplied property of the supplied object
     * and places them in the given sets.
     */
    protected static void getReferences (
        ConfigManager cfgmgr, Object object, Property property,
        Set<Tuple<Class, String>> configs, Set<String> resources)
    {
        Object value = property.get(object);
        if (value == null) {
            return;
        }
        Editable annotation = property.getAnnotation();
        String editor = annotation.editor();
        if (editor.equals("resource")) {
            resources.add((String)value);

        } else if (editor.equals("config")) {
            ConfigGroup group = cfgmgr.getGroup(annotation.mode());
            if (group != null) {
                configs.add(new Tuple<Class, String>(group.getConfigClass(), (String)value));
            }
        } else if (property.getType().equals(ConfigReference.class)) {
            @SuppressWarnings("unchecked") Class<ManagedConfig> cclass =
                (Class<ManagedConfig>)property.getArgumentType(ConfigReference.class);
            @SuppressWarnings("unchecked") ConfigReference<ManagedConfig> ref =
                (ConfigReference<ManagedConfig>)value;
            configs.add(new Tuple<Class, String>(cclass, ref.getName()));
            ArgumentMap args = ref.getArguments();
            if (args.isEmpty()) {
                return;
            }
            ManagedConfig config = cfgmgr.getConfig(cclass, ref.getName());
            if (!(config instanceof ParameterizedConfig)) {
                return;
            }
            ParameterizedConfig pconfig = (ParameterizedConfig)config;
            for (Map.Entry<String, Object> entry : args.entrySet()) {
                Parameter param = pconfig.getParameter(entry.getKey());
                if (param == null) {
                    continue;
                }
                Property prop = param.getArgumentProperty(pconfig);
                if (prop != null) {
                    getReferences(cfgmgr, args, prop, configs, resources);
                }
            }
        } else {
            getReferences(cfgmgr, value, configs, resources);
        }
    }

    /**
     * Finds all resources referenced by the specified editable object (and any configs referenced
     * by that object, etc.) and places them in the supplied set.
     */
    protected static void getResources (
        ConfigManager cfgmgr, Object object, Set<String> paths, ConfigReferenceSet refs)
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
            List list = (List)object;
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
            String editor = annotation.editor();
            boolean cfg = editor.equals("config");
            if (cfg || property.getType().equals(ConfigReference.class)) {
                Class<ManagedConfig> cclass;
                ConfigReference<ManagedConfig> ref;
                if (cfg) {
                    ConfigGroup group = cfgmgr.getGroup(annotation.mode());
                    if (group == null) {
                        continue;
                    }
                    @SuppressWarnings("unchecked") Class<ManagedConfig> mclass =
                        (Class<ManagedConfig>)group.getConfigClass();
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
                    getResources(cfgmgr, config, paths, refs);
                }
            } else if (editor.equals("resource")) {
                paths.add((String)value);

            } else {
                getResources(cfgmgr, value, paths, refs);
            }
        }
    }
}

