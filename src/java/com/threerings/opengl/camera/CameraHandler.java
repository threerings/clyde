//
// $Id$

package com.threerings.opengl.camera;

import com.threerings.math.FloatMath;

import com.threerings.opengl.renderer.Camera;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.util.Rectangle;

/**
 * Controls the camera parameters.
 */
public abstract class CameraHandler
{
    public CameraHandler (GlContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Sets the camera's perspective parameters.
     */
    public void setPerspective (float fovy, float near, float far)
    {
        _fovy = fovy;
        _near = near;
        _far = far;
        updatePerspective();
    }

    /**
     * Returns the camera's field of view in radians.
     */
    public float getFieldOfView ()
    {
        return _fovy;
    }

    /**
     * Returns the distance to the near clip plane.
     */
    public float getNear ()
    {
        return _near;
    }

    /**
     * Returns the distance to the far clip plane.
     */
    public float getFar ()
    {
        return _far;
    }

    /**
     * Updates the camera perspective parameters.
     */
    public void updatePerspective ()
    {
        Camera camera = _ctx.getRenderer().getCamera();
        Rectangle viewport = camera.getViewport();
        camera.setPerspective(_fovy, (float)viewport.width / viewport.height, _near, _far);
    }

    /**
     * Updates the camera position.
     */
    public abstract void updatePosition ();

    /** The renderer context. */
    protected GlContext _ctx;

    /** The vertical field of view (in radians). */
    protected float _fovy = FloatMath.PI / 3f;

    /** The distance to the near clip plane. */
    protected float _near = 1f;

    /** The distance to the far clip plane. */
    protected float _far = 100f;
}
