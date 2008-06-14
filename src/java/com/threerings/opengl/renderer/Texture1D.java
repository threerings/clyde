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
     */
    public void setImage (int format, int width, boolean border, boolean mipmap)
    {
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            width = nextPOT(width);
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        GL11.glTexImage1D(
            GL11.GL_TEXTURE_1D, 0, _format = format, (_width = width) + ib2, ib,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
        if (mipmap) {
            int level = 1;
            while ((width >>= 1) > 0) {
                GL11.glTexImage1D(
                    GL11.GL_TEXTURE_1D, level++, format, width + ib2, ib,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
            }
        }
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
