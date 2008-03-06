//
// $Id$

package com.threerings.opengl.renderer.state;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the back-face culling state.
 */
public class CullState extends RenderState
{
    /** A state that enables back-face culling. */
    public static final CullState ENABLED = new CullState(true);

    /** A state that disables back-face culling. */
    public static final CullState DISABLED = new CullState(false);

    /**
     * Creates a new back-face culling state.
     */
    public CullState (boolean cullFaceEnabled)
    {
        _cullFaceEnabled = cullFaceEnabled;
    }

    /**
     * Checks whether back-face culling is enabled.
     */
    public boolean isCullFaceEnabled ()
    {
        return _cullFaceEnabled;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return CULL_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setCullState(_cullFaceEnabled);
    }

    /** Whether or not back-face culling is enabled. */
    protected boolean _cullFaceEnabled;
}
