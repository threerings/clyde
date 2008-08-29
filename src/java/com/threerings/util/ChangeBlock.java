//
// $Id$

package com.threerings.util;

/**
 * A simple mechanism for preventing infinite recursion when one change listener responds to an
 * event by making another change, which in turns notifies another (or the same) listener, which
 * makes another change, and so on.  The following example demonstrates the pattern:
 *
 * <p><code>
 * public void stateChanged (ChangeEvent event)
 * {
 *     if (!_block.enter()) {
 *         return;
 *     }
 *     try {
 *         // do something that may cause another state change
 *     } finally {
 *         _block.leave();
 *     }
 * }
 * </code>
 */
public class ChangeBlock
{
    /**
     * Attempts to enter the change block.
     *
     * @return true if the change block was successfully entered, false if already in the change
     * block.
     */
    public boolean enter ()
    {
        return _changing ? false : (_changing = true);
    }

    /**
     * Leaves the change block.
     */
    public void leave ()
    {
        _changing = false;
    }

    /** If true, the current thread is in the change block. */
    protected boolean _changing;
}
