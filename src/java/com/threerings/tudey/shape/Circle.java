//
// $Id$

package com.threerings.tudey.shape;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.space.SpaceElement;

/**
 * A circle.
 */
public class Circle extends Shape
{
    /** The radius of the circle. */
    public float radius;

    /**
     * Creates a circle with the supplied center and radius.
     */
    public Circle (Vector2f center, float radius)
    {
        _center.set(center);
        this.radius = radius;
        updateBounds();
    }

    /**
     * Creates an uninitialized circle.
     */
    public Circle ()
    {
    }

    /**
     * Returns a reference to the center of the circle.
     */
    public Vector2f getCenter ()
    {
        return _center;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _bounds.getMinimumExtent().set(_center);
        _bounds.getMaximumExtent().set(_center);
        _bounds.expandLocal(radius, radius);
    }

    @Override // documentation inherited
    public Shape transform (Transform2D transform, Shape result)
    {
        Circle cresult = (result instanceof Circle) ? ((Circle)result) : new Circle();
        transform.transformPoint(_center, cresult._center);
        cresult.radius = radius * transform.approximateUniformScale();
        cresult.updateBounds();
        return cresult;
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
        return point.getLocation().distance(_center) <= radius;
    }

    @Override // documentation inherited
    public boolean intersects (Segment segment)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Circle circle)
    {
        return circle.getCenter().distance(_center) <= (circle.radius + radius);
    }

    @Override // documentation inherited
    public boolean intersects (Capsule capsule)
    {
        return capsule.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Polygon polygon)
    {
        return polygon.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Compound compound)
    {
        return compound.intersects(this);
    }

    /** The center of the circle. */
    protected Vector2f _center = new Vector2f();
}
