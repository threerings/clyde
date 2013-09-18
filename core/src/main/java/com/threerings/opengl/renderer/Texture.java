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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBDepthTexture;
import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.SGISGenerateMipmap;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntListUtil;

/**
 * An OpenGL texture object.
 */
public abstract class Texture
{
    /**
     * Creates a new texture for the specified renderer.
     */
    public Texture (Renderer renderer, int target)
    {
        _renderer = renderer;
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        GL11.glGenTextures(idbuf);
        _id = idbuf.get(0);
        _target = target;
        _renderer.textureCreated();
    }

    /**
     * Returns this texture's OpenGL identifier.
     */
    public final int getId ()
    {
        return _id;
    }

    /**
     * Returns the texture's target.
     */
    public final int getTarget ()
    {
        return _target;
    }

    /**
     * Returns the format of this texture.
     */
    public int getFormat ()
    {
        return _format;
    }

    /**
     * Checks whether this texture has an alpha channel.
     */
    public boolean hasAlpha ()
    {
        // these aren't all the alpha formats; just the ones in TextureConfig
        return _format == GL11.GL_ALPHA ||
            _format == ARBTextureCompression.GL_COMPRESSED_ALPHA_ARB ||
            _format == GL11.GL_LUMINANCE_ALPHA ||
            _format == ARBTextureCompression.GL_COMPRESSED_LUMINANCE_ALPHA_ARB ||
            _format == GL11.GL_RGBA ||
            _format == ARBTextureCompression.GL_COMPRESSED_RGBA_ARB;
    }

    /**
     * Determines whether this is a depth texture.
     */
    public boolean isDepth ()
    {
        return isDepth(_format);
    }

    /**
     * Returns the width of the texture.
     */
    public abstract int getWidth ();

    /**
     * Returns the height of the texture.
     */
    public abstract int getHeight ();

    /**
     * Returns the depth of the texture.
     */
    public int getDepth ()
    {
        return 1;
    }

    /**
     * Convenience method to set both the filters at once.
     */
    public void setFilters (int min, int mag)
    {
        setMinFilter(min);
        setMagFilter(mag);
    }

    /**
     * Sets the texture minification filter.
     */
    public void setMinFilter (int minFilter)
    {
        if (_minFilter != minFilter) {
            _renderer.setTexture(this);
            GL11.glTexParameteri(_target, GL11.GL_TEXTURE_MIN_FILTER, _minFilter = minFilter);
        }
    }

    /**
     * Sets the texture magnification filter.
     */
    public void setMagFilter (int magFilter)
    {
        if (_magFilter != magFilter) {
            _renderer.setTexture(this);
            GL11.glTexParameteri(_target, GL11.GL_TEXTURE_MAG_FILTER, _magFilter = magFilter);
        }
    }

    /**
     * Sets the texture maximum anisotropy.
     */
    public void setMaxAnisotropy (float maxAnisotropy)
    {
        if (_maxAnisotropy != maxAnisotropy) {
            _renderer.setTexture(this);
            GL11.glTexParameterf(
                _target, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
                _maxAnisotropy = maxAnisotropy);
        }
    }

    /**
     * Convenience method to set the s and t wrap modes at once.
     */
    public void setWrap (int s, int t)
    {
        setWrapS(s);
        setWrapT(t);
    }

    /**
     * Convenience method to set all the wrap modes at once.
     */
    public void setWrap (int s, int t, int r)
    {
        setWrapS(s);
        setWrapT(t);
        setWrapR(r);
    }

    /**
     * Sets the s texture wrap mode.
     */
    public void setWrapS (int wrap)
    {
        if (_wrapS != wrap) {
            _renderer.setTexture(this);
            GL11.glTexParameteri(_target, GL11.GL_TEXTURE_WRAP_S, _wrapS = wrap);
        }
    }

    /**
     * Sets the t texture wrap mode.
     */
    public void setWrapT (int wrap)
    {
        if (_wrapT != wrap) {
            _renderer.setTexture(this);
            GL11.glTexParameteri(_target, GL11.GL_TEXTURE_WRAP_T, _wrapT = wrap);
        }
    }

    /**
     * Sets the r texture wrap mode.
     */
    public void setWrapR (int wrap)
    {
        if (_wrapR != wrap) {
            _renderer.setTexture(this);
            GL11.glTexParameteri(_target, GL12.GL_TEXTURE_WRAP_R, _wrapR = wrap);
        }
    }

    /**
     * Sets the border color.
     */
    public void setBorderColor (Color4f borderColor)
    {
        if (!_borderColor.equals(borderColor)) {
            _renderer.setTexture(this);
            _borderColor.set(borderColor).get(_vbuf).rewind();
            GL11.glTexParameter(_target, GL11.GL_TEXTURE_BORDER_COLOR, _vbuf);
        }
    }

