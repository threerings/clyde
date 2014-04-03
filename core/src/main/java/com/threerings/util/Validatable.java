//
// $Id$

package com.threerings.util;

/**
 * An interface for types that can know when they are in a valid or invalid state.
 */
public interface Validatable
{
    /**
     * Is this object in a valid state?
     */
    public boolean isValid ();
}
