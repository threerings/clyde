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

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the fog state.
 */
public class FogState extends RenderState
{
    /** A state that disables the fog. */
    public static final FogState DISABLED = new FogState(-1, 0f, Color4f.WHITE);

    /**
     * Creates a new fog state.
     */
    public FogState (int fogMode, float fogDensity, Color4f fogColor)
    {
        _fogMode = fogMode;
        _fogDensity = fogDensity;
        _fogColor.set(fogColor);
    }

    /**
     * Creates a new linear fog state.
     */
    public FogState (int fogMode, float fogStart, float fogEnd, Color4f fogColor)
    {
        _fogMode = fogMode;
        _fogStart = fogStart;
        _fogEnd = fogEnd;
        _fogColor.set(fogColor);
    }

    /**
     * Returns the fog mode.
     */
    public int getFogMode ()
    {
        return _fogMode;
    }

    /**
     * Returns the fog density.
     */
    public float getFogDensity ()
    {
        return _fogDensity;
    }

    /**
     * Returns the fog start distance.
     */
    public float getFogStart ()
    {
        return _fogStart;
    }

    /**
     * Returns the fog end distance.
     */
    public float getFogEnd ()
    {
        return _fogEnd;
    }

    /**
     * Returns a reference to the fog color.
     */
    public Color4f getFogColor ()
    {
        return _fogColor;
    }

    @Override
    public int getType ()
    {
        return FOG_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        if (_fogMode == GL11.GL_LINEAR) {
            renderer.setFogState(_fogMode, _fogStart, _fogEnd, _fogColor);
        } else {
            renderer.setFogState(_fogMode, _fogDensity, _fogColor);
        }
    }

    /** The fog mode (or -1 if disabled). */
    protected int _fogMode;

    /** The fog density. */
    protected float _fogDensity;

    /** The fog start distance. */
    protected float _fogStart;

    /** The fog end distance. */
    protected float _fogEnd;

    /** The fog color. */
    protected Color4f _fogColor = new Color4f();
}