    /**
     * Sets whether or not to generate mipmaps automatically.
     */
    public void setGenerateMipmaps (boolean generate)
    {
        if (_generateMipmaps != generate) {
            _renderer.setTexture(this);
            GL11.glTexParameteri(
                _target, SGISGenerateMipmap.GL_GENERATE_MIPMAP_SGIS,
                (_generateMipmaps = generate) ? GL11.GL_TRUE : GL11.GL_FALSE);
        }
    }

    /**
     * Convenience method to set both compare parameters at once.
     */
    public void setCompare (int mode, int func)
    {
        setCompareMode(mode);
        setCompareFunc(func);
    }

    /**
     * Sets the texture compare mode.
     */
    public void setCompareMode (int compareMode)
    {
        if (_compareMode != compareMode) {
            _renderer.setTexture(this);
            GL11.glTexParameteri(
                _target, ARBShadow.GL_TEXTURE_COMPARE_MODE_ARB,
                _compareMode = compareMode);
        }
    }

    /**
     * Sets the texture compare function.
     */
    public void setCompareFunc (int compareFunc)
    {
        if (_compareFunc != compareFunc) {
            _renderer.setTexture(this);
            GL11.glTexParameteri(
                _target, ARBShadow.GL_TEXTURE_COMPARE_FUNC_ARB,
                _compareFunc = compareFunc);
        }
    }

    /**
     * Sets the depth texture mode.
     */
    public void setDepthMode (int depthMode)
    {
        if (_depthMode != depthMode) {
            _renderer.setTexture(this);
            GL11.glTexParameteri(
                _target, ARBDepthTexture.GL_DEPTH_TEXTURE_MODE_ARB,
                _depthMode = depthMode);
        }
    }

    /**
     * Generates a set of mipmaps for this texture.  This relies on the GL_EXT_framebuffer_object
     * extension, so it's really only useful in conjunction with FBOs.
     */
    public void generateMipmap ()
    {
        _renderer.setTexture(this);
        EXTFramebufferObject.glGenerateMipmapEXT(_target);
    }

    /**
     * Deletes this texture, rendering it unusable.
     */
    public void delete ()
    {
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        idbuf.put(_id).rewind();
        GL11.glDeleteTextures(idbuf);
        _id = 0;
        _renderer.textureDeleted(getTotalBytes());
    }

    /**
     * Creates an invalid texture (used by the renderer to force reapplication).
     */
    protected Texture ()
    {
    }

