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
}
