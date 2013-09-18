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

import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.TextureUnit;

/**
 * An image with a mask.
 */
public class MaskedImage extends Image
{
    /**
     * Creates a masked image from existing images.
     */
    public MaskedImage (Image image, Image mask)
    {
        super(image.getWidth(), image.getHeight());
        _mask = mask;
        if (image._units != null) {
            setTexture((Texture2D)image._units[0].texture);
        } else {
            _image = image._image;
        }
    }

    @Override
    public void load (Renderer renderer, int format)
    {
        if (_units == null) {
            super.load(renderer, format);
        }
        if (_units[1] == null) {
            Texture2D mtex = _mask.getTexture(renderer);
            _units[1] = new TextureUnit(mtex);
        }
    }

    @Override
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

        float mwidth = _mask.getWidth() / (float)_units[1].texture.getWidth();
        float mheight = _mask.getHeight() / (float)_units[1].texture.getHeight();
        float mls = sx / (float)twidth * mwidth;
        float mus = (sx + swidth) / (float)twidth * mwidth;
        float mlt = sy / (float)theight * mheight;
        float mut = (sy + sheight) / (float)theight  * mheight;

        float a = color.a * alpha;
        renderer.setColorState(color.r * a, color.g * a, color.b * a, a);
        renderer.setTextureState(_units);
        renderer.setMatrixMode(GL11.GL_MODELVIEW);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(lx, ly);
        ARBMultitexture.glMultiTexCoord2fARB(
            ARBMultitexture.GL_TEXTURE1_ARB, mls, mlt);
        GL11.glVertex2f(tx, ty);
        GL11.glTexCoord2f(ux, ly);
        ARBMultitexture.glMultiTexCoord2fARB(
            ARBMultitexture.GL_TEXTURE1_ARB, mus, mlt);
        GL11.glVertex2f(tx+twidth, ty);
        GL11.glTexCoord2f(ux, uy);
        ARBMultitexture.glMultiTexCoord2fARB(
            ARBMultitexture.GL_TEXTURE1_ARB, mus, mut);
        GL11.glVertex2f(tx+twidth, ty+theight);
        GL11.glTexCoord2f(lx, uy);
        ARBMultitexture.glMultiTexCoord2fARB(
            ARBMultitexture.GL_TEXTURE1_ARB, mls, mut);
        GL11.glVertex2f(tx, ty+theight);
        GL11.glEnd();
    }


    @Override
    protected void setTexture (Texture2D texture)
    {
        _twidth = texture.getWidth();
        _theight = texture.getHeight();
        _units = new TextureUnit[2];
        _units[0] = new TextureUnit(texture);
    }

    protected Image _mask;
}
