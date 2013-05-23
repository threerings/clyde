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

import java.awt.image.BufferedImage;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.opengl.util.GlUtil;

/**
 * A one-dimensional texture.
 */
public class Texture1D extends Texture
{
    /**
     * Creates a new texture.
     */
    public Texture1D (Renderer renderer)
    {
        super(renderer, GL11.GL_TEXTURE_1D);
    }

    /**
     * Sets this texture to an empty image with the specified format and dimension.
     *
     * @param mipmap if true, generate a complete mipmap chain.
     */
    public void setImage (int format, int width, boolean border, boolean mipmap)
    {
        setImage(0, format, width, border);
        if (mipmap) {
            for (int ww = _width/2, ll = 1; ww > 0; ll++, ww /= 2) {
                setImage(ll, format, ww, border);
            }
        }
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImage (int level, int format, int width, boolean border)
    {
        setImage(level, format, width, border, getTransferFormat(format),
            GL11.GL_UNSIGNED_BYTE, null);
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImage (
        int level, int format, int width, boolean border, int dformat, int dtype, ByteBuffer data)
    {
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            width = GlUtil.nextPowerOfTwo(width);
        }
        if (level == 0) {
            _format = format;
            _width = width;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        GL11.glTexImage1D(
            GL11.GL_TEXTURE_1D, level, format, width + ib2, ib, dformat, dtype, data);
        setBytes(level, (data == null) ? (width*4) : data.remaining());
    }

    /**
     * Sets this texture to the supplied image.
     *
     * @param mipmap if true, generate a complete mipmap chain.
     */
    public void setImage (
        int format, boolean border, BufferedImage image, boolean premultiply,
        boolean rescale, boolean mipmap)
    {
        setImage(0, format, border, image, premultiply, rescale);
        if (mipmap) {
            for (int ww = _width/2, ll = 1; ww > 0; ll++, ww /= 2) {
                setImage(ll, format, border, image = halveImage(image), premultiply, rescale);
            }
        }
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImage (
        int level, int format, boolean border, BufferedImage image,
        boolean premultiply, boolean rescale)
    {
        int width = image.getWidth();
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            width = GlUtil.nextPowerOfTwo(width);
        }
        if (level == 0) {
            _format = format;
            _width = width;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        ByteBuffer data = getData(image, premultiply, width, 1, rescale);
        GL11.glTexImage1D(
            GL11.GL_TEXTURE_1D, level, format, width + ib2, ib,
            getFormat(image), GL11.GL_UNSIGNED_BYTE, data);
        setBytes(level, data.remaining());
    }

    /**
     * Copies part of the read buffer to the texture.
     */
    public void copySubImage (int level, int xoffset, int x, int y, int width)
    {
        _renderer.setTexture(this);
        GL11.glCopyTexSubImage1D(GL11.GL_TEXTURE_1D, level, xoffset, x, y, width);
    }

    @Override
    public int getWidth ()
    {
        return _width;
    }

    @Override
    public int getHeight ()
    {
        return 1;
    }

    /** The dimension of the texture. */
    protected int _width;
}
