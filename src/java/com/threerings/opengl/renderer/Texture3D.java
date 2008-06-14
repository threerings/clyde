//
// $Id$

package com.threerings.opengl.renderer;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

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
     */
    public void setImage (
        int format, int width, int height, int depth, boolean border, boolean mipmap)
    {
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            width = nextPOT(width);
            height = nextPOT(height);
            depth = nextPOT(depth);
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        GL12.glTexImage3D(
            GL12.GL_TEXTURE_3D, 0, _format = format, (_width = width) + ib2,
            (_height = height) + ib2, (_depth = depth) + ib2, ib, GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
        if (mipmap) {
            int level = 1;
            while ((width >>= 1) > 0 && (height >>= 1) > 0 && (depth >>= 1) > 0) {
                GL12.glTexImage3D(
                    GL12.GL_TEXTURE_3D, level++, format, width + ib2, height + ib2, depth + ib2,
                    ib, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
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

    /**
     * Returns the height of this texture (only valid after {@link #setImage} is called).
     */
    public int getHeight ()
    {
        return _height;
    }

    /**
     * Returns the depth of this texture (only valid after {@link #setImage} is called).
     */
    public int getDepth ()
    {
        return _depth;
    }

    /** The dimensions of the texture. */
    protected int _width, _height, _depth;
}
