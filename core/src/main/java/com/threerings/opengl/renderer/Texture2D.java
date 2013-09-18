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

import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.ARBTextureRectangle;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;

import com.threerings.opengl.util.GlUtil;

/**
 * A two-dimensional texture.
 */
public class Texture2D extends Texture
{
    /**
     * Creates a new texture.
     */
    public Texture2D (Renderer renderer)
    {
        this(renderer, false);
    }

    /**
     * Creates a new texture.
     *
     * @param rectangle if true, create a texture for the rectangle target.  Rectangular textures
     * are not limited to square powers-of-two dimensions and use non-normalized texture
     * coordinates.
     */
    public Texture2D (Renderer renderer, boolean rectangle)
    {
        super(renderer,
            rectangle ? ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB : GL11.GL_TEXTURE_2D);
        if (rectangle) {
            _minFilter = GL11.GL_LINEAR;
            _wrapS = _wrapT = GL12.GL_CLAMP_TO_EDGE;
        }
    }

    /**
     * Determines whether this texture is associated with the rectangle target.
     */
    public boolean isRectangle ()
    {
        return _target == ARBTextureRectangle.GL_TEXTURE_RECTANGLE_ARB;
    }

    /**
     * Sets this texture to an empty image with the specified format and dimensions.
     *
     * @param mipmap if true, generate a complete mipmap chain.
     */
    public void setImage (int format, int width, int height, boolean border, boolean mipmap)
    {
        setImage(0, format, width, height, border);
        if (mipmap && !isRectangle()) {
            for (int ww = _width/2, hh = _height/2, ll = 1;
                    ww > 0 || hh > 0; ll++, ww /= 2, hh /= 2) {
                setImage(ll, format, Math.max(ww, 1), Math.max(hh, 1), border);
            }
        }
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImage (int level, int format, int width, int height, boolean border)
    {
        setImage(level, format, width, height, border,
            getTransferFormat(format), GL11.GL_UNSIGNED_BYTE, null);
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImage (
        int level, int format, int width, int height, boolean border,
        int dformat, int dtype, ByteBuffer data)
    {
        if (!(isRectangle() || GLContext.getCapabilities().GL_ARB_texture_non_power_of_two)) {
            width = GlUtil.nextPowerOfTwo(width);
            height = GlUtil.nextPowerOfTwo(height);
        }
        if (level == 0) {
            _format = format;
            _width = width;
            _height = height;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        GL11.glTexImage2D(
            _target, level, format, width + ib2, height + ib2, ib, dformat, dtype, data);
        setBytes(level, (data == null) ? (width*height*4) : data.remaining());
    }

    /**
     * Sets a single compressed level of this texture.
     */
    public void setCompressedImage (
        int level, int format, int width, int height, boolean border, ByteBuffer data)
    {
        if (level == 0) {
            _format = format;
            _width = width;
            _height = height;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        ARBTextureCompression.glCompressedTexImage2DARB(
            _target, level, format, width + ib2, height + ib2, ib, data);
        setBytes(level, (data == null) ? (width*height*4) : data.remaining());
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
        if (!mipmap || isRectangle()) {
            setGenerateMipmaps(false);
            setImage(0, format, border, image, premultiply, rescale);
            return;
        }
        if (GLContext.getCapabilities().GL_SGIS_generate_mipmap) {
            setGenerateMipmaps(true);
            setImage(0, format, border, image, premultiply, rescale);
            return;
        }
        int width = image.getWidth(), height = image.getHeight();
        if (!(isRectangle() || GLContext.getCapabilities().GL_ARB_texture_non_power_of_two)) {
            width = GlUtil.nextPowerOfTwo(width);
            height = GlUtil.nextPowerOfTwo(height);
        }
        _format = format;
        _width = width;
        _height = height;
        _renderer.setTexture(this);
        ByteBuffer data = getData(image, premultiply, width, height, rescale);
        GLU.gluBuild2DMipmaps(
            _target, format, width, height,
            getFormat(image), GL11.GL_UNSIGNED_BYTE, data);
        setMipmapBytes(data.remaining(), width, height);
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImage (
        int level, int format, boolean border, BufferedImage image,
        boolean premultiply, boolean rescale)
    {
        int width = image.getWidth(), height = image.getHeight();
        if (!(isRectangle() || GLContext.getCapabilities().GL_ARB_texture_non_power_of_two)) {
            width = GlUtil.nextPowerOfTwo(width);
            height = GlUtil.nextPowerOfTwo(height);
        }
        if (level == 0) {
            _format = format;
            _width = width;
            _height = height;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        ByteBuffer data = getData(image, premultiply, width, height, rescale);
        GL11.glTexImage2D(
            _target, level, format, width + ib2, height + ib2, ib,
            getFormat(image), GL11.GL_UNSIGNED_BYTE, data);
        if (level == 0 && _generateMipmaps) {
            setMipmapBytes(data.remaining(), width, height);
        } else {
            setBytes(level, data.remaining());
        }
    }

    /**
     * Sets this texture to the provided image.
     *
     * @param premultiply if true, premultiply the alpha values.
     * @param mipmap if true, generate a complete set of mipmaps for the image.
     * @param compress if true, compress the texture if possible.
     */
    public void setImage (
        BufferedImage image, boolean premultiply, boolean mipmap, boolean compress)
    {
        setImage(image, premultiply, mipmap, compress, true);
    }

    /**
     * Sets this texture to the provided image.
     *
     * @param premultiply if true, premultiply the alpha values.
     * @param mipmap if true, generate a complete set of mipmaps for the image.
     * @param compress if true, compress the texture if possible.
     * @param rescale if true, rescale the image if it is not a power of two (and the video
     * card doesn't support non-power-of-two textures).  If false, place the unscaled image in
     * the lower left part of the texture.
     */
    public void setImage (
        BufferedImage image, boolean premultiply, boolean mipmap,
        boolean compress, boolean rescale)
    {
        // determine the width and height of the texture
        int width = image.getWidth(), height = image.getHeight();
        if (!(isRectangle() || GLContext.getCapabilities().GL_ARB_texture_non_power_of_two)) {
            width = GlUtil.nextPowerOfTwo(width);
            height = GlUtil.nextPowerOfTwo(height);
        }
        _renderer.setTexture(this);
        ByteBuffer data = getData(image, premultiply, width, height, rescale);
        if (mipmap && !isRectangle()) { // rectangles cannot be mipmapped
            if (GLContext.getCapabilities().GL_SGIS_generate_mipmap) {
                setGenerateMipmaps(true);
                GL11.glTexImage2D(
                    _target, 0, getInternalFormat(image, compress), _width = width,
                    _height = height, 0, _format = getFormat(image), GL11.GL_UNSIGNED_BYTE, data);
            } else {
                GLU.gluBuild2DMipmaps(
                    _target, getInternalFormat(image, compress), _width = width, _height = height,
                    _format = getFormat(image), GL11.GL_UNSIGNED_BYTE, data);
            }
            setMipmapBytes(data.remaining(), width, height);

        } else {
            setGenerateMipmaps(false);
            GL11.glTexImage2D(
                _target, 0, getInternalFormat(image, compress), _width = width, _height = height,
                0, _format = getFormat(image), GL11.GL_UNSIGNED_BYTE, data);
            setBytes(0, data.remaining());
        }
    }

    /**
     * Sets part of the texture image.
     */
    public void setSubimage (
        BufferedImage image, boolean premultiply, int x, int y, int width, int height)
    {
        setSubimage(
            0, x, y, width, height, getFormat(image), GL11.GL_UNSIGNED_BYTE,
            getData(image, premultiply, width, height, true));
    }

    /**
     * Sets part of the texture image.
     */
    public void setSubimage (
        int level, int x, int y, int width, int height, int dformat, int dtype, ByteBuffer data)
    {
        _renderer.setTexture(this);
        GL11.glTexSubImage2D(_target, level, x, y, width, height, dformat, dtype, data);
    }

    /**
     * Copies part of the read buffer to the texture.
     */
    public void copySubImage (
        int level, int xoffset, int yoffset, int x, int y, int width, int height)
    {
        _renderer.setTexture(this);
        GL11.glCopyTexSubImage2D(_target, level, xoffset, yoffset, x, y, width, height);
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

    /** The dimensions of the texture. */
    protected int _width, _height;
}
