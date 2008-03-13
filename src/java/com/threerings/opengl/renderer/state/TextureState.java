//
// $Id$

package com.threerings.opengl.renderer.state;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.TextureUnit;

/**
 * Contains the texture state.
 */
public class TextureState extends RenderState
{
    /** A state that disables texturing. */
    public static final TextureState DISABLED = new TextureState(null);

    /**
     * Creates a new texture state.
     */
    public TextureState (TextureUnit[] units)
    {
        _units = units;
    }

    /**
     * Returns a reference to the array of texture units.
     */
    public TextureUnit[] getUnits ()
    {
        return _units;
    }

    /**
     * Returns the unit at the specified index, if any.
     */
    public TextureUnit getUnit (int idx)
    {
        return (_units == null || _units.length <= idx) ? null : _units[idx];
    }

    @Override // documentation inherited
    public int getType ()
    {
        return TEXTURE_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setTextureState(_units);
    }

    /** The states of the texture units. */
    protected TextureUnit[] _units;
}
