//
// $Id$

package com.threerings.math;

import java.util.Comparator;

import com.threerings.export.Exportable;

/**
 * A triangle defined by three vertices (in CCW winding order).
 */
public final class Triangle
    implements Exportable
{
    /** Compares triangles based on the x coordinates of their centers. */
    public static final Comparator<Triangle> X_COMPARATOR = new Comparator<Triangle>() {
        public int compare (Triangle t1, Triangle t2) {
            return Float.compare(t1.getCenterX(), t2.getCenterX());
        }
    };

    /** Compares triangles based on the y coordinates of their centers. */
    public static final Comparator<Triangle> Y_COMPARATOR = new Comparator<Triangle>() {
        public int compare (Triangle t1, Triangle t2) {
            return Float.compare(t1.getCenterY(), t2.getCenterY());
        }
    };

    /** Compares triangles based on the z coordinates of their centers. */
    public static final Comparator<Triangle> Z_COMPARATOR = new Comparator<Triangle>() {
        public int compare (Triangle t1, Triangle t2) {
            return Float.compare(t1.getCenterZ(), t2.getCenterZ());
        }
    };

    /**
     * Creates a triangle from the values contained in the supplied objects.
     */
    public Triangle (Vector3f v1, Vector3f v2, Vector3f v3)
    {
        set(v1, v2, v3);
    }

    /**
     * Copy constructor.
     */
    public Triangle (Triangle other)
    {
        set(other);
    }

    /**
     * Creates a triangle whose vertices are zero vectors.
     */
    public Triangle ()
    {
    }

    /**
     * Returns a reference to the triangle's first vertex.
     */
    public Vector3f getFirstVertex ()
    {
        return _v1;
    }

    /**
     * Returns a reference to the triangle's second vertex.
     */
    public Vector3f getSecondVertex ()
    {
        return _v2;
    }

    /**
     * Returns a reference to the triangle's third vertex.
     */
    public Vector3f getThirdVertex ()
    {
        return _v3;
    }

    /**
     * Returns a reference to the vertex at the specified index.
     */
    public Vector3f getVertex (int idx)
    {
        switch (idx) {
            case 0: return _v1;
            case 1: return _v2;
            case 2: return _v3;
        }
        throw new IndexOutOfBoundsException(Integer.toString(idx));
    }

    /**
     * Returns the center of the triangle as a new vector.
     */
    public Vector3f getCenter ()
    {
        return _v1.add(_v2).addLocal(_v3).multLocal(1f / 3f);
    }

    /**
     * Returns the x coordinate of the triangle's center.
     */
    public float getCenterX ()
    {
        return (_v1.x + _v2.x + _v3.x) * (1f / 3f);
    }

    /**
     * Returns the y coordinate of the triangle's center.
     */
    public float getCenterY ()
    {
        return (_v1.y + _v2.y + _v3.y) * (1f / 3f);
    }

    /**
     * Returns the z coordinate of the triangle's center.
     */
    public float getCenterZ ()
    {
        return (_v1.z + _v2.z + _v3.z) * (1f / 3f);
    }

    /**
     * Sets the vertices of the triangle to those of the specified other triangle.
     *
     * @return a reference to this triangle, for chaining.
     */
    public Triangle set (Triangle other)
    {
        return set(other.getFirstVertex(), other.getSecondVertex(), other.getThirdVertex());
    }

    /**
     * Sets the vertices of the triangle to those contained in the supplied objects.
     *
     * @return a reference to this triangle, for chaining.
     */
    public Triangle set (Vector3f v1, Vector3f v2, Vector3f v3)
    {
        _v1.set(v1);
        _v2.set(v2);
        _v3.set(v3);
        return this;
    }

    /**
     * Computes the intersection of the supplied ray with this triangle, placing the result in
     * the given vector (if the ray intersects).  Uses the algorithm described in Tomas Moller
     * and Ben Trumbore's <a href="http://www.graphics.cornell.edu/pubs/1997/MT97.html">
     * Fast, Minimum Storage Ray/Triangle Intersection</a>.
     *
     * @return true if the ray intersects this triangle (in which case the result will contain
     * the point of intersection), false if not.
     */
    public boolean getIntersection (Ray ray, Vector3f result)
    {
        // compute edges
        Vector3f e1 = _v2.subtract(_v1);
        Vector3f e2 = _v3.subtract(_v1);

        // P = D x E2
        Vector3f dir = ray.getDirection();
        Vector3f pvec = dir.cross(e2);

        // if determinant is near zero, ray lies in triangle plane
        float determinant = e1.dot(pvec);
        if (determinant < FloatMath.EPSILON) {
            return false;
        }

        // T = O - V0
        Vector3f origin = ray.getOrigin();
        Vector3f tvec = origin.subtract(_v1);

        // calculate u parameter and test bounds
        float u = tvec.dot(pvec);
        if (u < 0f || u > determinant) {
            return false;
        }

        // calculate v parameter and test bounds
        Vector3f qvec = tvec.cross(e1);
        float v = dir.dot(qvec);
        if (v < 0f || (u + v) > determinant) {
            return false;
        }

        // calculate t, ray intersects triangle
        origin.addScaled(dir, e2.dot(qvec) / determinant, result);
        return true;
    }

    /** The vertices of the triangle. */
    protected Vector3f _v1 = new Vector3f(), _v2 = new Vector3f(), _v3 = new Vector3f();
}
