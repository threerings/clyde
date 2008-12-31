//
// $Id$

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

    @Override // documentation inherited
    public void updatePosition ()
    {
        // update the camera translation and rotation
        Transform3D xform = _camera.getWorldTransform();
        float ce = FloatMath.cos(_coords.elevation);
        xform.getTranslation().set(
            FloatMath.sin(_coords.azimuth) * ce,
            -FloatMath.cos(_coords.azimuth) * ce,
            FloatMath.sin(_coords.elevation)).multLocal(_coords.distance).addLocal(_target);
        xform.getRotation().fromAnglesXZ(
            FloatMath.HALF_PI - _coords.elevation, _coords.azimuth);

        // update the camera transform
        _camera.updateTransform();
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
