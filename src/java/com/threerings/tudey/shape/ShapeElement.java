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
     * Sets the configuration of the shape.
     */
    public void setConfig (ShapeConfig config)
    {
        _config = config;
        updateBounds();
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _shape = _config.getShape().transform(_transform, _shape);
        Rect sbounds = _shape.getBounds();
        if (!_bounds.equals(sbounds)) {
            boundsWillChange();
            _bounds.set(sbounds);
            boundsDidChange();
        }
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return _shape.getIntersection(ray, result);
    }

    @Override // documentation inherited
    public boolean intersects (Point point)
    {
        return _shape.intersects(point);
    }

    @Override // documentation inherited
    public boolean intersects (Segment segment)
    {
        return _shape.intersects(segment);
    }

    @Override // documentation inherited
    public boolean intersects (Quad quad)
    {
        return _shape.intersects(quad);
    }

    @Override // documentation inherited
    public boolean intersects (Circle circle)
    {
        return _shape.intersects(circle);
    }

    @Override // documentation inherited
    public boolean intersects (Capsule capsule)
    {
        return _shape.intersects(capsule);
    }

    @Override // documentation inherited
    public boolean intersects (Polygon polygon)
    {
        return _shape.intersects(polygon);
    }

    @Override // documentation inherited
    public boolean intersects (Compound compound)
    {
        return _shape.intersects(compound);
    }

    /** The configuration of the shape. */
    protected ShapeConfig _config;

    /** The transformed shape. */
    protected Shape _shape;
}
