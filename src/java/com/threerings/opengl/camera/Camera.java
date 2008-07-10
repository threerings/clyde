//
// $Id$

package com.threerings.opengl.camera;

import com.threerings.math.FloatMath;
import com.threerings.math.Frustum;
import com.threerings.math.Matrix4f;
import com.threerings.math.Ray;
import com.threerings.math.Rect;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Contains the camera state.
 */
public class Camera
{
    /**
     * Returns a reference to the camera's current transform in world space.
     */
    public Transform3D getWorldTransform ()
    {
        return _worldTransform;
    }

    /**
     * Returns a reference to the camera view transform (the inverse of the world transform).
     */
    public Transform3D getViewTransform ()
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
        if (ortho) {
            _projection.setToOrtho(left, right, bottom, top, near, far);
        } else {
            _projection.setToFrustum(left, right, bottom, top, near, far);
        }
    }

    /**
     * Returns a reference to the projection matrix.
     */
    public Matrix4f getProjection ()
    {
        return _projection;
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
     * Updates the camera transform.
     */
    public void updateTransform ()
    {
        // the view transform is the inverse of the world transform
        _worldTransform.invert(_viewTransform);

        // make sure our matrices are up-to-date
        _worldTransform.update(Transform3D.AFFINE);
        _viewTransform.update(Transform3D.AFFINE);

        // transform the frustum into world space
        _localVolume.transform(_worldTransform, _worldVolume);
    }

    /**
     * Applies the camera state to the specified renderer.
     */
    public void apply (Renderer renderer)
    {
        renderer.setViewport(_viewport);
        renderer.setProjection(_left, _right, _bottom, _top, _near, _far, _ortho);
    }

    /**
     * Transforms and projects a point in world space into window space.
     *
     * @return a reference to the point, for chaining.
     */
    public Vector3f transformLocal (Vector3f point)
    {
        return transform(point, point);
    }

    /**
     * Transforms and projects a point in world space into window space.
     *
     * @return a new vector containing the result.
     */
    public Vector3f transform (Vector3f point)
    {
        return transform(point, new Vector3f());
    }

    /**
     * Transforms and projects a point in world space into window space, placing the result in
     * the object provided.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector3f transform (Vector3f point, Vector3f result)
    {
        return _projection.projectPointLocal(_viewTransform.transformPoint(point, result));
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

    /** The camera viewport. */
    protected Rectangle _viewport = new Rectangle();

    /** The camera frustum parameters. */
    protected float _left = -1f, _right = +1f, _bottom = -1f, _top = +1f, _near = +1f, _far = -1f;

    /** Whether or not the camera is set to an orthographic projection. */
    protected boolean _ortho = true;

    /** The cached projection matrix. */
    protected Matrix4f _projection = new Matrix4f();

    /** The camera's current transform in world space. */
    protected Transform3D _worldTransform = new Transform3D(Transform3D.UNIFORM);

    /** The camera's view transform (the inverse of its world transform). */
    protected Transform3D _viewTransform = new Transform3D(Transform3D.UNIFORM);

    /** The camera's local view volume. */
    protected Frustum _localVolume = new Frustum();

    /** The camera's world space view volume. */
    protected Frustum _worldVolume = new Frustum();
}
