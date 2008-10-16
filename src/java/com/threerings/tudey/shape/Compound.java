//
// $Id$

package com.threerings.tudey.shape;

import org.lwjgl.opengl.GL11;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.space.SpaceElement;

/**
 * A compound shape.
 */
public class Compound extends Shape
{
    /**
     * Creates a compound containing the supplied shapes (which will be referenced, not copied).
     */
    public Compound (Shape... shapes)
    {
        _shapes = shapes;
        updateBounds();
    }

    /**
     * Creates an uninitialized compound.
     */
    public Compound (int scount)
    {
        _shapes = new Shape[scount];
    }

    /**
     * Returns the number of shapes in this compound.
     */
    public int getShapeCount ()
    {
        return _shapes.length;
    }

    /**
     * Returns a reference to the indexed shape.
     */
    public Shape getShape (int idx)
    {
        return _shapes[idx];
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _bounds.setToEmpty();
        for (Shape shape : _shapes) {
            shape.updateBounds();
            _bounds.addLocal(shape.getBounds());
        }
    }

    @Override // documentation inherited
    public Shape transform (Transform2D transform, Shape result)
    {
        Compound cresult = (result instanceof Compound) ?
            ((Compound)result) : new Compound(_shapes.length);
        if (cresult.getShapeCount() != _shapes.length) {
            cresult._shapes = new Shape[_shapes.length];
        }
        for (int ii = 0; ii < _shapes.length; ii++) {
            cresult._shapes[ii] = _shapes[ii].transform(transform, cresult._shapes[ii]);
        }
        cresult.updateBounds();
        return cresult;
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        if (!_bounds.intersects(ray)) {
            return false;
        }
        Vector2f closest = result;
        for (Shape shape : _shapes) {
            if (shape.getIntersection(ray, result)) {
                result = updateClosest(ray.getOrigin(), result, closest);
            }
        }
        return (result != closest);
    }

    @Override // documentation inherited
    public IntersectionType getIntersectionType (Rect rect)
    {
        if (!rect.intersects(_bounds)) {
            return IntersectionType.NONE;
        }
        IntersectionType type = IntersectionType.NONE;
        for (Shape shape : _shapes) {
            switch (shape.getIntersectionType(rect)) {
                case CONTAINS:
                    return IntersectionType.CONTAINS;
                case INTERSECTS:
                    type = IntersectionType.INTERSECTS;
            }
        }
        return type;
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
        if (!point.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (shape.intersects(point)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Segment segment)
    {
        if (!segment.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (shape.intersects(segment)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Circle circle)
    {
        if (!circle.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (shape.intersects(circle)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Capsule capsule)
    {
        if (!capsule.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (shape.intersects(capsule)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Polygon polygon)
    {
        if (!polygon.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (shape.intersects(polygon)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Compound compound)
    {
        if (!compound.getBounds().intersects(_bounds)) {
            return false;
        }
        for (Shape shape : _shapes) {
            if (compound.intersects(shape)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public void draw (boolean outline)
    {
        for (Shape shape : _shapes) {
            shape.draw(outline);
        }
    }

    /** The shapes of which this shape is composed. */
    protected Shape[] _shapes;
}
