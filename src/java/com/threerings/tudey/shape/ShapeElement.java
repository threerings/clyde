//
// $Id$

package com.threerings.tudey.shape;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.config.ShapeConfig;
import com.threerings.tudey.space.SimpleSpaceElement;

/**
 * A shape element.
 */
public class ShapeElement extends SimpleSpaceElement
{
    /**
     * Creates a new shape element.
     */
    public ShapeElement (ShapeConfig config)
    {
        setConfig(config);
    }

    /**
     * Creates a new shape element.
     */
    public ShapeElement (Shape localShape)
    {
        setLocalShape(localShape);
    }

    /**
     * Sets the configuration of the shape.
     */
    public void setConfig (ShapeConfig config)
    {
        setLocalShape(config.getShape());
    }

    /**
     * Sets the local shape reference.
     */
    public void setLocalShape (Shape shape)
    {
        _localShape = shape;
        updateBounds();
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _worldShape = _localShape.transform(_transform, _worldShape);
        Rect sbounds = _worldShape.getBounds();
        if (!_bounds.equals(sbounds)) {
            boundsWillChange();
            _bounds.set(sbounds);
            boundsDidChange();
        }
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return _worldShape.getIntersection(ray, result);
    }

    @Override // documentation inherited
    public boolean intersects (Point point)
    {
        return _worldShape.intersects(point);
    }

    @Override // documentation inherited
    public boolean intersects (Segment segment)
    {
        return _worldShape.intersects(segment);
    }

    @Override // documentation inherited
    public boolean intersects (Circle circle)
    {
        return _worldShape.intersects(circle);
    }

    @Override // documentation inherited
    public boolean intersects (Capsule capsule)
    {
        return _worldShape.intersects(capsule);
    }

    @Override // documentation inherited
    public boolean intersects (Polygon polygon)
    {
        return _worldShape.intersects(polygon);
    }

    @Override // documentation inherited
    public boolean intersects (Compound compound)
    {
        return _worldShape.intersects(compound);
    }

    /** The untransformed shape. */
    protected Shape _localShape;

    /** The transformed shape. */
    protected Shape _worldShape;
}
