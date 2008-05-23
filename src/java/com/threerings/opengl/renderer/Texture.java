//
// $Id$

package com.threerings.opengl.renderer;

import java.awt.Graphics2D;
import java.awt.Transparency;
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
        return _format == GL11.GL_LUMINANCE_ALPHA || _format == GL11.GL_RGBA;
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
    }

    /**
     * Creates an invalid texture (used by the renderer to force reapplication).
     */
    protected Texture ()
    {
    }

    @Override // documentation inherited
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _renderer.textureFinalized(_id);
        }
    }

    /**
     * Rounds the supplied value up to a power of two.
     */
    protected static int nextPOT (int value)
    {
        return (Integer.bitCount(value) > 1) ? (Integer.highestOneBit(value) << 1) : value;
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
     * Converts (and resizes) an image into a buffer of data to be passed to OpenGL.
     */
    protected static ByteBuffer getData (
        BufferedImage image, boolean premultiply, int width, int height, boolean rescale)
    {
        int iwidth = image.getWidth(), iheight = image.getHeight();
        int ncomps = image.getColorModel().getNumComponents();
        int format = FORMATS[ncomps - 1];

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
            true, null);

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
        graphics.drawImage(image, xform, null);
        graphics.dispose();

        // get the pixel data and copy it to a byte buffer
        byte[] rgba = ((DataBufferByte)dest.getData().getDataBuffer()).getData();
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

    /** The texture compare mode. */
    protected int _compareMode = GL11.GL_NONE;

    /** The texture compare function. */
    protected int _compareFunc = GL11.GL_LEQUAL;

    /** The depth texture mode. */
    protected int _depthMode = GL11.GL_LUMINANCE;

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
