//
// $Id$

package com.threerings.expr;

/**
 * Provides access to contextual variables.
 */
public interface ExpressionContext
{
    /**
     * Returns the floating-point variable with the given name.
     */
    public MutableFloat getFloatVariable (String name);
    
    /**
     * Returns the start time of the specified scope.
     */
    public long getStartTime (String scope);
}
