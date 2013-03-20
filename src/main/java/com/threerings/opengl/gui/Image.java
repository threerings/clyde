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

package com.threerings.opengl.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
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
     * Configures a texture for use as an image.
     */
    public static void configureTexture (Texture2D texture)
    {
        texture.setMinFilter(GL11.GL_LINEAR);
        int wrap = GLContext.getCapabilities().OpenGL12 ? GL12.GL_CLAMP_TO_EDGE : GL11.GL_CLAMP;
        texture.setWrap(wrap, wrap);
    }

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
     * Creates an image from an existing texture.
     */
    public Image (Texture2D texture)
    {
        this(texture, texture.getWidth(), texture.getHeight());
    }

    /**
     * Creates an image from an existing texture.
     */
    public Image (Texture2D texture, int width, int height)
    {
        this(width, height);
        setTexture(texture);
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
     * Returns a reference to the image texture, loading it if necessary.
     */
    public Texture2D getTexture (Renderer renderer)
    {
        load(renderer, -1);
        return (Texture2D)_units[0].texture;
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
        // don't bother rendering if it's completely transparent
        if (alpha == 0f) {
            return;
        }

        // initialize the texture units if necessary
        load(renderer, -1);
        float lx = sx / (float)_twidth;
        float ly = sy / (float)_theight;
        float ux = (sx+swidth) / (float)_twidth;
        float uy = (sy+sheight) / (float)_theight;

        float a = color.a * alpha;
        renderer.setColorState(color.r * a, color.g * a, color.b * a, a);
        renderer.setTextureState(_units);
        renderer.setMatrixMode(GL11.GL_MODELVIEW);
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
     * Loads the image into a texture (this is called automatically when first used and need only
     * be called manually if a specific format is desired).
     *
     * @param format the internal format to use for the texture, or -1 to use the default.
     */
    public void load (Renderer renderer, int format)
    {
        if (_units != null) {
            return; // already loaded
        }
        Texture2D texture = new Texture2D(renderer);
        if (format == -1) {
            texture.setImage(_image, true, false, false, false);
        } else {
            texture.setImage(format, false, _image, true, false, false);
        }
        configureTexture(texture);
        setTexture(texture);
    }

    /**
     * Helper constructor.
     */
    protected Image (int width, int height)
    {
        _width = width;
        _height = height;
    }

    /**
     * Sets the image texture.
     */
    protected void setTexture (Texture2D texture)
    {
        _twidth = texture.getWidth();
        _theight = texture.getHeight();
        _units = new TextureUnit[] { new TextureUnit(texture) };
    }

    protected int _width, _height;
    protected int _twidth, _theight;
    protected BufferedImage _image;

    protected TextureUnit[] _units;
}
