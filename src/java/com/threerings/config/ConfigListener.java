//
// $Id$

package com.threerings.config;

/**
 * Used to notify objects when managed configurations have been added, removed, or updated.
 */
public interface ConfigListener<T extends ManagedConfig>
{
    /**
     * Called when a new configuration has been added.
     */
    public void configAdded (ConfigEvent<T> event);

    /**
     * Called when a configuration has been removed.
     */
    public void configRemoved (ConfigEvent<T> event);

    /**
     * Called when a configuration has been updated.
     */
    public void configUpdated (ConfigEvent<T> event);
}
