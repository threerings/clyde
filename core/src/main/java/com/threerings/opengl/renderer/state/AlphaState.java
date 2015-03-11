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

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the alpha testing and blending state.
 */
public class AlphaState extends RenderState
{
    /** An alpha state for completely opaque objects. */
    public static final AlphaState OPAQUE =
        new AlphaState(GL11.GL_ALWAYS, 0f, GL11.GL_ONE, GL11.GL_ZERO);

    /** An alpha state for masked opaque objects. */
    public static final AlphaState MASKED =
        new AlphaState(GL11.GL_EQUAL, 1f, GL11.GL_ONE, GL11.GL_ZERO);

    /** An alpha state for translucent objects. */
    public static final AlphaState TRANSLUCENT =
        new AlphaState(GL11.GL_ALWAYS, 0f, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    /** An alpha state for additive objects. */
    public static final AlphaState ADDITIVE =
        new AlphaState(GL11.GL_ALWAYS, 0f, GL11.GL_ONE, GL11.GL_ONE);

    /** An alpha state for objects with premultiplied alpha. */
    public static final AlphaState PREMULTIPLIED =
        new AlphaState(GL11.GL_ALWAYS, 0f, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static AlphaState getInstance (
        int alphaTestFunc, float alphaTestRef, int srcBlendFactor, int destBlendFactor)
    {
        return getInstance(new AlphaState(
            alphaTestFunc, alphaTestRef, srcBlendFactor, destBlendFactor));
    }

    /**
     * If there is a shared equivalent to the specified state, this method will return the shared
     * state; otherwise, it will simply return the parameter.
     */
    public static AlphaState getInstance (AlphaState state)
    {
        if (state.equals(OPAQUE)) {
            return OPAQUE;
        } else if (state.equals(MASKED)) {
            return MASKED;
        } else if (state.equals(TRANSLUCENT)) {
            return TRANSLUCENT;
        } else if (state.equals(ADDITIVE)) {
            return ADDITIVE;
        } else if (state.equals(PREMULTIPLIED)) {
            return PREMULTIPLIED;
        } else {
            return state;
        }
    }

    /**
     * Returns an alpha state that only passes fragments with alpha values greater than or equal to
     * the supplied value, then blends them with the destination.  All requests for instances with
     * the same value will return the same object.
     */
    public static AlphaState getTestInstance (float reference)
    {
        if (reference == 0f) {
            return PREMULTIPLIED;
        } else if (reference == 1f) {
            return MASKED;
        }
        AlphaState instance = _testInstances.get(reference);
        if (instance == null) {
            _testInstances.put(reference, instance = new AlphaState(
                GL11.GL_GEQUAL, reference, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA));
        }
        return instance;
    }

    /**
     * Creates a new alpha state.
     */
    public AlphaState (
        int alphaTestFunc, float alphaTestRef, int srcBlendFactor, int destBlendFactor)
    {
        _alphaTestFunc = alphaTestFunc;
        _alphaTestRef = alphaTestRef;
        _srcBlendFactor = srcBlendFactor;
        _destBlendFactor = destBlendFactor;
    }

    /**
     * Returns the alpha test function.
     */
    public int getAlphaTestFunc ()
    {
        return _alphaTestFunc;
    }

    /**
     * Returns the alpha test reference value.
     */
    public float getAlphaTestRef ()
    {
        return _alphaTestRef;
    }

    /**
     * Returns the source blend factor.
     */
    public int getSrcBlendFactor ()
    {
        return _srcBlendFactor;
    }

    /**
     * Returns the destination blend factor.
     */
    public int getDestBlendFactor ()
    {
        return _destBlendFactor;
    }

    @Override
    public int getType ()
    {
        return ALPHA_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setAlphaState(_alphaTestFunc, _alphaTestRef, _srcBlendFactor, _destBlendFactor);
    }

    @Override
    public boolean equals (Object other)
    {
        AlphaState ostate;
        return other instanceof AlphaState &&
            _alphaTestFunc == (ostate = (AlphaState)other)._alphaTestFunc &&
            _alphaTestRef == ostate._alphaTestRef &&
            _srcBlendFactor == ostate._srcBlendFactor &&
            _destBlendFactor == ostate._destBlendFactor;
    }

    @Override
    public int hashCode ()
    {
        int result = _alphaTestFunc;
        result = 31 * result + Float.floatToIntBits(_alphaTestRef);
        result = 31 * result + _srcBlendFactor;
        result = 31 * result + _destBlendFactor;
        return result;
    }

    /** The alpha test function. */
    protected int _alphaTestFunc;

    /** The reference value for alpha testing. */
    protected float _alphaTestRef;

    /** The source blend factor. */
    protected int _srcBlendFactor;

    /** The destination blend factor. */
    protected int _destBlendFactor;

    /** Shared instances mapped by test threshold. */
    protected static HashMap<Float, AlphaState> _testInstances = new HashMap<Float, AlphaState>();
}
