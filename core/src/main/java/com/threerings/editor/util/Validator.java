//
// $Id$

package com.threerings.editor.util;

import java.io.PrintStream;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
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
import com.threerings.config.util.ConfigId;;

import com.threerings.editor.Editable;
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
        this("", out);
    }

    /**
     * Create a new validator with the specified "where".
     */
    public Validator (String where, PrintStream out)
    {
        _where = where;
        _out = out;
    }

    /**
     * Get the current "where" value.
     */
    public String getWhere ()
    {
        return _where;
    }

    /**
     * Get the printstream to which we show errors.
     */
    public PrintStream getPrintStream ()
    {
        return _out;
    }

    /**
     * Push an identifier onto the "where" string.
     * This should be immediately followed by a try block that pops in its finally.
     */
    public void pushWhere (String where)
    {
        _wheres.push(_where); // save the old one
        _where += where;
    }

    /**
     * Pop a "where" identifier off.
     */
    public void popWhere ()
    {
        _where = _wheres.pop();
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
    }

    /**
     * Hook for a post-validate step.
     * Clear any internal data structures.
     */
    protected void postValidate ()
    {
        _configs.clear();
        _resources.clear();
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
        Object value = property.get(object);
        if (value == null) {
            return;
        }
        Editable annotation = property.getAnnotation();
        String editor = annotation.editor();
        if (editor.equals("resource")) {
            _resources.add((String)value);

        } else if (editor.equals("config")) {
            @SuppressWarnings("unchecked")
            ConfigGroup<ManagedConfig> group =
                    (ConfigGroup<ManagedConfig>)cfgmgr.getGroup(annotation.mode());
            if (group != null) {
                _configs.add(new ConfigId(group.getConfigClass(), (String)value));
            }

        } else if (property.getType().equals(ConfigReference.class)) {
            Class<?> clazz = property.getArgumentType(ConfigReference.class);
            if (clazz == null && (object instanceof DerivedConfig)) {
                clazz = ((DerivedConfig)object).cclass;
            }
            @SuppressWarnings("unchecked") Class<ManagedConfig> cclass =
                (Class<ManagedConfig>)clazz;
            @SuppressWarnings("unchecked") ConfigReference<ManagedConfig> ref =
                (ConfigReference<ManagedConfig>)value;
            _configs.add(new ConfigId(cclass, ref.getName()));
            ArgumentMap args = ref.getArguments();
            if (args.isEmpty()) {
                return;
            }
            ManagedConfig config = cfgmgr.getRawConfig(cclass, ref.getName());
            if (!(config instanceof ParameterizedConfig)) {
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

        } else {
            getReferences(cfgmgr, value);
        }
    }

    /**
     * Validate the internal data structures against the supplied config manager.
     */
    protected boolean validate (ConfigManager cfgmgr)
    {
        boolean result = true;
        for (ConfigId configId : _configs) {
            if (cfgmgr.getConfig(configId.clazz, configId.name) == null) {
                output("references missing config of type " +
                        ConfigGroup.getName(configId.clazz) + ": " + configId.name);
                result = false;
            }
        }
        ResourceManager rsrcmgr = cfgmgr.getResourceManager();
        for (String resource : _resources) {
            if (!rsrcmgr.getResourceFile(resource).exists()) {
                output("references missing resource: " + resource);
                result = false;
            }
        }
        return result;
    }

    /**
     * Output a validation error message.
     */
    protected void output (String message)
    {
        _out.println(_where + " " + message);
    }

    /** Our output stream. */
    protected final PrintStream _out;

    /** The current location context. */
    protected String _where;

    /** The stack of wheres. */
    protected Deque<String> _wheres = new ArrayDeque<String>();

    /** The gathered resources while validating the current object. */
    protected List<String> _resources = Lists.newArrayList();

    /** The gathered configs while validating the current object. */
    protected Set<ConfigId> _configs = Sets.newHashSet();
}
