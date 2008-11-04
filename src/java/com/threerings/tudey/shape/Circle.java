//
// $Id$

package com.threerings.tudey.shape;

import org.lwjgl.opengl.GL11;

import com.threerings.math.FloatMath;
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

    /**
     * Checks whether the circle contains the specified point.
     */
    public boolean contains (Vector2f pt)
    {
        return _center.distanceSquared(pt) <= radius*radius;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _bounds.getMinimumExtent().set(_center);
        _bounds.getMaximumExtent().set(_center);
        _bounds.expandLocal(radius, radius);
    }

    @Override // documentation inherited
    public Vector2f getCenter (Vector2f result)
    {
        return result.set(_center);
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
        return ray.getIntersection(_center, radius, result);
    }

    @Override // documentation inherited
    public IntersectionType getIntersectionType (Rect rect)
    {
        // test the points of the rect against the circle
        Vector2f min = rect.getMinimumExtent(), max = rect.getMaximumExtent();
        float r2 = radius*radius;
        int ccount =
            (contains(min.x, min.y, r2) ? 1 : 0) +
            (contains(max.x, min.y, r2) ? 1 : 0) +
            (contains(max.x, max.y, r2) ? 1 : 0) +
            (contains(min.x, max.y, r2) ? 1 : 0);
        if (ccount > 0) {
            return (ccount == 4) ? IntersectionType.CONTAINS : IntersectionType.INTERSECTS;
        }
        // handle the non-corner cases
        if (_center.x < min.x) { // left
            return (_center.y >= min.y && _center.y <= max.y && (min.x - _center.x) <= radius) ?
                IntersectionType.INTERSECTS : IntersectionType.NONE;

        } else if (_center.x > max.x) { // right
            return (_center.y >= min.y && _center.y <= max.y && (_center.x - max.x) <= radius) ?
                IntersectionType.INTERSECTS : IntersectionType.NONE;

        } else { // middle
            if (_center.y < min.y) { // middle-bottom
                return (min.y - _center.y) <= radius ?
                    IntersectionType.INTERSECTS : IntersectionType.NONE;

            } else if (_center.y > max.y) { // middle-top
                return (_center.y - max.y) <= radius ?
                    IntersectionType.INTERSECTS : IntersectionType.NONE;

            } else { // middle-middle
                return IntersectionType.INTERSECTS;
            }
        }
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
        return intersects(_center, radius, segment.getStart(), segment.getEnd());
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

    @Override // documentation inherited
    public void draw (boolean outline)
    {
        GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_POLYGON);
        for (int ii = 0; ii < CIRCLE_SEGMENTS; ii++) {
            float angle = ii * CIRCLE_INCREMENT;
            GL11.glVertex2f(
                _center.x + FloatMath.cos(angle) * radius,
                _center.y + FloatMath.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    /**
     * Checks whether the circle contains the specified point.
     */
    protected boolean contains (float x, float y, float r2)
    {
        float dx = x - _center.x, dy = y - _center.y;
        return dx*dx + dy*dy <= r2;
    }

    /** The center of the circle. */
    protected Vector2f _center = new Vector2f();
}
