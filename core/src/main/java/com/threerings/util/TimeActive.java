package com.threerings.util;

/**
 * Arbitrates whether something is active or not and provides a way to get the next transition time.
 */
public interface TimeActive
{
    /**
     * Is the specified time part of the active time?
     */
    boolean isActive (long now);

    /**
     * Get the next "transition time"- the next start or stop time after the specified stamp,
     * or null if there are no upcoming transitions.
     */
    Long getNextTime (long now);
}
