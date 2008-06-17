//
// $Id$

package com.threerings.config;

/**
 * Used to notify objects when managed configurations have been updated.
 */
public interface ConfigUpdateListener<T extends ManagedConfig>
{
    /**
     * Called when a configuration has been updated.
     */
    public void configUpdated (ConfigEvent<T> event);
}
