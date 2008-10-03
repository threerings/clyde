//
// $Id$

package com.threerings.tudey.util;

import com.samskivert.util.Config;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;

import com.threerings.opengl.gui.util.Rectangle;

/**
 * Contains static methods relating to the Tudey coordinate system.
 */
public class TudeySceneMetrics
{
    /** Tile rotations corresponding to each direction. */
    public static final Quaternion[] TILE_ROTATIONS = {
        new Quaternion(0f, 0f, 0f, 1f),
        new Quaternion(0f, 0f, 0.707106781f, 0.707106781f),
        new Quaternion(0f, 0f, 1f, 0f),
        new Quaternion(0f, 0f, 0.707106781f, -0.707106781f) };

    /**
     * Gets the transform of a tile with the specified dimensions at the supplied coordinates
     * with the given rotation.
     *
     * @param width the untransformed width of the tile.
     * @param height the untransformed height of the tile.
     */
    public static void getTileTransform (
        int width, int height, int x, int y, int elevation, int rotation, Transform3D result)
    {
        // adjust for rotation
        switch (rotation) {
            case 1:
                x += height;
                break;
            case 2:
                x += width;
                y += height;
                break;
            case 3:
                y += width;
                break;
        }
        result.setType(Transform3D.RIGID);
        result.getRotation().set(TILE_ROTATIONS[rotation]);
        result.getTranslation().set(x, y, getTileZ(elevation));
    }

    /**
     * Finds the region covered by a tile with the specified dimensions at the supplied coordinates
     * with the given rotation.
     */
    public static void getTileRegion (
        int width, int height, int x, int y, int rotation, Rectangle result)
    {
        result.set(
            x, y,
            getTileWidth(width, height, rotation),
            getTileHeight(width, height, rotation));
    }

    /**
     * Returns the width of a tile with the specified dimensions under the supplied rotation.
     */
    public static int getTileWidth (int width, int height, int rotation)
    {
        return (rotation == 0 || rotation == 2) ? width : height;
    }

    /**
     * Returns the height of a tile with the specified dimensions under the supplied rotation.
     */
    public static int getTileHeight (int width, int height, int rotation)
    {
        return (rotation == 0 || rotation == 2) ? height : width;
    }

    /**
     * Returns the world space z coordinate corresponding to the given tile elevation.
     */
    public static float getTileZ (int elevation)
    {
        return elevation * _elevationScale;
    }

    /**
     * Returns the (closest) tile elevation corresponding to the given world space z coordinate.
     */
    public static int getTileElevation (float z)
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
