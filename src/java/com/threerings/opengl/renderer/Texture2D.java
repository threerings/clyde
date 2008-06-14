//
// $Id$

package com.threerings.opengl.renderer;

import java.awt.image.BufferedImage;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.ARBTextureRectangle;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.glu.GLU;

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
     */
    public void setImage (int format, int width, int height, boolean border, boolean mipmap)
    {
        if (!(isRectangle() || GLContext.getCapabilities().GL_ARB_texture_non_power_of_two)) {
            width = nextPOT(width);
            height = nextPOT(height);
        }
        _renderer.setTexture(this);
        int ib = border ? 1 : 0, ib2 = ib*2;
        GL11.glTexImage2D(
            _target, 0, _format = format, (_width = width) + ib2, (_height = height) + ib2, ib,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
        if (mipmap && !isRectangle()) {
            int level = 1;
            while ((width >>= 1) > 0 && (height >>= 1) > 0) {
                GL11.glTexImage2D(
                    _target, level++, format, width + ib2, height + ib2, ib,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
            }
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
            width = nextPOT(width);
            height = nextPOT(height);
        }
        _renderer.setTexture(this);
        if (mipmap && !isRectangle()) { // rectangles cannot be mipmapped
            GLU.gluBuild2DMipmaps(
                _target, getInternalFormat(image, compress), _width = width, _height = height,
                _format = getFormat(image), GL11.GL_UNSIGNED_BYTE,
                getData(image, premultiply, width, height, rescale));
        } else {
            GL11.glTexImage2D(
                _target, 0, getInternalFormat(image, compress), _width = width, _height = height,
                0, _format = getFormat(image), GL11.GL_UNSIGNED_BYTE,
                getData(image, premultiply, width, height, rescale));
        }
    }

    /**
     * Sets part of the texture image.
     */
    public void setSubimage (
        BufferedImage image, boolean premultiply, int x, int y, int width, int height)
    {
        _renderer.setTexture(this);
        GL11.glTexSubImage2D(
            _target, 0, x, y, width, height, getFormat(image), GL11.GL_UNSIGNED_BYTE,
            getData(image, premultiply, width, height, true));
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

    /** The dimensions of the texture. */
    protected int _width, _height;
}
