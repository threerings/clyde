//
// $Id$

package com.threerings.config;

/**
 * Used to notify objects when managed configurations have been added to or removed from a group.
 */
public interface ConfigGroupListener<T extends ManagedConfig>
{
    /**
     * Called when a configuration has been added to the group.
     */
    public void configAdded (ConfigEvent<T> event);

    /**
     * Called when a configuration has been removed from the group.
     */
    public void configRemoved (ConfigEvent<T> event);
}
