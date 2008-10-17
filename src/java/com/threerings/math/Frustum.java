//
// $Id$

package com.threerings.math;

/**
 * A pyramidal frustum.
 */
public class Frustum
{
    /** Intersection types indicating that the frustum does not intersect, intersects, or fully
     * contains, respectively, the parameter. */
    public enum IntersectionType { NONE, INTERSECTS, CONTAINS };

    /**
     * Creates an empty (invalid) frustum.
     */
    public Frustum ()
    {
        // initialize the vertices and planes of the frustum
        for (int ii = 0; ii < 8; ii++) {
            _vertices[ii] = new Vector3f();
        }
        for (int ii = 0; ii < 6; ii++) {
            _planes[ii] = new Plane();
            _extents[ii] = new Box();
        }
    }

    /**
     * Sets this frustum to one pointing in the Z- direction with the specified parameters
     * determining its size and shape (see the OpenGL documentation for
     * <code>gluPerspective</code>).
     *
     * @param fovy the vertical field of view, in radians.
     * @param aspect the aspect ratio (width over height).
     * @param znear the distance to the near clip plane.
     * @param zfar the distance to the far clip plane.
     * @return a reference to this frustum, for chaining.
     */
    public Frustum setToPerspective (float fovy, float aspect, float znear, float zfar)
    {
        float top = znear * FloatMath.tan(fovy / 2f), bottom = -top;
        float right = top * aspect, left = -right;
        return setToFrustum(left, right, bottom, top, znear, zfar);
    }

    /**
     * Sets this frustum to one pointing in the Z- direction with the specified parameters
     * determining its size and shape (see the OpenGL documentation for <code>glFrustum</code>).
     *
     * @return a reference to this frustum, for chaining.
     */
    public Frustum setToFrustum (
        float left, float right, float bottom, float top, float near, float far)
    {
        return setToProjection(left, right, bottom, top, near, far, false);
    }

    /**
     * Sets this frustum to an orthographic one pointing in the Z- direction with the specified
     * parameters determining its size (see the OpenGL documentation for <code>glOrtho</code>).
     *
     * @return a reference to this frustum, for chaining.
     */
    public Frustum setToOrtho (
        float left, float right, float bottom, float top, float near, float far)
    {
        return setToProjection(left, right, bottom, top, near, far, true);
    }

    /**
     * Sets this frustum to a perspective or orthographic projection with the specified parameters
     * determining its size and shape.
     *
     * @return a reference to this frustum, for chaining.
     */
    public Frustum setToProjection (
        float left, float right, float bottom, float top, float near, float far, boolean ortho)
    {
        _vertices[0].set(left, bottom, -near);
        _vertices[1].set(right, bottom, -near);
        _vertices[2].set(right, top, -near);
        _vertices[3].set(left, top, -near);

        if (ortho) {
            _vertices[4].set(left, bottom, -far);
            _vertices[5].set(right, bottom, -far);
            _vertices[6].set(right, top, -far);
            _vertices[7].set(left, top, -far);
        } else {
            float fscale = far / near, fleft = left * fscale, fright = right * fscale;
            float fbottom = bottom * fscale, ftop = top * fscale;
            _vertices[4].set(fleft, fbottom, -far);
            _vertices[5].set(fright, fbottom, -far);
            _vertices[6].set(fright, ftop, -far);
            _vertices[7].set(fleft, ftop, -far);
        }

        updateDerivedState();
        return this;
    }

    /**
     * Transforms this frustum in-place by the specified transformation.
     *
     * @return a reference to this frustum, for chaining.
     */
    public Frustum transformLocal (Transform3D transform)
    {
        return transform(transform, this);
    }

    /**
     * Transforms this frustum by the specified transformation.
     *
     * @return a new frustum containing the result.
     */
    public Frustum transform (Transform3D transform)
    {
        return transform(transform, new Frustum());
    }

