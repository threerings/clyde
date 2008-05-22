//
// $Id$

package com.threerings.config;

/**
 * Provides a default implementation of {@link ConfigListener} for use as a base class.
 */
public class ConfigAdapter<T extends ManagedConfig>
    implements ConfigListener<T>
{
    // documentation inherited from interface ConfigListener
    public void configAdded (ConfigEvent<T> event)
    {
        // nothing by default
    }

    // documentation inherited from interface ConfigListener
    public void configRemoved (ConfigEvent<T> event)
    {
        // nothing by default
    }

    // documentation inherited from interface ConfigListener
    public void configUpdated (ConfigEvent<T> event)
    {
        // nothing by default
    }
}
