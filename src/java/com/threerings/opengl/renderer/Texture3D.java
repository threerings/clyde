//
// $Id$

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
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
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
        int ib = border ? 1 : 0, ib2 = ib*2;
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

    @Override // documentation inherited
    public int getWidth ()
    {
        return _width;
    }

    @Override // documentation inherited
    public int getHeight ()
    {
        return _height;
    }

    @Override // documentation inherited
    public int getDepth ()
    {
        return _depth;
    }

    /** The dimensions of the texture. */
    protected int _width, _height, _depth;
}