    /**
     * Transforms this frustum by the specified transformation, placing the result in the object
     * provided.
     *
     * @return a reference to the result frustum, for chaining.
     */
    public Frustum transform (Transform3D transform, Frustum result)
    {
        // transform all of the vertices
        for (int ii = 0; ii < 8; ii++) {
            transform.transformPoint(_vertices[ii], result._vertices[ii]);
        }
        result.updateDerivedState();
        return result;
    }

    /**
     * Returns a reference to the bounds of this frustum.
     */
    public Box getBounds ()
    {
        return _bounds;
    }

    /**
     * Determines the maximum signed distance of the point from the planes of the frustum.  If
     * the distance is less than or equal to zero, the point lies inside the frustum.
     */
    public float getDistance (Vector3f point)
    {
        float distance = -Float.MAX_VALUE;
        for (Plane plane : _planes) {
            distance = Math.max(distance, plane.getDistance(point));
        }
        return distance;
    }

    /**
     * Checks whether the frustum intersects the specified box.
     */
    public IntersectionType getIntersectionType (Box box)
    {
        // exit quickly in cases where the bounding boxes don't overlap (equivalent to a separating
        // axis test using the axes of the box)
        if (!_bounds.intersects(box)) {
            return IntersectionType.NONE;
        }

        // consider each side of the frustum as a potential separating axis
        int ccount = 0;
        for (int ii = 0; ii < 6; ii++) {
            // determine how many vertices fall inside/outside the plane and compute cross
            // product extents
            int inside = 0;
            Plane plane = _planes[ii];
            Vector3f normal = plane.getNormal();
            _extent.setToEmpty();
            for (int jj = 0; jj < 8; jj++) {
                if (plane.getDistance(box.getVertex(jj, _vertex)) <= 0f) {
                    inside++;
                }
                _extent.addLocal(_vertex.cross(normal, _vector));
            }
            if (inside == 0 || !_extents[ii].intersects(_extent)) {
                return IntersectionType.NONE;
            } else if (inside == 8) {
                ccount++;
            }
        }
        return (ccount == 6) ? IntersectionType.CONTAINS : IntersectionType.INTERSECTS;
    }

    /**
     * Sets the planes and bounding box of the frustum based on its vertices.
     */
    protected void updateDerivedState ()
    {
        _planes[0].fromPoints(_vertices[0], _vertices[1], _vertices[2]); // near
        _planes[1].fromPoints(_vertices[5], _vertices[4], _vertices[7]); // far
        _planes[2].fromPoints(_vertices[1], _vertices[5], _vertices[6]); // left
        _planes[3].fromPoints(_vertices[4], _vertices[0], _vertices[3]); // right
        _planes[4].fromPoints(_vertices[3], _vertices[2], _vertices[6]); // top
        _planes[5].fromPoints(_vertices[4], _vertices[5], _vertices[1]); // bottom
        _bounds.fromPoints(_vertices);

        // compute the cross product extents
        for (int ii = 0; ii < 6; ii++) {
            Box extents = _extents[ii];
            extents.setToEmpty();
            Vector3f normal = _planes[ii].getNormal();
            for (Vector3f vertex : _vertices) {
                extents.addLocal(vertex.cross(normal, _vector));
            }
        }
    }

    /** The vertices of the frustum. */
    protected Vector3f[] _vertices = new Vector3f[8];

    /** The planes of the frustum (as derived from the vertices).  The plane normals point out of
     * the frustum. */
    protected Plane[] _planes = new Plane[6];

    /** The frustum's bounding box (as derived from the vertices). */
    protected Box _bounds = new Box();

    /** Cross product extents for the six planes. */
    protected Box[] _extents = new Box[6];

    /** A working vertex. */
    protected static Vector3f _vertex = new Vector3f();

    /** A working vector. */
    protected static Vector3f _vector = new Vector3f();

    /** A working box. */
    protected static Box _extent = new Box();
}
