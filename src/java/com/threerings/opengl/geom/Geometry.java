//
// $Id$

package com.threerings.opengl.geom;

import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.state.ArrayState;

/**
 * Represents a geometry instance.
 */
public abstract class Geometry
{
    /** The coordinate spaces in which geometry may be specified. */
    public enum CoordSpace { OBJECT, WORLD, EYE };

    /**
     * Returns the coordinate space in which the specified pass is given.
     */
    public CoordSpace getCoordSpace (int pass)
    {
        return CoordSpace.OBJECT;
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
     * Checks whether this geometry requires a call to its {@link #update} method before rendering.
     */
    public boolean requiresUpdate ()
    {
        return false;
    }

    /**
     * Updates the state of the geometry.
     */
    public void update ()
    {
        // nothing by default
    }
}
