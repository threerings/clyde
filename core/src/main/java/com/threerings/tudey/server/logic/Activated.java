//
// $Id$

package com.threerings.tudey.server.logic;

/**
 * An interface for logics that have an activator.
 */
public interface Activated
{
    /**
     * Returns the logic that activated this logic.
     */
    public Logic getActivator();
}
