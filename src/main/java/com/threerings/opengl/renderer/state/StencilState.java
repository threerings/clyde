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

import com.threerings.util.ArrayKey;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the stencil state.
 */
public class StencilState extends RenderState
{
    /** Disables stencil testing/writing. */
    public static final StencilState DISABLED = getInstance(
        GL11.GL_ALWAYS, 0, 0x7FFFFFFF, GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP, 0x7FFFFFFF);

    /**
     * Returns a stencil state instance with the supplied parameters.  All requests for
     * instances with the same parameters will return the same object.
     */
    public static StencilState getInstance (
        int stencilTestFunc, int stencilTestRef, int stencilTestMask,
        int stencilFailOp, int stencilDepthFailOp, int stencilPassOp,
        int stencilWriteMask)
    {
        if (_instances == null) {
            _instances = new HashMap<ArrayKey, StencilState>();
        }
        ArrayKey key = new ArrayKey(
            stencilTestFunc, stencilTestRef, stencilTestMask,
            stencilFailOp, stencilDepthFailOp, stencilPassOp,
            stencilWriteMask);
        StencilState instance = _instances.get(key);
        if (instance == null) {
            _instances.put(key, instance = new StencilState(
                stencilTestFunc, stencilTestRef, stencilTestMask,
                stencilFailOp, stencilDepthFailOp, stencilPassOp,
                stencilWriteMask));
        }
        return instance;
    }

    /**
     * Creates a new stencil state with the supplied parameters.
     */
    public StencilState (
        int stencilTestFunc, int stencilTestRef, int stencilTestMask,
        int stencilFailOp, int stencilDepthFailOp, int stencilPassOp,
        int stencilWriteMask)
    {
        _stencilTestFunc = stencilTestFunc;
        _stencilTestRef = stencilTestRef;
        _stencilTestMask = stencilTestMask;
        _stencilFailOp = stencilFailOp;
        _stencilDepthFailOp = stencilDepthFailOp;
        _stencilPassOp = stencilPassOp;
        _stencilWriteMask = stencilWriteMask;
    }

    /**
     * Returns the stencil test function.
     */
    public int getStencilTestFunc ()
    {
        return _stencilTestFunc;
    }

    /**
     * Returns the stencil test reference value.
     */
    public int getStencilTestRef ()
    {
        return _stencilTestRef;
    }

    /**
     * Returns the stencil test mask.
     */
    public int getStencilTestMask ()
    {
        return _stencilTestMask;
    }

    /**
     * Returns the action to take when the fragment fails the stencil test.
     */
    public int getStencilFailOp ()
    {
        return _stencilFailOp;
    }

    /**
     * Returns the action to take when the fragment passes the stencil test, but fails the depth
     * test.
     */
    public int getStencilDepthFailOp ()
    {
        return _stencilDepthFailOp;
    }

    /**
     * Returns the action to take when the fragment passes both the stencil test and the depth
     * test.
     */
    public int getStencilPassOp ()
    {
        return _stencilPassOp;
    }

    /**
     * Returns the stencil write mask.
     */
    public int getStencilWriteMask ()
    {
        return _stencilWriteMask;
    }

    @Override
    public int getType ()
    {
        return STENCIL_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setStencilState(
            _stencilTestFunc, _stencilTestRef, _stencilTestMask,
            _stencilFailOp, _stencilDepthFailOp, _stencilPassOp,
            _stencilWriteMask);
    }

    /** The stencil test function. */
    protected int _stencilTestFunc;

    /** The reference value for stencil testing. */
    protected int _stencilTestRef;

    /** The mask applied to both the reference value and the stencil value when testing. */
    protected int _stencilTestMask;

    /** The operation to take when the incoming value fails the stencil test. */
    protected int _stencilFailOp;

    /** The operation to take when the incoming value passes the stencil test but fails the
     * depth test. */
    protected int _stencilDepthFailOp;

    /** The operation to take when the incoming value passes both the stencil test and the
     * depth test. */
    protected int _stencilPassOp;

    /** The stencil write mask. */
    protected int _stencilWriteMask;

    /** Shared instances. */
    protected static HashMap<ArrayKey, StencilState> _instances;
}
