//
// $Id$

package com.threerings.util;

/**
 * Provides a means of creating "inner" classes without requiring that the class definitions be
 * lexically enclosed within the definitions of the outer class.  The first parameter of each
 * of the inner class's constructors should be a reference to the outer class.
 */
public interface Inner
{
    /**
     * Returns a reference to the outer object.
     */
    public Object getOuter ();
}
