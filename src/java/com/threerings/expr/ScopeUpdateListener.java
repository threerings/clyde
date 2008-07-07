//
// $Id$

package com.threerings.expr;

/**
 * Used to notify objects when the scope has been updated.
 */
public interface ScopeUpdateListener
{
    /**
     * Called when the scope has been updated.
     */
    public void scopeUpdated (ScopeEvent event);
}
