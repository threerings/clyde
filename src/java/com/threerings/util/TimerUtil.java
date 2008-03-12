//
// $Id$

package com.threerings.util;

import com.threerings.media.timer.MediaTimer;
import com.threerings.media.timer.MillisTimer;
import com.threerings.media.timer.NanoTimer;
import com.threerings.media.timer.PerfTimer;

/**
 * Utility methods relating to timers.
 */
public class TimerUtil
{
    /**
     * Creates and returns a new timer object.
     */
    public static MediaTimer createTimer ()
    {
        // first try the nano timer
        try {
            return new NanoTimer();
        } catch (Throwable t) { }

        // then the performance timer
        try {
            return new PerfTimer();
        } catch (Throwable t) { }

        // finally, fall back on the millisecond timer
        return new MillisTimer();
    }
}
