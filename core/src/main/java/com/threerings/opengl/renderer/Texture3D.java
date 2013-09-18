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
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

import com.threerings.opengl.util.GlUtil;

/**
 * A three-dimensional texture.
 */
public class Texture3D extends Texture
{
    /**
     * Creates a new texture.
     */
    public Texture3D (Renderer renderer)
    {
        super(renderer, GL12.GL_TEXTURE_3D);
    }

    /**
     * Sets this texture to an empty image with the specified format and dimensions.
     *
     * @param mipmap if true, generate a complete mipmap chain.
     */
    public void setImage (
        int format, int width, int height, int depth, boolean border, boolean mipmap)
    {
        setImage(0, format, width, height, depth, border);
        if (mipmap) {
            for (int ww = _width/2, hh = _height/2, dd = _depth/2, ll = 1;
                    ww > 0 || hh > 0 || dd > 0; ll++, ww /= 2, hh /= 2, dd /= 2) {
                setImage(ll, format, Math.max(ww, 1), Math.max(hh, 1), Math.max(dd, 1), border);
            }
        }
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImage (int level, int format, int width, int height, int depth, boolean border)
    {
        setImage(level, format, width, height, depth, border,
            getTransferFormat(format), GL11.GL_UNSIGNED_BYTE, null);
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImage (
        int level, int format, int width, int height, int depth, boolean border,
        int dformat, int dtype, ByteBuffer data)
    {
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            width = GlUtil.nextPowerOfTwo(width);
            height = GlUtil.nextPowerOfTwo(height);
            depth = GlUtil.nextPowerOfTwo(depth);
        }
        if (level == 0) {
            _format = format;
            _width = width;
            _height = height;
            _depth = depth;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        GL12.glTexImage3D(
            GL12.GL_TEXTURE_3D, level, format, width + ib2, height + ib2, depth + ib2, ib,
            dformat, dtype, data);
        setBytes(level, (data == null) ? (width*height*depth*4) : data.remaining());
    }

    /**
     * Sets this texture to the supplied image.
     *
     * @param mipmap if true, generate a complete mipmap chain.
     */
    public void setImages (
        int format, boolean border, BufferedImage image, int sdivs, int tdivs,
        int depth, boolean premultiply, boolean rescale, boolean mipmap)
    {
        setImage(0, format, border, image, sdivs, tdivs, depth, premultiply, rescale);
        if (mipmap) {
            for (int ww = _width/2, hh = _height/2, dd = _depth/2, ll = 1;
                    ww > 0 || hh > 0 || dd > 0; ll++, ww /= 2, hh /= 2, dd /= 2) {
                // TODO
            }
        }
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImage (
        int level, int format, boolean border, BufferedImage image, int sdivs, int tdivs,
        int depth, boolean premultiply, boolean rescale)
    {
        int width = image.getWidth() / sdivs, height = image.getHeight() / tdivs;
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            width = GlUtil.nextPowerOfTwo(width);
            height = GlUtil.nextPowerOfTwo(height);
            depth = GlUtil.nextPowerOfTwo(depth);
        }
        if (level == 0) {
            _format = format;
            _width = width;
            _height = height;
            _depth = depth;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0;
        // TODO
    }

    /**
     * Copies part of the read buffer to the texture.
     */
    public void copySubImage (
        int level, int xoffset, int yoffset, int zoffset, int x, int y, int width, int height)
    {
        _renderer.setTexture(this);
        GL12.glCopyTexSubImage3D(
            GL12.GL_TEXTURE_3D, level, xoffset, yoffset, zoffset, x, y, width, height);
    }

    @Override
    public int getWidth ()
    {
        return _width;
    }

    @Override
    public int getHeight ()
    {
        return _height;
    }

    @Override
    public int getDepth ()
    {
        return _depth;
    }

    /** The dimensions of the texture. */
    protected int _width, _height, _depth;
}
