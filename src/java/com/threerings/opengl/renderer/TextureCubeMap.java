//
// $Id$

package com.threerings.opengl.renderer;

import java.awt.image.BufferedImage;

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
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            size = nextPOT(size);
        }
        if (level == 0) {
            _format = format;
            _size = size;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        int bsize = size + ib2;
        for (int target : FACE_TARGETS) {
            GL11.glTexImage2D(
                target, level, format, bsize, bsize, ib,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
        }
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
            size = nextPOT(size);
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
            size = nextPOT(size);
        }
        if (level == 0) {
            _format = format;
            _size = size;
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        int bsize = size + ib2;
        for (int ii = 0; ii < FACE_TARGETS.length; ii++) {
            // TODO
        }
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
