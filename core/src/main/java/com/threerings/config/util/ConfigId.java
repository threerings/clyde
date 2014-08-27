//
// $Id$

package com.threerings.config.util;

import com.google.common.base.Preconditions;

import com.threerings.config.ManagedConfig;

/**
 * Identifies a config by type and name, rather than using a janky-ass tuple or something.
 */
public class ConfigId
{
    /** The type of the referenced config. */
    public final Class<? extends ManagedConfig> clazz;

    /** The name of the referenced config. */
    public final String name;

    /** Constructor. */
    public ConfigId (Class<? extends ManagedConfig> clazz, String name)
    {
        this.clazz = Preconditions.checkNotNull(clazz);
        this.name = Preconditions.checkNotNull(name);
    }

    @Override
    public int hashCode ()
    {
        return clazz.hashCode() * 31 + name.hashCode();
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof ConfigId)) {
            return false;
        }
        ConfigId that = (ConfigId)other;
        return (this.clazz == that.clazz) && this.name.equals(that.name);
    }

    @Override
    public String toString ()
    {
        return "ConfigId[" + clazz + ", " + name + "]";
    }
}
