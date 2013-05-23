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
import org.lwjgl.opengl.ARBTextureCubeMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.opengl.util.GlUtil;

/**
 * A cube map texture.
 */
public class TextureCubeMap extends Texture
{
    /** The targets for each face. */
    public static final int[] FACE_TARGETS = {
        ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_POSITIVE_X_ARB,
        ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_NEGATIVE_X_ARB,
        ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_POSITIVE_Y_ARB,
        ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y_ARB,
        ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_POSITIVE_Z_ARB,
        ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z_ARB };

    /**
     * Creates a new texture.
     */
    public TextureCubeMap (Renderer renderer)
    {
        super(renderer, ARBTextureCubeMap.GL_TEXTURE_CUBE_MAP_ARB);
    }

    /**
     * Sets this texture to a set of empty images with the specified format and dimension.
     *
     * @param mipmap if true, generate a complete mipmap chain.
     */
    public void setImages (int format, int size, boolean border, boolean mipmap)
    {
        setImages(0, format, size, border);
        if (mipmap) {
            for (int ss = _size/2, ll = 1; ss > 0; ll++, ss /= 2) {
                setImages(ll, format, ss, border);
            }
        }
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImages (int level, int format, int size, boolean border)
    {
        for (int target : FACE_TARGETS) {
            setImage(target, level, format, size, border, getTransferFormat(format),
                GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
        }
    }

    /**
     * Sets a single mipmap level of a single face of this texture.
     */
    public void setImage (
        int target, int level, int format, int size, boolean border,
        int dformat, int dtype, ByteBuffer data)
    {
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            size = GlUtil.nextPowerOfTwo(size);
        }
        if (level == 0) {
            _format = format;
            _size = size;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        int bsize = size + ib2;
        GL11.glTexImage2D(target, level, format, bsize, bsize, ib, dformat, dtype, data);
    }

    /**
     * Sets a single compressed mipmap level of a single face of this texture.
     */
    public void setCompressedImage (
        int target, int level, int format, int size, boolean border, ByteBuffer data)
    {
        if (level == 0) {
            _format = format;
            _size = size;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        int bsize = size + ib2;
        ARBTextureCompression.glCompressedTexImage2DARB(
            target, level, format, bsize, bsize, ib, data);
    }

    /**
     * Sets this texture to the supplied images.
     *
     * @param mipmap if true, generate a complete mipmap chain.
     */
    public void setImages (
        int format, boolean border, BufferedImage[] images,
        boolean premultiply, boolean rescale, boolean mipmap)
    {
        setImages(0, format, border, images, premultiply, rescale);
        if (mipmap) {
            for (int ss = _size/2, ll = 1; ss > 0; ll++, ss /= 2) {
                for (int ii = 0; ii < images.length; ii++) {
                    if (images[ii] != null) {
                        images[ii] = halveImage(images[ii]);
                    }
                }
                setImages(ll, format, border, images, premultiply, rescale);
            }
        }
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImages (
        int level, int format, boolean border, BufferedImage[] images,
        boolean premultiply, boolean rescale)
    {
        int size = 1;
        for (BufferedImage image : images) {
            if (image != null) {
                size = Math.max(size, Math.max(image.getWidth(), image.getHeight()));
            }
        }
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            size = GlUtil.nextPowerOfTwo(size);
        }
        if (level == 0) {
            _format = format;
            _size = size;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        int bsize = size + ib2;
        for (int ii = 0; ii < FACE_TARGETS.length; ii++) {
            BufferedImage image = images[ii];
            if (image != null) {
                GL11.glTexImage2D(
                    FACE_TARGETS[ii], level, format, bsize, bsize, ib,
                    getFormat(image), GL11.GL_UNSIGNED_BYTE,
                    getData(image, premultiply, size, size, rescale));
            }
        }
    }

    /**
     * Sets this texture to the supplied image.
     *
     * @param mipmap if true, generate a complete mipmap chain.
     */
    public void setImages (
        int format, boolean border, BufferedImage image, int sdivs, int tdivs,
        boolean premultiply, boolean rescale, boolean mipmap)
    {
        setImages(0, format, border, image, sdivs, tdivs, premultiply, rescale);
        if (mipmap) {
            for (int ss = _size/2, ll = 1; ss > 0; ll++, ss /= 2) {
                setImages(ll, format, border, image, sdivs, tdivs, premultiply, rescale);
            }
        }
    }

    /**
     * Sets a single mipmap level of this texture.
     */
    public void setImages (
        int level, int format, boolean border, BufferedImage image,
        int sdivs, int tdivs, boolean premultiply, boolean rescale)
    {
        int size = Math.max(image.getWidth() / sdivs, image.getHeight() / tdivs);
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            size = GlUtil.nextPowerOfTwo(size);
        }
        if (level == 0) {
            _format = format;
            _size = size;
        }
        _renderer.setTexture(this);
        // TODO
//        int ib = border ? 1 : 0, ib2 = ib*2;
//        int bsize = size + ib2;
//        for (int target : FACE_TARGETS) {
//        }
    }

    @Override
    public int getWidth ()
    {
        return _size;
    }

    @Override
    public int getHeight ()
    {
        return _size;
    }

    /** The size (width and height) of each face. */
    protected int _size;
}
