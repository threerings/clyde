//
// $Id$

package com.threerings.opengl.camera;

import com.threerings.math.FloatMath;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.util.Rectangle;

/**
 * Controls the camera parameters.
 */
public abstract class CameraHandler
    implements Renderer.Observer
{
    /**
     * Creates a new camera handler for the compositor camera.
     */
    public CameraHandler (GlContext ctx)
    {
        this(ctx, ctx.getCompositor().getCamera(), true);
    }

    /**
     * Creates a new camera handler for the specified camera.
     *
     * @param matchRenderSurface if true, automatically adjust the camera viewport to match the
     * dimensions of the renderer surface.
     */
    public CameraHandler (GlContext ctx, Camera camera, boolean matchRenderSurface)
    {
        _ctx = ctx;
        _camera = camera;

        // if specified, update the camera viewport and listen for changes
        if (matchRenderSurface) {
            Renderer renderer = ctx.getRenderer();
            sizeChanged(renderer.getWidth(), renderer.getHeight());
            renderer.addObserver(this);
        }
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
        Rectangle viewport = _camera.getViewport();
        _camera.setPerspective(_fovy, (float)viewport.width / viewport.height, _near, _far);
    }

    /**
     * Updates the camera position.
     */
    public abstract void updatePosition ();

    // documentation inherited from interface Renderer.Observer
    public void sizeChanged (int width, int height)
    {
        _camera.getViewport().set(0, 0, width, height);
        updatePerspective();
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** The camera that we're handling. */
    protected Camera _camera;

    /** The vertical field of view (in radians). */
    protected float _fovy = FloatMath.PI / 3f;

    /** The distance to the near clip plane. */
    protected float _near = 1f;

    /** The distance to the far clip plane. */
    protected float _far = 100f;
}
