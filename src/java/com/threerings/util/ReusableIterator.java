//
// $Id$

package com.threerings.util;

import java.util.Iterator;

/**
 * An interface for iterators that can be reused in order to avoid creating a new object for
 * each pass.  Calling the {@link #reset} method reinitializes the iterator.
 */
public interface ReusableIterator<E> extends Iterator<E>
{
    /**
     * Resets this iterator, preparing it for another iteration.
     */
    public void reset (); 
}
