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

import com.samskivert.util.HashIntMap;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the color mask state.
 */
public class ColorMaskState extends RenderState
{
    /** All colors enabled. */
    public static final ColorMaskState ALL = getInstance(true, true, true, true);

    /** All colors disabled. */
    public static final ColorMaskState NONE = getInstance(false, false, false, false);

    /**
     * Returns a color mask state instance with the supplied parameters.  All requests for
     * instances with the same parameters will return the same object.
     */
    public static ColorMaskState getInstance (
        boolean red, boolean green, boolean blue, boolean alpha)
    {
        if (_instances == null) {
            _instances = new HashIntMap<ColorMaskState>();
        }
        int key = (red ? 1 : 0) << 3 | (green ? 1 : 0) << 2 |
            (blue ? 1 : 0) << 1 | (alpha ? 1 : 0) << 0;
        ColorMaskState instance = _instances.get(key);
        if (instance == null) {
            _instances.put(key, instance = new ColorMaskState(red, green, blue, alpha));
        }
        return instance;
    }

    /**
     * Creates a new color mask state with the supplied parameters.
     */
    public ColorMaskState (boolean red, boolean green, boolean blue, boolean alpha)
    {
        _red = red;
        _green = green;
        _blue = blue;
        _alpha = alpha;
    }

    /**
     * Checks whether the mask allows writing red values.
     */
    public boolean getRed ()
    {
        return _red;
    }

    /**
     * Checks whether the mask allows writing green values.
     */
    public boolean getGreen ()
    {
        return _green;
    }

    /**
     * Checks whether the mask allows writing blue values.
     */
    public boolean getBlue ()
    {
        return _blue;
    }

    /**
     * Checks whether the mask allows writing alpha values.
     */
    public boolean getAlpha ()
    {
        return _alpha;
    }

    @Override
    public int getType ()
    {
        return COLOR_MASK_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setColorMaskState(_red, _green, _blue, _alpha);
    }

    /** Whether each component is enabled for writing. */
    protected boolean _red, _green, _blue, _alpha;

    /** Shared instances. */
    protected static HashIntMap<ColorMaskState> _instances;
}
