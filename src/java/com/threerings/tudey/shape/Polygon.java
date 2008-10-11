//
// $Id$

package com.threerings.tudey.shape;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.space.SpaceElement;

/**
 * A convex polygon.
 */
public class Polygon extends Shape
{
    /**
     * Creates a polygon with the supplied vertices.
     */
    public Polygon (Vector2f... vertices)
    {
        _vertices = new Vector2f[vertices.length];
        for (int ii = 0; ii < vertices.length; ii++) {
            _vertices[ii] = new Vector2f(vertices[ii]);
        }
        updateBounds();
    }

    /**
     * Creates an uninitialized polygon with the specified number of vertices.
     */
    public Polygon (int vcount)
    {
        initVertices(vcount);
    }

    /**
     * Returns the number of vertices in this polygon.
     */
    public int getVertexCount ()
    {
        return _vertices.length;
    }

    /**
     * Returns a reference to the indexed vertex.
     */
    public Vector2f getVertex (int idx)
    {
        return _vertices[idx];
    }

    /**
     * Checks whether the polygon contains the specified point.
     */
    public boolean contains (Vector2f pt)
    {
        return contains(pt.x, pt.y);
    }

    /**
     * Checks whether the polygon contains the specified point.
     */
    public boolean contains (float x, float y)
    {
        // check the point against each edge
        for (int ii = 0; ii < _vertices.length; ii++) {
            Vector2f start = _vertices[ii], end = _vertices[(ii + 1) % _vertices.length];
            float a = start.y - end.y;
            float b = end.x - start.x;
            if (a*x + b*y < a*start.x + b*start.y) {
                return false;
            }
        }
        return true;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _bounds.fromPoints(_vertices);
    }

    @Override // documentation inherited
    public Shape transform (Transform2D transform, Shape result)
    {
        Polygon presult = (result instanceof Polygon) ?
            ((Polygon)result) : new Polygon(_vertices.length);
        if (presult.getVertexCount() != _vertices.length) {
            presult.initVertices(_vertices.length);
        }
        for (int ii = 0; ii < _vertices.length; ii++) {
            transform.transformPoint(_vertices[ii], presult._vertices[ii]);
        }
        presult.updateBounds();
        return presult;
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        // see if we start inside the polygon
        Vector2f origin = ray.getOrigin();
        if (contains(origin)) {
            result.set(origin);
            return true;
        }
        // check the ray against each edge (making sure it's on the right side)
        for (int ii = 0; ii < _vertices.length; ii++) {
            Vector2f start = _vertices[ii], end = _vertices[(ii + 1) % _vertices.length];
            float a = start.y - end.y;
            float b = end.x - start.x;
            if (a*origin.x + b*origin.y <= a*start.x + b*start.y &&
                    ray.getIntersection(start, end, result)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public IntersectionType getIntersectionType (Rect rect)
    {
        // check the corners of the rectangle
        Vector2f min = rect.getMinimumExtent(), max = rect.getMaximumExtent();
        int ccount =
            (contains(min.x, min.y) ? 1 : 0) +
            (contains(max.x, min.y) ? 1 : 0) +
            (contains(max.x, max.y) ? 1 : 0) +
            (contains(min.x, max.y) ? 1 : 0);
        if (ccount > 0) {
            return (ccount == 4) ? IntersectionType.CONTAINS : IntersectionType.INTERSECTS;
        }
        return IntersectionType.NONE;
    }

    @Override // documentation inherited
    public boolean intersects (SpaceElement element)
    {
        return element.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Shape shape)
    {
        return shape.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Point point)
    {
        return contains(point.getLocation());
    }

    @Override // documentation inherited
    public boolean intersects (Segment segment)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Circle circle)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Capsule capsule)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Polygon polygon)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Compound compound)
    {
        return compound.intersects(this);
    }

    /**
     * (Re)initializes the vertex array for the specified number of vertices.
     */
    protected void initVertices (int vcount)
    {
        _vertices = new Vector2f[vcount];
        for (int ii = 0; ii < vcount; ii++) {
            _vertices[ii] = new Vector2f();
        }
    }

    /** The vertices of the polygon. */
    protected Vector2f[] _vertices;
}
