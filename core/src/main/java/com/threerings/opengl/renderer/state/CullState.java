//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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

    @Override
    public int getType ()
    {
        return CULL_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setCullState(_cullFace);
    }

    @Override
    public boolean equals (Object other)
    {
        return other instanceof CullState &&
            _cullFace == ((CullState)other)._cullFace;
    }

    @Override
    public int hashCode ()
    {
        return _cullFace;
    }

    /** The cull face (or -1 if disabled). */
    protected int _cullFace = -1;

    /** Shared instances. */
    protected static HashIntMap<CullState> _instances;
}
