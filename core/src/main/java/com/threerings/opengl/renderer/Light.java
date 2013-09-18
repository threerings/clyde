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

package com.threerings.opengl.renderer;

import com.threerings.math.Vector3f;
import com.threerings.math.Vector4f;

/**
 * Represents the state of a single light.
 */
public class Light
{
    /** The basic light types. */
    public enum Type { DIRECTIONAL, POINT, SPOT };

    /** The ambient light intensity. */
    public Color4f ambient = new Color4f(0f, 0f, 0f, 1f);

    /** The diffuse light intensity. */
    public Color4f diffuse = new Color4f(1f, 1f, 1f, 1f);

    /** The specular light intensity. */
    public Color4f specular = new Color4f(1f, 1f, 1f, 1f);

    /** The position or direction of the light. */
    public Vector4f position = new Vector4f(0f, 0f, 1f, 0f);

    /** The light's spot direction. */
    public Vector3f spotDirection = new Vector3f(0f, 0f, -1f);

    /** The light's spot exponent. */
    public float spotExponent;

    /** The light's spot cutoff. */
    public float spotCutoff = 180f;

    /** The light's constant attenuation. */
    public float constantAttenuation = 1f;

    /** The light's linear attenuation. */
    public float linearAttenuation;

    /** The light's quadratic attenuation. */
    public float quadraticAttenuation;

    /** Set when the light has changed and must be reapplied. */
    public boolean dirty;

    /** A hint as to whether or not this light should cast shadows. */
    public boolean castsShadows;

    /**
     * Determines whether this light is "compatible" with the specified other.  Lights are
     * compatible if they have the same type (directional, point, spot) and hints.
     */
    public boolean isCompatible (Light olight)
    {
        return getType() == olight.getType() && castsShadows == olight.castsShadows;
    }

    /**
     * Returns the basic type of this light.
     */
    public Type getType ()
    {
        return (position.w == 0f) ? Type.DIRECTIONAL :
            (spotCutoff == 180f ? Type.POINT : Type.SPOT);
    }
}
