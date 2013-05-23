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

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the light state.
 */
public class LightState extends RenderState
{
    /** A state that disables lighting (the default). */
    public static final LightState DISABLED = new LightState(null, Color4f.DARK_GRAY);

    /**
     * Creates a new light state.
     */
    public LightState (Light[] lights, Color4f globalAmbient)
    {
        _lights = lights;
        _globalAmbient.set(globalAmbient);
    }

    /**
     * Returns a reference to the array of lights.
     */
    public Light[] getLights ()
    {
        return _lights;
    }

    /**
     * Sets the reference to the global ambient intensity.
     */
    public void setGlobalAmbient (Color4f color)
    {
        _globalAmbient = color;
    }

    /**
     * Returns a reference to the global ambient intensity.
     */
    public Color4f getGlobalAmbient ()
    {
        return _globalAmbient;
    }

    @Override
    public int getType ()
    {
        return LIGHT_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setLightState(_lights, _globalAmbient);
    }

    /** The states of the lights. */
    protected Light[] _lights;

    /** The global ambient intensity. */
    protected Color4f _globalAmbient = new Color4f();
}
