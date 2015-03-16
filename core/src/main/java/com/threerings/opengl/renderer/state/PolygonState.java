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

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the polygon state.
 */
public class PolygonState extends RenderState
{
    /** The default state. */
    public static final PolygonState DEFAULT =
        new PolygonState(GL11.GL_FILL, GL11.GL_FILL, 0f, 0f);

    /** Simple wireframe state. */
    public static final PolygonState WIREFRAME =
        new PolygonState(GL11.GL_LINE, GL11.GL_LINE, 0f, 0f);

    /**
     * If there is a shared instance with the supplied parameters, returns a reference to it;
     * otherwise, returns a new state with the parameters.
     */
    public static PolygonState getInstance (
        int frontPolygonMode, int backPolygonMode, float polygonOffsetFactor,
        float polygonOffsetUnits)
    {
        return getInstance(new PolygonState(
            frontPolygonMode, backPolygonMode, polygonOffsetFactor, polygonOffsetUnits));
    }

    /**
     * If there is a shared equivalent to the specified state, this method will return the shared
     * state; otherwise, it will simply return the parameter.
     */
    public static PolygonState getInstance (PolygonState state)
    {
        if (state.equals(DEFAULT)) {
            return DEFAULT;
        } else if (state.equals(WIREFRAME)) {
            return WIREFRAME;
        } else {
            return state;
        }
    }

    /**
     * Creates a new polygon state.
     */
    public PolygonState (
        int frontPolygonMode, int backPolygonMode, float polygonOffsetFactor,
        float polygonOffsetUnits)
    {
        _frontPolygonMode = frontPolygonMode;
        _backPolygonMode = backPolygonMode;
        _polygonOffsetFactor = polygonOffsetFactor;
        _polygonOffsetUnits = polygonOffsetUnits;
    }

    /**
     * Returns the front-facing polygon mode.
     */
    public int getFrontPolygonMode ()
    {
        return _frontPolygonMode;
    }

    /**
     * Returns the back-facing polygon mode.
     */
    public int getBackPolygonMode ()
    {
        return _backPolygonMode;
    }

    /**
     * Returns the proportional polygon offset.
     */
    public float getPolygonOffsetFactor ()
    {
        return _polygonOffsetFactor;
    }

    /**
     * Returns the constant polygon offset.
     */
    public float getPolygonOffsetUnits ()
    {
        return _polygonOffsetUnits;
    }

    @Override
    public int getType ()
    {
        return POLYGON_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setPolygonState(
            _frontPolygonMode, _backPolygonMode, _polygonOffsetFactor, _polygonOffsetUnits);
    }

    @Override
    public boolean equals (Object other)
    {
        PolygonState ostate;
        return other instanceof PolygonState &&
            _frontPolygonMode == (ostate = (PolygonState)other)._frontPolygonMode &&
            _backPolygonMode == ostate._backPolygonMode &&
            _polygonOffsetFactor == ostate._polygonOffsetFactor &&
            _polygonOffsetUnits == ostate._polygonOffsetUnits;
    }

    @Override
    public int hashCode ()
    {
        int result = _frontPolygonMode;
        result = 31 * result + _backPolygonMode;
        result = 31 * result + Float.floatToIntBits(_polygonOffsetFactor);
        result = 31 * result + Float.floatToIntBits(_polygonOffsetUnits);
        return result;
    }

    /** The front polygon mode. */
    protected int _frontPolygonMode;

    /** The back polygon mode. */
    protected int _backPolygonMode;

    /** The proportional polygon offset. */
    protected float _polygonOffsetFactor;

    /** The constant polygon offset. */
    protected float _polygonOffsetUnits;
}

