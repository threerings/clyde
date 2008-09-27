//
// $Id$

package com.threerings.tudey.shape;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.config.ShapeConfig;
import com.threerings.tudey.space.Intersector;
import com.threerings.tudey.space.SimpleSpaceElement;

/**
 * A Tudey shape.
 */
public class Shape extends SimpleSpaceElement
{
    /**
     * Creates a new shape.
     */
    public Shape (ShapeConfig config)
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
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        // we must transform the ray into shape space before checking against the config
        if (!(_bounds.intersects(ray) &&
                _config.getIntersection(ray.transform(_transform.invert()), result))) {
            return false;
        }
        // then transform it back if we get a hit
        _transform.transformPointLocal(result);
        return true;
    }

    @Override // documentation inherited
    public boolean intersects (Intersector.Point point)
    {
        return _config.intersects((Intersector.Point)point.transform(_transform.invert()));
    }

    @Override // documentation inherited
    public boolean intersects (Intersector.Segment segment)
    {
        return _config.intersects((Intersector.Segment)segment.transform(_transform.invert()));
    }

    @Override // documentation inherited
    public boolean intersects (Intersector.Quad quad)
    {
        return _config.intersects((Intersector.Quad)quad.transform(_transform.invert()));
    }

    @Override // documentation inherited
    public boolean intersects (Intersector.Circle circle)
    {
        return _config.intersects((Intersector.Circle)circle.transform(_transform.invert()));
    }

    @Override // documentation inherited
    public boolean intersects (Intersector.Capsule capsule)
    {
        return _config.intersects((Intersector.Capsule)capsule.transform(_transform.invert()));
    }

    @Override // documentation inherited
    public boolean intersects (Intersector.Polygon polygon)
    {
        return _config.intersects((Intersector.Polygon)polygon.transform(_transform.invert()));
    }

    @Override // documentation inherited
    public boolean intersects (Intersector.Compound compound)
    {
        return _config.intersects((Intersector.Compound)compound.transform(_transform.invert()));
    }

    @Override // documentation inherited
    protected Rect getLocalBounds ()
    {
        return _config.getBounds();
    }

    /** The configuration of the shape. */
    protected ShapeConfig _config;
}
