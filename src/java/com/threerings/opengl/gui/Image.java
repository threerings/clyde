//
// $Id$

package com.threerings.opengl.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.TextureUnit;

/**
 * Contains a texture and its dimensions.
 */
public class Image
{
    /**
     * Creates an image from the supplied source URL.
     */
    public Image (URL image)
        throws IOException
    {
        this(ImageIO.read(image));
    }

    /**
     * Creates an image from the supplied source AWT image.
     */
    public Image (BufferedImage image)
    {
        this(image.getWidth(null), image.getHeight(null));
        _image = image;
    }

    /**
     * Returns the width of this image.
     */
    public int getWidth ()
    {
        return _width;
    }

    /**
     * Returns the height of this image.
     */
    public int getHeight ()
    {
        return _height;
    }

    /**
     * Renders this image at the specified coordinates.
     */
    public void render (Renderer renderer, int tx, int ty, float alpha)
    {
        render(renderer, tx, ty, _width, _height, alpha);
    }

    /**
     * Renders this image at the specified coordinates in the specified color.
     */
    public void render (Renderer renderer, int tx, int ty, Color4f color, float alpha)
    {
        render(renderer, 0, 0, _width, _height, tx, ty, _width, _height, color, alpha);
    }

    /**
     * Renders this image at the specified coordinates, scaled to the specified size.
     */
    public void render (Renderer renderer, int tx, int ty, int twidth, int theight, float alpha)
    {
        render(renderer, 0, 0, _width, _height, tx, ty, twidth, theight, alpha);
    }

    /**
     * Renders a region of this image at the specified coordinates.
     */
    public void render (Renderer renderer, int sx, int sy,
                        int swidth, int sheight, int tx, int ty, float alpha)
    {
        render(renderer, sx, sy, swidth, sheight, tx, ty, swidth, sheight, alpha);
    }

    /**
     * Renders a region of this image at the specified coordinates, scaled to the specified size.
     */
    public void render (Renderer renderer, int sx, int sy, int swidth, int sheight,
                        int tx, int ty, int twidth, int theight, float alpha)
    {
        render(renderer, sx, sy, swidth, sheight, tx, ty, twidth, theight, Color4f.WHITE, alpha);
    }

    /**
     * Renders a region of this image at the specified coordinates, scaled to the specified size,
     * in the specified color.
     */
    public void render (
        Renderer renderer, int sx, int sy, int swidth, int sheight,
        int tx, int ty, int twidth, int theight, Color4f color, float alpha)
    {
        // initialize the texture units if necessary
        if (_units == null) {
            Texture2D texture = new Texture2D(renderer);
            texture.setImage(_image, true, false, false, false);
            texture.setMinFilter(GL11.GL_LINEAR);
            _twidth = texture.getWidth();
            _theight = texture.getHeight();
            _units = new TextureUnit[] { new TextureUnit(texture) };
        }
        float lx = sx / (float)_twidth;
        float ly = sy / (float)_theight;
        float ux = (sx+swidth) / (float)_twidth;
        float uy = (sy+sheight) / (float)_theight;

        float a = color.a * alpha;
        renderer.setColorState(color.r * a, color.g * a, color.b * a, a);
        renderer.setTextureState(_units);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(lx, ly);
        GL11.glVertex2f(tx, ty);
        GL11.glTexCoord2f(ux, ly);
        GL11.glVertex2f(tx + twidth, ty);
        GL11.glTexCoord2f(ux, uy);
        GL11.glVertex2f(tx + twidth, ty + theight);
        GL11.glTexCoord2f(lx, uy);
        GL11.glVertex2f(tx, ty + theight);
        GL11.glEnd();
    }

    /**
     * Helper constructor.
     */
    protected Image (int width, int height)
    {
        _width = width;
        _height = height;
    }

    protected int _width, _height;
    protected int _twidth, _theight;
    protected BufferedImage _image;

    protected TextureUnit[] _units;

    protected static boolean _supportsNonPowerOfTwo;
    static {
        _supportsNonPowerOfTwo = GLContext.getCapabilities().GL_ARB_texture_non_power_of_two;
    }
}
