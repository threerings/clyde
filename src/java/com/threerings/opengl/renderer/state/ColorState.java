//
// $Id$

package com.threerings.opengl.renderer.state;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the draw color state.
 */
public class ColorState extends RenderState
{
    /** An opaque white color state. */
    public static final ColorState WHITE = new ColorState(Color4f.WHITE);

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static ColorState getInstance (Color4f color)
    {
        return getInstance(new ColorState(color));
    }

    /**
     * If there is a shared equivalent to the specified state, this method will return the shared
     * state; otherwise, it will simply return the parameter.
     */
    public static ColorState getInstance (ColorState state)
    {
        return state.equals(WHITE) ? WHITE : state;
    }

    /**
     * Creates a new color state with the values in the supplied color object.
     */
    public ColorState (Color4f color)
    {
        _color.set(color);
    }

    /**
     * Creates a new color state.
     */
    public ColorState ()
    {
    }

    /**
     * Returns a reference to the draw color object.
     */
    public Color4f getColor ()
    {
        return _color;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return COLOR_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setColorState(_color);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        ColorState ostate;
        return other instanceof ColorState && _color.equals((ostate = (ColorState)other)._color);
    }

    /** The draw color. */
    protected Color4f _color = new Color4f();
}
