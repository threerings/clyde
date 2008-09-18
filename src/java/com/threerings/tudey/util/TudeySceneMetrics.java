//
// $Id$

package com.threerings.tudey.util;

import com.samskivert.util.Config;

import com.threerings.math.Quaternion;

/**
 * Contains static methods relating to the Tudey coordinate system.
 */
public class TudeySceneMetrics
{
    /** Rotations corresponding to each direction. */
    public static final Quaternion[] ROTATIONS = {
        new Quaternion(0f, 0f, 0f, 1f),
        new Quaternion(0f, 0f, 0.707106781f, 0.707106781f),
        new Quaternion(0f, 0f, 1f, 0f),
        new Quaternion(0f, 0f, 0.707106781f, -0.707106781f) };

    /**
     * Returns the world space z coordinate corresponding to the given tile elevation.
     */
    public static float getZ (int elevation)
    {
        return elevation * _elevationScale;
    }

    /**
     * Returns the (closest) tile elevation corresponding to the given world space z coordinate.
     */
    public static int getElevation (float z)
    {
        return Math.round(z / _elevationScale);
    }

    /** The number of world units per tile elevation unit. */
    protected static float _elevationScale;

    static {
        // load the fields from the configuration
        Config config = new Config("/rsrc/config/tudey/scene");
        _elevationScale = config.getValue("elevation_scale", 0.5f);
    }
}
