//
// $Id$

package com.threerings.opengl.renderer;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.ARBTextureCubeMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

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
     * Sets this texture to an empty image with the specified format and dimensions.
     *
     * @param size the size (width and height) of each face.
     */
    public void setImages (int format, int size)
    {
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            size = nextPOT(size);
        }
        _renderer.setTexture(this);
        for (int target : FACE_TARGETS) {
            GL11.glTexImage2D(
                target, 0, format, size, size, 0, format, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
        }
        _size = size;
        _format = format;
    }

    /**
     * Returns the size (width and height) of each face (only valid after {@link #setImages} is
     * called).
     */
    public int getSize ()
    {
        return _size;
    }

    /** The size (width and height) of each face. */
    protected int _size;
}
