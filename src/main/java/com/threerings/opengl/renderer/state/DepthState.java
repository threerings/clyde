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

import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import com.samskivert.util.Tuple;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the depth buffer testing/writing state.
 */
public class DepthState extends RenderState
{
    /** A depth state for testing, but not writing to, the depth buffer. */
    public static final DepthState TEST = getInstance(GL11.GL_LEQUAL, false);

    /** A depth state for writing to, but not testing, the depth buffer. */
    public static final DepthState WRITE = getInstance(GL11.GL_ALWAYS, true);

    /** A depth state for testing and writing to the depth buffer. */
    public static final DepthState TEST_WRITE = getInstance(GL11.GL_LEQUAL, true);

    /** A depth state for neither testing nor writing to the depth buffer. */
    public static final DepthState DISABLED = getInstance(GL11.GL_ALWAYS, false);

    /**
     * Returns a depth state instance with the supplied parameters.  All requests for instances
     * with the same parameters will return the same object.
     */
    public static DepthState getInstance (int depthTestFunc, boolean depthMask)
    {
        if (_instances == null) {
            _instances = new HashMap<Tuple<Integer, Boolean>, DepthState>();
        }
        Tuple<Integer, Boolean> key = new Tuple<Integer, Boolean>(depthTestFunc, depthMask);
        DepthState instance = _instances.get(key);
        if (instance == null) {
            _instances.put(key, instance = new DepthState(depthTestFunc, depthMask));
        }
        return instance;
    }

    /**
     * Creates a new depth state with the supplied parameters.
     */
    public DepthState (int depthTestFunc, boolean depthMask)
    {
        _depthTestFunc = depthTestFunc;
        _depthMask = depthMask;
    }

    /**
     * Returns the depth test function.
     */
    public int getDepthTestFunc ()
    {
        return _depthTestFunc;
    }

    /**
     * Returns the depth mask value.
     */
    public boolean getDepthMask ()
    {
        return _depthMask;
    }

    @Override
    public int getType ()
    {
        return DEPTH_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setDepthState(_depthTestFunc, _depthMask);
    }

    /** The depth test function. */
    protected int _depthTestFunc;

    /** Whether or not depth-writing is enabled. */
    protected boolean _depthMask;

    /** Shared instances. */
    protected static HashMap<Tuple<Integer, Boolean>, DepthState> _instances;
}
