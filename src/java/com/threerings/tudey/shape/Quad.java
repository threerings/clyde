//
// $Id$

package com.threerings.tudey.shape;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.space.SpaceElement;

/**
 * A quadrilateral.
 */
public class Quad extends Shape
{
    /**
     * Creates a quad with the supplied vertices.
     */
    public Quad (Vector2f... vertices)
    {
        for (int ii = 0; ii < 4; ii++) {
            _vertices[ii].set(vertices[ii]);
        }
        updateBounds();
    }

    /**
     * Creates an uninitialized quad.
     */
    public Quad ()
    {
    }

    /**
     * Returns a reference to the indexed vertex.
     */
    public Vector2f getVertex (int idx)
    {
        return _vertices[idx];
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _bounds.fromPoints(_vertices);
    }

    @Override // documentation inherited
    public Shape transform (Transform2D transform, Shape result)
    {
        Quad qresult = (result instanceof Quad) ? ((Quad)result) : new Quad();
        for (int ii = 0; ii < 4; ii++) {
            transform.transformPoint(_vertices[ii], qresult._vertices[ii]);
        }
        qresult.updateBounds();
        return qresult;
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return false;
    }

    @Override // documentation inherited
    public IntersectionType getIntersectionType (Rect rect)
    {
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
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Segment segment)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Quad quad)
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
        return false;
    }

    /** The vertices of the quad. */
    protected Vector2f[] _vertices = new Vector2f[] {
        new Vector2f(), new Vector2f(), new Vector2f(), new Vector2f() };
}
