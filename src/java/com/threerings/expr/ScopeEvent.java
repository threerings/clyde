//
// $Id$

package com.threerings.expr;

import java.util.EventObject;

/**
 * Contains information about an updated scope.
 */
public class ScopeEvent extends EventObject
{
    /**
     * Creates a new scope event.
     */
    public ScopeEvent (Object source)
    {
        super(source);
    }
}
