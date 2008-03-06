//
// $Id$

package com.threerings.opengl.renderer;

import org.lwjgl.opengl.GL11;

import com.threerings.math.FloatMath;
import com.threerings.math.Frustum;
import com.threerings.math.Ray;
import com.threerings.math.Transform;
import com.threerings.math.Vector3f;

import com.threerings.opengl.gui.util.Rectangle;

/**
 * Contains the camera state.
 */
public class Camera
{
    /**
     * Creates a camera for the specified renderer.
     *
     * @param width the initial viewport width.
     * @param height the initial viewport height.
     */
    public Camera (int width, int height)
    {
        _viewport.set(0, 0, width, height);
    }

    /**
     * Updates the camera transform.
     */
    public void updateTransform ()
    {
        _worldTransform.invert(_viewTransform);
        _localVolume.transform(_worldTransform, _worldVolume);
    }

    /**
     * Returns a reference to the camera's current transform in world space.
     */
    public Transform getWorldTransform ()
    {
        return _worldTransform;
    }

    /**
     * Returns a reference to the camera view transform (the inverse of the world transform).
     */
    public Transform getViewTransform ()
    {
        return _viewTransform;
    }

    /**
     * Returns a reference to the volume that the camera occupies in the world.
     */
    public Frustum getWorldVolume ()
    {
        return _worldVolume;
    }

    /**
     * Sets the camera viewport.
     */
    public void setViewport (int x, int y, int width, int height)
    {
        _viewport.set(x, y, width, height);
        if (_renderer != null) {
            _renderer.setViewport(_viewport);
        }
    }

    /**
     * Returns a reference to the camera viewport.
     */
    public Rectangle getViewport ()
    {
        return _viewport;
    }

    /**
     * Sets the camera perspective parameters.
     */
    public void setPerspective (float fovy, float aspect, float near, float far)
    {
        float top = near * FloatMath.tan(fovy / 2f), bottom = -top;
        float right = top * aspect, left = -right;
        setFrustum(left, right, bottom, top, near, far);
    }

    /**
     * Sets the camera frustum parameters.
     */
    public void setFrustum (
        float left, float right, float bottom, float top, float near, float far)
    {
        setProjection(left, right, bottom, top, near, far, false);
    }

    /**
     * Sets the camera frustum parameters.
     */
    public void setOrtho (
        float left, float right, float bottom, float top, float near, float far)
    {
        setProjection(left, right, bottom, top, near, far, true);
    }

    /**
     * Sets the camera projection parameters.
     */
    public void setProjection (
        float left, float right, float bottom, float top, float near, float far, boolean ortho)
    {
        _localVolume.setToProjection(
            _left = left, _right = right, _bottom = bottom, _top = top,
            _near = near, _far = far, _ortho = ortho);
        if (_renderer != null) {
            _renderer.setProjection(_left, _right, _bottom, _top, _near, _far, _ortho);
        }
    }

    /**
     * Returns the location of the left edge of the view frustum at the near plane.
     */
    public float getLeft ()
    {
        return _left;
    }

    /**
     * Returns the location of the right edge of the view frustum at the near plane.
     */
    public float getRight ()
    {
        return _right;
    }

    /**
     * Returns the location of the bottom edge of the view frustum at the near plane.
     */
    public float getBottom ()
    {
        return _bottom;
    }

    /**
     * Returns the location of the top edge of the view frustum at the near plane.
     */
    public float getTop ()
    {
        return _top;
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
     * Determines whether or not the camera is set to an orthographic projection.
     */
    public boolean isOrtho ()
    {
        return _ortho;
    }

    /**
     * Populates the supplied object with a ray through the center of the viewport.
     */
    public void getCenterRay (Ray result)
    {
        getEyeRay((_left + _right) / 2f, (_bottom + _top) / 2f, result);
    }

    /**
     * Populates the supplied object with a ray through the specified viewport coordinates.
     */
    public void getPickRay (int x, int y, Ray result)
    {
        // convert to fractional coordinates
        float tx = (float)(x - _viewport.x) / _viewport.width;
        float ty = (float)(y - _viewport.y) / _viewport.height;

        // convert coords to eye space
        getEyeRay(FloatMath.lerp(_left, _right, tx), FloatMath.lerp(_bottom, _top, ty), result);
    }

    /**
     * Populates the supplied object with a ray through the specified eye space coordinates (at the
     * near clip plane).
     */
    protected void getEyeRay (float ex, float ey, Ray result)
    {
        result.getOrigin().set(ex, ey, -_near);
        if (_ortho) {
            result.getDirection().set(0f, 0f, -1f);
        } else {
            result.getDirection().set(ex, ey, -_near);
        }
        // transforming the ray also normalizes its direction
        result.transformLocal(_worldTransform);
    }

    /**
     * Notifies this camera that it has been set in the renderer.
     */
    protected void setRenderer (Renderer renderer)
    {
        if ((_renderer = renderer) != null) {
            _renderer.setViewport(_viewport);
            _renderer.setProjection(_left, _right, _bottom, _top, _near, _far, _ortho);
        }
    }

    /** The renderer with which this camera is registered, if any. */
    protected Renderer _renderer;

    /** The camera viewport. */
    protected Rectangle _viewport = new Rectangle();

    /** The camera frustum parameters. */
    protected float _left = -1f, _right = +1f, _bottom = -1f, _top = +1f, _near = +1f, _far = -1f;

    /** Whether or not the camera is set to an orthographic projection. */
    protected boolean _ortho = true;

    /** The camera's current transform in world space. */
    protected Transform _worldTransform = new Transform(Transform.UNIFORM);

    /** The camera's view transform (the inverse of its world transform). */
    protected Transform _viewTransform = new Transform(Transform.UNIFORM);

    /** The camera's local view volume. */
    protected Frustum _localVolume = new Frustum();

    /** The camera's world space view volume. */
    protected Frustum _worldVolume = new Frustum();
}
