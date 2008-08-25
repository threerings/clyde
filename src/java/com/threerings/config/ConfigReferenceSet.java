//
// $Id$

package com.threerings.config;

import java.util.HashSet;

import com.samskivert.util.Tuple;

/**
 * A set of config references of different types.
 */
public class ConfigReferenceSet extends HashSet<Tuple<Class, ConfigReference>>
{
    /**
     * Adds a reference to the set.
     */
    public <T extends ManagedConfig> boolean add (Class<T> clazz, String name)
    {
        return name != null && add(clazz, new ConfigReference<T>(name));
    }

    /**
     * Adds a reference to the set.
     */
    public <T extends ManagedConfig> boolean add (Class<T> clazz, ConfigReference<T> ref)
    {
        return ref != null && add(new Tuple<Class, ConfigReference>(clazz, ref));
    }
}
