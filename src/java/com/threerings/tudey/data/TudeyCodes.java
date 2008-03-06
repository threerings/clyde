//
// $Id$

package com.threerings.tudey.data;

/**
 * Various constants.
 */
public interface TudeyCodes
{
    /** The message identifier for scene update messages. */
    public static final String SCENE_UPDATE = "scene_update";

    /** The delay between when we receive an update from the server and when we render it
     * (used to ensure that we are always interpolating between two valid states). */
    public static final long INTERPOLATION_DELAY = 100L;

    /** The server delays actions by this long to account for buffering. */
    public static final long BUFFER_INTERVAL = 150L;

    /** Our maximum expected one-way latency. */
    public static final long MAX_LATENCY = 500L;

    /** The Y+ direction. */
    public static final int NORTH = 0;

    /** The X- direction. */
    public static final int WEST = 1;

    /** The Y- direction. */
    public static final int SOUTH = 2;

    /** The X+ direction. */
    public static final int EAST = 3;

    /** The four cardinal directions. */
    public static final int[] DIRECTIONS = { NORTH, WEST, SOUTH, EAST };
}
