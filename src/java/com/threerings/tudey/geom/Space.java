//
// $Id$

package com.threerings.tudey.geom;

import java.util.List;

/**
 * A container for shapes (as well as a shape itself) that allows intersection testing.
 */
public abstract class Space extends Shape
{
    /**
     * Adds a shape to this space if it is not already present.
     *
     * @return true if the shape was added, false if it was already present.
     */
    public abstract boolean add (Shape shape);

    /**
     * Removes a shape from this space if it is present.
     *
     * @return true if the shape was removed, false if it was not present.
     */
    public abstract boolean remove (Shape shape);

    /**
     * Determines whether this space contains the specified shape.
     */
    public boolean contains (Shape shape)
    {
        return (shape.getSpace() == this);
    }

    /**
     * Determines whether this space is empty.
     */
    public boolean isEmpty ()
    {
        return size() == 0;
    }

    /**
     * Returns the number of shapes in this space.
     */
    public abstract int size ();

    /**
     * Removes all the shapes from this space.
     */
    public abstract void clear ();

    /**
     * Finds all shapes that intersect the one given and places them into the
     * provided result list.
     */
    public abstract void getIntersecting (Shape shape, List<Shape> results);

    /**
     * Finds all the shape intersections in the space and places them into the
     * provided result list.
     */
    public abstract void getIntersecting (List<Intersection> results);

    @Override // documentation inherited
    protected boolean checkIntersects (Point point)
    {
        return intersects(point);
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Line line)
    {
        return intersects(line);
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Circle circle)
    {
        return intersects(circle);
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Rectangle rectangle)
    {
        return intersects(rectangle);
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Capsule capsule)
    {
        return intersects(capsule);
    }

    /**
     * Notifies the space that a contained shape is about to move.
     */
    protected void shapeWillMove (Shape shape)
    {
        // nothing by default
    }

    /**
     * Notifies the space that a contained shape just moved.
     */
    protected void shapeDidMove (Shape shape)
    {
        // nothing by default
    }
}
