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

package com.threerings.opengl.camera;

import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;
import com.threerings.math.SphereCoords;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.util.GlContext;

/**
 * Swings the camera around a target position.
 */
public class OrbitCameraHandler extends CameraHandler
{
    /**
     * Creates a new orbit camera handler for the compositor camera.
     */
    public OrbitCameraHandler (GlContext ctx)
    {
        super(ctx);
    }

    /**
     * Creates a new orbit camera handler for the specified camera.
     */
    public OrbitCameraHandler (GlContext ctx, Camera camera, boolean matchRenderSurface)
    {
        super(ctx, camera, matchRenderSurface);
    }

    /**
     * Returns a reference to the location of the camera's target.
     */
    public Vector3f getTarget ()
    {
        return _target;
    }

    /**
     * Returns a reference to the camera's coordinates relative to the target.
     */
    public SphereCoords getCoords ()
    {
        return _coords;
    }

    /**
     * Adjusts the azimuth and elevation of the camera by the specified amounts.
     */
    public void orbit (float azimuth, float elevation)
    {
        _coords.azimuth += azimuth;
        _coords.elevation = FloatMath.clamp(
            _coords.elevation + elevation, _minElevation, _maxElevation);
    }

    /**
     * "Zooms" in (if negative) or out (if positive) by the specified amount.
     */
    public void zoom (float distance)
    {
        _coords.distance = FloatMath.clamp(
            _coords.distance + distance, _minDistance, _maxDistance);
    }

    /**
     * Pans the target by the specified amounts relative to the camera's current orientation.
     */
    public void pan (float x, float y)
    {
        Quaternion rot = _camera.getWorldTransform().getRotation();
        _target.addLocal(rot.transformLocal(_pan.set(x, y, 0f)));
    }

    /**
     * Pans the target on the XY plane by the specified amounts relative to the camera's current
     * orientation.
     */
    public void panXY (float x, float y)
    {
        float cosa = FloatMath.cos(_coords.azimuth);
        float sina = FloatMath.sin(_coords.azimuth);
        _target.addLocal(x*cosa - y*sina, x*sina + y*cosa, 0f);
    }

    /**
     * Sets the limits on the camera's relative coordinates.
     */
    public void setCoordLimits (
        float minElevation, float maxElevation, float minDistance, float maxDistance)
    {
        _minElevation = minElevation;
        _maxElevation = maxElevation;
        _minDistance = minDistance;
        _maxDistance = maxDistance;
    }

    @Override
    public Vector3f getViewerTranslation ()
    {
        return _target;
    }

    @Override
    protected void getTransform (Transform3D transform)
    {
        float ce = FloatMath.cos(_coords.elevation);
        transform.getTranslation().set(
            FloatMath.sin(_coords.azimuth) * ce,
            -FloatMath.cos(_coords.azimuth) * ce,
            FloatMath.sin(_coords.elevation)).multLocal(_coords.distance).addLocal(_target);
        transform.getRotation().fromAnglesXZ(
            FloatMath.HALF_PI - _coords.elevation, _coords.azimuth);
    }

    /** The location that the camera is looking at. */
    protected Vector3f _target = new Vector3f();

    /** The coordinates relative to the target. */
    protected SphereCoords _coords = new SphereCoords(0f, FloatMath.PI / 4f, 10f);

    /** The minimum and maximum elevations. */
    protected float _minElevation = FloatMath.PI / 16f, _maxElevation = FloatMath.HALF_PI;

    /** The minimum and maximum distances. */
    protected float _minDistance = 2f, _maxDistance = 50f;

    /** A temporary vector for pan calculations. */
    protected Vector3f _pan = new Vector3f();
}
