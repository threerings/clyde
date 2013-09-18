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

package com.threerings.opengl.gui.icon;

import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.Image;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Displays a scaled region of an image as an icon.
 */
public class ScaledImageIcon extends Icon
{
    /**
     * Creates an icon that will display the specified image scaled to the supplied size.
     */
    public ScaledImageIcon (Image image, int twidth, int theight)
    {
        this(image, 0, 0, image.getWidth(), image.getHeight(), twidth, theight);
    }

    /**
     * Creates an icon that will display the specified region of the supplied image scaled to the
     * supplied size.
     */
    public ScaledImageIcon (
        Image image, int sx, int sy, int swidth, int sheight, int twidth, int theight)
    {
        _image = image;
        _sregion = new Rectangle(sx, sy, swidth, sheight);
        _twidth = twidth;
        _theight = theight;
    }

    // documentation inherited
    public int getWidth ()
    {
        return _twidth;
    }

    // documentation inherited
    public int getHeight ()
    {
        return _theight;
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, float alpha)
    {
        super.render(renderer, x, y, alpha);
        _image.render(renderer, _sregion.x, _sregion.y,
                      _sregion.width, _sregion.height, x, y, _twidth, _theight, alpha);
    }

    protected Image _image;
    protected Rectangle _sregion;
    protected int _twidth, _theight;
}
