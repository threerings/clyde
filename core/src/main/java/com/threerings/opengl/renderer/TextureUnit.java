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

import org.lwjgl.opengl.ARBTextureEnvCombine;
import org.lwjgl.opengl.GL11;

import com.threerings.math.Transform3D;
import com.threerings.math.Vector4f;

/**
 * Represents the state of a single texture unit.
 */
public class TextureUnit
{
    /** The texture bound to this unit. */
    public Texture texture;

    /** The texture environment mode. */
    public int envMode = GL11.GL_MODULATE;

    /** The texture environment color. */
    public Color4f envColor = new Color4f(0f, 0f, 0f, 0f);

    /** The RGB combine mode. */
    public int rgbCombine = GL11.GL_MODULATE;

    /** The alpha combine mode. */
    public int alphaCombine = GL11.GL_MODULATE;

    /** The first RGB combine source. */
    public int rgbSource0 = GL11.GL_TEXTURE;

    /** The second RGB combine source. */
    public int rgbSource1 = ARBTextureEnvCombine.GL_PREVIOUS_ARB;

    /** The third RGB combine source. */
    public int rgbSource2 = ARBTextureEnvCombine.GL_CONSTANT_ARB;

    /** The first alpha combine source. */
    public int alphaSource0 = GL11.GL_TEXTURE;

    /** The second alpha combine source. */
    public int alphaSource1 = ARBTextureEnvCombine.GL_PREVIOUS_ARB;

    /** The third alpha combine source. */
    public int alphaSource2 = ARBTextureEnvCombine.GL_CONSTANT_ARB;

    /** The first RGB combine operand. */
    public int rgbOperand0 = GL11.GL_SRC_COLOR;

    /** The second RGB combine operand. */
    public int rgbOperand1 = GL11.GL_SRC_COLOR;

    /** The third RGB combine operand. */
    public int rgbOperand2 = GL11.GL_SRC_ALPHA;

    /** The first alpha combine operand. */
    public int alphaOperand0 = GL11.GL_SRC_ALPHA;

    /** The second alpha combine operand. */
    public int alphaOperand1 = GL11.GL_SRC_ALPHA;

    /** The third alpha combine operand. */
    public int alphaOperand2 = GL11.GL_SRC_ALPHA;

    /** The RGB combine scale. */
    public float rgbScale = 1f;

    /** The alpha combine scale. */
    public float alphaScale = 1f;

    /** The texture level of detail bias. */
    public float lodBias;

    /** The s texture coordinate generation mode (-1 for disabled). */
    public int genModeS = -1;

    /** The s texture coordinate generation plane. */
    public Vector4f genPlaneS = new Vector4f(1f, 0f, 0f, 0f);

    /** The t texture coordinate generation mode (-1 for disabled). */
    public int genModeT = -1;

    /** The t texture coordinate generation plane. */
    public Vector4f genPlaneT = new Vector4f(0f, 1f, 0f, 0f);

    /** The r texture coordinate generation mode (-1 for disabled). */
    public int genModeR = -1;

    /** The r texture coordinate generation plane. */
    public Vector4f genPlaneR = new Vector4f(0f, 0f, 0f, 0f);

    /** The q texture coordinate generation mode (-1 for disabled). */
    public int genModeQ = -1;

    /** The q texture coordinate generation plane. */
    public Vector4f genPlaneQ = new Vector4f(0f, 0f, 0f, 0f);

    /** The texture transform. */
    public Transform3D transform = new Transform3D();

    /** Set when the state has changed and must be reapplied. */
    public boolean dirty;

    /**
     * Creates a simple texture unit.
     */
    public TextureUnit (Texture texture)
    {
        this.texture = texture;
    }

    /**
     * Creates an empty texture unit.
     */
    public TextureUnit ()
    {
    }

    /**
     * Sets the texture and the dirty flag.
     */
    public void setTexture (Texture texture)
    {
        this.texture = texture;
        dirty = true;
    }

    /**
     * Checks whether all of the unit's gen modes equal the supplied mode.
     */
    public boolean allGenModesEqual (int mode)
    {
        return genModeS == mode && genModeT == mode && genModeR == mode && genModeQ == mode;
    }

    /**
     * Checks whether any of the unit's gen modes equal the supplied mode.
     */
    public boolean anyGenModesEqual (int mode)
    {
        return genModeS == mode || genModeT == mode || genModeR == mode || genModeQ == mode;
    }
}
