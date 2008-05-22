//
// $Id$

package com.threerings.config;

import com.samskivert.util.StringUtil;

import com.threerings.export.Exportable;

/**
 * Represents a configuration managed by the {@link ConfigManager}.
 */
public abstract class ManagedConfig
    implements Exportable
{
    /**
     * Returns the name of this configuration.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Returns the unique identifier of this configuration.
     */
    public int getId ()
    {
        return _id;
    }

    @Override // documentation inherited
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /**
     * Initializes the configuration with its name and identifier.
     */
    protected void init (String name, int id)
    {
        _name = name;
        _id = id;
    }

    /** The name of this configuration. */
    protected String _name;

    /** The unique identifier of this configuration. */
    protected int _id;
}
