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
 * A point.
 */
public class Point extends Shape
{
    /**
     * Creates a point at the specified location.
     */
    public Point (Vector2f location)
    {
        _location.set(location);
        updateBounds();
    }

    /**
     * Creates an uninitialized point.
     */
    public Point ()
    {
    }

    /**
     * Returns a reference to the location of the point.
     */
    public Vector2f getLocation ()
    {
        return _location;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _bounds.getMinimumExtent().set(_location);
        _bounds.getMaximumExtent().set(_location);
    }

    @Override // documentation inherited
    public Vector2f getCenter (Vector2f result)
    {
        return result.set(_location);
    }

    @Override // documentation inherited
    public Shape transform (Transform2D transform, Shape result)
    {
        Point presult = (result instanceof Point) ? ((Point)result) : new Point();
        transform.transformPoint(_location, presult._location);
        presult.updateBounds();
        return presult;
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        boolean isect = ray.intersects(_location);
        if (isect) {
            result.set(_location);
        }
        return isect;
    }

    @Override // documentation inherited
    public IntersectionType getIntersectionType (Rect rect)
    {
        return rect.contains(_location) ? IntersectionType.INTERSECTS : IntersectionType.NONE;
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
        return point.getLocation().equals(_location);
    }

    @Override // documentation inherited
    public boolean intersects (Segment segment)
    {
        return segment.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Circle circle)
    {
        return circle.intersects(this);
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
    public Vector2f getPenetration (Shape shape, Vector2f result)
    {
        return shape.getPenetration(this, result).negateLocal();
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Point point, Vector2f result)
    {
        return result.set(Vector2f.ZERO);
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Segment segment, Vector2f result)
    {
        return segment.getPenetration(this, result).negateLocal();
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Circle circle, Vector2f result)
    {
        return circle.getPenetration(this, result).negateLocal();
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Capsule capsule, Vector2f result)
    {
        return capsule.getPenetration(this, result).negateLocal();
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Polygon polygon, Vector2f result)
    {
        return polygon.getPenetration(this, result).negateLocal();
    }

    @Override // documentation inherited
    public Vector2f getPenetration (Compound compound, Vector2f result)
    {
        return compound.getPenetration(this, result).negateLocal();
    }

    @Override // documentation inherited
    public void draw (boolean outline)
    {
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glVertex2f(_location.x, _location.y);
        GL11.glEnd();
    }

    /** The location of the point. */
    protected Vector2f _location = new Vector2f();
}
