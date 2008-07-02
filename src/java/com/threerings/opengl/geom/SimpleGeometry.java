//
// $Id$

package com.threerings.opengl.geom;

import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.state.ArrayState;

/**
 * A simple geometry implementation that returns the same array state and draw command for all
 * passes.
 */
public class SimpleGeometry extends Geometry
{
    /**
     * For convenience, creates a geometry instance that calls the {@link #draw} method (which
     * must be overridden) to draw the geometry in immediate mode.
     */
    public SimpleGeometry ()
    {
        this(false, 0);
    }

    /**
     * For convenience, creates a geometry instance that calls the {@link #draw} method (which
     * must be overridden) to draw the geometry in immediate mode.
     *
     * @param modifiesColorState whether or not the draw method modifies the color state.
     * @param primitiveCount the primitive count to report to the renderer.
     */
    public SimpleGeometry (final boolean modifiesColorState, final int primitiveCount)
    {
        _drawCommand = new DrawCommand() {
            public boolean call () {
                draw();
                return modifiesColorState;
            }
            public int getPrimitiveCount () {
                return primitiveCount;
            }
        };
    }

    /**
     * Creates a geometry instance with the specified draw command and no array state.
     */
    public SimpleGeometry (DrawCommand drawCommand)
    {
        _drawCommand = drawCommand;
    }

    /**
     * Creates a geometry instance with the specified array state and draw command.
     */
    public SimpleGeometry (ArrayState arrayState, DrawCommand drawCommand)
    {
        _arrayState = arrayState;
        _drawCommand = drawCommand;
    }

    @Override // documentation inherited
    public ArrayState getArrayState (int pass)
    {
        return _arrayState;
    }

    @Override // documentation inherited
    public DrawCommand getDrawCommand (int pass)
    {
        return _drawCommand;
    }

    /**
     * Draws the geometry in immediate mode.
     */
    protected void draw ()
    {
        throw new RuntimeException("Override draw method to draw geometry.");
    }

    /** The array state for all passes. */
    protected ArrayState _arrayState;

    /** The draw command for all passes. */
    protected DrawCommand _drawCommand;
}
