//
// $Id$

package com.threerings.config;

import java.util.EventObject;

/**
 * Contains information about a configuration added, removed, or updated.
 */
public class ConfigEvent<T extends ManagedConfig> extends EventObject
{
    /**
     * Creates a new config event.
     */
    public ConfigEvent (Object source, T config)
    {
        super(source);
        _config = config;
    }

    /**
     * Returns a reference to the affected configuration.
     */
    public T getConfig ()
    {
        return _config;
    }

    /** The affected configuration. */
    protected T _config;
}