    @Override
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _renderer.textureFinalized(_id, getTotalBytes());
        }
    }

    /**
     * Sets the number of bytes occupied by all mipmap levels.
     */
    protected void setMipmapBytes (int bytes, int... dimensions)
    {
        int size = IntListUtil.getMaxValue(dimensions);
        for (int ll = 0; size > 0; ll++, bytes /= 4, size /= 2) {
            setBytes(ll, bytes);
        }
    }

    /**
     * Sets the number of bytes occupied by the specified mipmap level.
     */
    protected void setBytes (int level, int bytes)
    {
        if (level >= _bytes.length) {
            int[] obytes = _bytes;
            _bytes = new int[level + 1];
            System.arraycopy(obytes, 0, _bytes, 0, obytes.length);
        }
        _renderer.textureResized(bytes - _bytes[level]);
        _bytes[level] = bytes;
    }

    /**
     * Returns the total number of bytes in the texture.
     */
    protected int getTotalBytes ()
    {
        return IntListUtil.sum(_bytes);
    }

    /**
     * Returns the format of the data to be returned by {@link #getData} for the specified image.
     */
    protected static int getFormat (BufferedImage image)
    {
        return FORMATS[image.getColorModel().getNumComponents() - 1];
    }

    /**
     * Returns the internal format to use for the given image with optional compression.
     */
    protected static int getInternalFormat (BufferedImage image, boolean compress)
    {
        int[] formats = (GLContext.getCapabilities().GL_ARB_texture_compression && compress) ?
            COMPRESSED_FORMATS : FORMATS;
        return formats[image.getColorModel().getNumComponents() - 1];
    }

    /**
     * Returns a suitable transfer format corresponding to the provided internal format.
     */
    protected static int getTransferFormat (int internalFormat)
    {
        return isDepth(internalFormat) ? GL11.GL_DEPTH_COMPONENT : GL11.GL_RGBA;
    }

    /**
     * Checks whether the specified format is a depth format.
     */
    protected static boolean isDepth (int format)
    {
        return format == GL11.GL_DEPTH_COMPONENT ||
            format == ARBDepthTexture.GL_DEPTH_COMPONENT16_ARB ||
            format == ARBDepthTexture.GL_DEPTH_COMPONENT24_ARB ||
            format == ARBDepthTexture.GL_DEPTH_COMPONENT32_ARB;
    }

    /**
     * Scales the provided image in half by each dimension for use as a mipmap.
     */
    protected static BufferedImage halveImage (BufferedImage image)
    {
        int width = Math.max(1, image.getWidth() / 2);
        int height = Math.max(1, image.getHeight() / 2);
        BufferedImage dest = new BufferedImage(width, height, image.getType());
        Graphics2D graphics = dest.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return dest;
    }

    /**
     * Converts (and resizes) an image into a buffer of data to be passed to OpenGL.
     */
    protected static ByteBuffer getData (
        BufferedImage image, boolean premultiply, int width, int height, boolean rescale)
    {
        int iwidth = image.getWidth(), iheight = image.getHeight();
        int ncomps = image.getColorModel().getNumComponents();
        // create a compatible color model
        boolean hasAlpha = (ncomps == 2 || ncomps == 4);
        ComponentColorModel cmodel = new ComponentColorModel(
            ColorSpace.getInstance(ncomps >= 3 ? ColorSpace.CS_sRGB : ColorSpace.CS_GRAY),
            hasAlpha,
            hasAlpha && premultiply,
            hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE,
            DataBuffer.TYPE_BYTE);

        // create the target image
        BufferedImage dest = new BufferedImage(
            cmodel,
            Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, ncomps, null),
            cmodel.isAlphaPremultiplied(), null);

        // draw the image into the target buffer, scaling and flipping it in the process
        double xscale, yscale;
        if (rescale && (width != iwidth || height != iheight)) {
            xscale = (double)width / iwidth;
            yscale = -(double)height / iheight;
        } else {
            xscale = +1.0;
            yscale = -1.0;
        }
        AffineTransform xform = AffineTransform.getScaleInstance(xscale, yscale);
        xform.translate(0.0, -iheight);
        Graphics2D graphics = dest.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                rescale ? RenderingHints.VALUE_INTERPOLATION_BILINEAR :
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawRenderedImage(image, xform);
        } finally {
            graphics.dispose();
        }

        // get the pixel data and copy it to a byte buffer
        byte[] rgba = ((DataBufferByte)dest.getRaster().getDataBuffer()).getData();
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * ncomps);
        data.put(rgba).rewind();
        return data;
    }

    /** The renderer responsible for this texture. */
    protected Renderer _renderer;

    /** The OpenGL identifier for this texture. */
    protected int _id;

    /** The texture target. */
    protected int _target;

    /** The format of the texture. */
    protected int _format;

    /** The current minification filter. */
    protected int _minFilter = GL11.GL_NEAREST_MIPMAP_LINEAR;

    /** The current magnification filter. */
    protected int _magFilter = GL11.GL_LINEAR;

    /** The maximum degree of anisotropy. */
    protected float _maxAnisotropy = 1f;

    /** The s texture wrap mode. */
    protected int _wrapS = GL11.GL_REPEAT;

    /** The t texture wrap mode. */
    protected int _wrapT = GL11.GL_REPEAT;

    /** The r texture wrap mode. */
    protected int _wrapR = GL11.GL_REPEAT;

    /** The border color. */
    protected Color4f _borderColor = new Color4f(0f, 0f, 0f, 0f);

    /** Whether or not mipmaps should be automatically generated. */
    protected boolean _generateMipmaps;

    /** The texture compare mode. */
    protected int _compareMode = GL11.GL_NONE;

    /** The texture compare function. */
    protected int _compareFunc = GL11.GL_LEQUAL;

    /** The depth texture mode. */
    protected int _depthMode = GL11.GL_LUMINANCE;

    /** The number of bytes occupied by each mipmap level. */
    protected int[] _bytes = ArrayUtil.EMPTY_INT;

    /** A buffer for floating point values. */
    protected static FloatBuffer _vbuf = BufferUtils.createFloatBuffer(16);

    /** Formats corresponding to one, two, three, or four color components. */
    protected static final int[] FORMATS = {
        GL11.GL_LUMINANCE, GL11.GL_LUMINANCE_ALPHA, GL11.GL_RGB, GL11.GL_RGBA };

    /** Compressed internal formats for one, two, three, or four color components. */
    protected static final int[] COMPRESSED_FORMATS = {
        ARBTextureCompression.GL_COMPRESSED_LUMINANCE_ARB,
        ARBTextureCompression.GL_COMPRESSED_LUMINANCE_ALPHA_ARB,
        ARBTextureCompression.GL_COMPRESSED_RGB_ARB,
        ARBTextureCompression.GL_COMPRESSED_RGBA_ARB };
}
