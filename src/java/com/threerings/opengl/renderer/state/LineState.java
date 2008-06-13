//
// $Id$

package com.threerings.opengl.renderer.state;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the line state.
 */
public class LineState extends RenderState
{
    /** The default state. */
    public static final LineState DEFAULT = new LineState(1f);

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static LineState getInstance (float lineWidth)
    {
        return getInstance(new LineState(lineWidth));
    }

    /**
     * If there is a shared equivalent to the specified state, this method will return the shared
     * state; otherwise, it will simply return the parameter.
     */
    public static LineState getInstance (LineState state)
    {
        if (state.equals(DEFAULT)) {
            return DEFAULT;
        } else {
            return state;
        }
    }

    /**
     * Creates a new line state.
     */
    public LineState (float lineWidth)
    {
        _lineWidth = lineWidth;
    }

    /**
     * Returns the line width.
     */
    public float getLineWidth ()
    {
        return _lineWidth;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return LINE_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setLineState(_lineWidth);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return other instanceof LineState && _lineWidth == ((LineState)other)._lineWidth;
    }

    /** The line width. */
    protected float _lineWidth;
}
