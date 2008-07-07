//
// $Id$

package com.threerings.expr;

/**
 * A general-purpose function object.
 */
public interface Function
{
    /** A function that does nothing and returns <code>null</code>. */
    public static final Function NOOP = new Function() {
        public Object call (Object... args) {
            return null;
        }
    };
    
    /**
     * Calls the function.
     */
    public Object call (Object... args);
}
