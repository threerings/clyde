//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.tudey.util;

import com.samskivert.util.Config;

import com.threerings.math.FloatMath;
import com.threerings.math.Frustum;
import com.threerings.math.Quaternion;
import com.threerings.math.Rect;
import com.threerings.math.SphereCoords;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;

import com.threerings.opengl.gui.util.Rectangle;

import com.threerings.tudey.config.CameraConfig;

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

    /**
     * Returns a reference to the default camera config.
     */
    public static CameraConfig getDefaultCameraConfig ()
    {
        return _defaultCameraConfig;
    }

    /**
     * Returns the default local interest region (as derived from the camera parameters).
     */
    public static Rect getDefaultLocalInterest ()
    {
        return _defaultLocalInterest;
    }

    /**
     * Returns the local area of interest corresponding to the given camera parameters.
     */
    public static Rect getLocalInterest (CameraConfig config, float aspect)
    {
        // create and initialize a frustum
        Frustum frustum = new Frustum().setToPerspective(
            config.fov, aspect, config.near, config.far);

        // transform based on sphere coordinates
        Transform3D xform = new Transform3D(Transform3D.UNIFORM);
        SphereCoords coords = config.coords;
        float ce = FloatMath.cos(coords.elevation);
        xform.getTranslation().set(
            FloatMath.sin(coords.azimuth) * ce,
            -FloatMath.cos(coords.azimuth) * ce,
            FloatMath.sin(coords.elevation)).multLocal(coords.distance).addLocal(config.offset);
        xform.getRotation().fromAnglesXZ(
            FloatMath.HALF_PI - coords.elevation, coords.azimuth);
        frustum.transformLocal(xform);

        // start with an empty rect
        Rect interest = new Rect();

        // add projected frustum coordinates
        for (Vector3f vertex : frustum.getVertices()) {
            interest.addLocal(new Vector2f(vertex.x, vertex.y));
        }

        // expand by fixed amount and return
        return interest.expandLocal(_interestExpansion, _interestExpansion);
    }

    /** The number of world units per tile elevation unit. */
    protected static float _elevationScale;

    /** The default camera config. */
    protected static CameraConfig _defaultCameraConfig = new CameraConfig();

    /** The fixed amount by which to expand the area of interest. */
    protected static float _interestExpansion;

    /** The local interest region corresponding to the default camera parameters. */
    protected static Rect _defaultLocalInterest;

    static {
        // load the fields from the configuration
        Config config = new Config("/rsrc/config/tudey/scene");
        _elevationScale = config.getValue("elevation_scale", 0.5f);
        _defaultCameraConfig.priority = Integer.MIN_VALUE;
        _defaultCameraConfig.fov = FloatMath.toRadians(config.getValue("camera_fov", 60f));
        _defaultCameraConfig.near = config.getValue("camera_near", 1f);
        _defaultCameraConfig.far = config.getValue("camera_far", 100f);
        _defaultCameraConfig.coords.set(
            FloatMath.toRadians(config.getValue("camera_azimuth", 0f)),
            FloatMath.toRadians(config.getValue("camera_elevation", 45f)),
            config.getValue("camera_distance", 10f));
        _defaultCameraConfig.offset.set(
            config.getValue("camera_offset", new float[] { 0f, 0f, 1f }));
        _interestExpansion = config.getValue("interest_expansion", 5f);
        _defaultLocalInterest = getLocalInterest(_defaultCameraConfig, 4f / 3f);
    }
}
