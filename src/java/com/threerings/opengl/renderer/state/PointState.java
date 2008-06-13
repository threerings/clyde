//
// $Id$

package com.threerings.opengl.renderer.state;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the point state.
 */
public class PointState extends RenderState
{
    /** The default state. */
    public static final PointState DEFAULT = new PointState(1f);

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static PointState getInstance (float pointSize)
    {
        return getInstance(new PointState(pointSize));
    }

    /**
     * If there is a shared equivalent to the specified state, this method will return the shared
     * state; otherwise, it will simply return the parameter.
     */
    public static PointState getInstance (PointState state)
    {
        if (state.equals(DEFAULT)) {
            return DEFAULT;
        } else {
            return state;
        }
    }

    /**
     * Creates a new point state.
     */
    public PointState (float pointSize)
    {
        _pointSize = pointSize;
    }

    /**
     * Returns the point size.
     */
    public float getPointSize ()
    {
        return _pointSize;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return POINT_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setPointState(_pointSize);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return other instanceof PointState && _pointSize == ((PointState)other)._pointSize;
    }

    /** The point size. */
    protected float _pointSize;
}
