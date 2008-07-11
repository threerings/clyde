//
// $Id$

package com.threerings.opengl.geom;

import com.threerings.math.Box;

import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.state.ArrayState;

/**
 * Represents a geometry instance.
 */
public abstract class Geometry
{
    /**
     * Returns a reference to the model space bounds of the geometry.
     */
    public Box getBounds ()
    {
        return _bounds;
    }

    /**
     * Returns the array state for the specified pass.
     */
    public abstract ArrayState getArrayState (int pass);

    /**
     * Returns the draw command for the specified pass.
     */
    public abstract DrawCommand getDrawCommand (int pass);

    /**
     * Updates the state of the geometry.
     */
    public void update ()
    {
        // nothing by default
    }

    /** The model space bounds of the geometry. */
    protected Box _bounds = new Box();
}
