//
// $Id$

package com.threerings.opengl.renderer;

import java.awt.image.BufferedImage;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

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
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            width = nextPOT(width);
        }
        if (level == 0) {
            _format = format;
            _width = width;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        GL11.glTexImage1D(
            GL11.GL_TEXTURE_1D, level, format, width + ib2, ib,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
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
            width = nextPOT(width);
        }
        if (level == 0) {
            _format = format;
            _width = width;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        GL11.glTexImage1D(
            GL11.GL_TEXTURE_1D, level, format, width + ib2, ib,
            getFormat(image), GL11.GL_UNSIGNED_BYTE,
            getData(image, premultiply, width, 1, rescale));
    }

    /**
     * Returns the width of this texture (only valid after {@link #setImage} is called).
     */
    public int getWidth ()
    {
        return _width;
    }

    /** The dimension of the texture. */
    protected int _width;
}
