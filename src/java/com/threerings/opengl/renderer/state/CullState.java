//
// $Id$

package com.threerings.opengl.renderer.state;

import org.lwjgl.opengl.GL11;

import com.samskivert.util.HashIntMap;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the back-face culling state.
 */
public class CullState extends RenderState
{
    /** A state that disables back-face culling. */
    public static final CullState DISABLED = getInstance(-1);

    /** A state that enables back-face culling. */
    public static final CullState BACK_FACE = getInstance(GL11.GL_BACK);

    /** A state that enabled front-face culling. */
    public static final CullState FRONT_FACE = getInstance(GL11.GL_FRONT);

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static CullState getInstance (int cullFace)
    {
        if (_instances == null) {
            _instances = new HashIntMap<CullState>();
        }
        CullState instance = _instances.get(cullFace);
        if (instance == null) {
            _instances.put(cullFace, instance = new CullState(cullFace));
        }
        return instance;
    }

    /**
     * Creates a new back-face culling state.
     */
    public CullState (int cullFace)
    {
        _cullFace = cullFace;
    }

    /**
     * Returns the cull face constant.
     */
    public int getCullFace ()
    {
        return _cullFace;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return CULL_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setCullState(_cullFace);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return other instanceof CullState &&
            _cullFace == ((CullState)other)._cullFace;
    }

    /** The cull face (or -1 if disabled). */
    protected int _cullFace = -1;

    /** Shared instances. */
    protected static HashIntMap<CullState> _instances;
}
