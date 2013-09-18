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
 * Displays a region of an image as an icon.
 */
public class SubimageIcon extends Icon
{
    /**
     * Creates an icon that will display the specified region of the supplied
     * image.
     */
    public SubimageIcon (Image image, int x, int y, int width, int height)
    {
        _region = new Rectangle(x, y, width, height);
        _image = image;
    }

    // documentation inherited
    public int getWidth ()
    {
        return _region.width;
    }

    // documentation inherited
    public int getHeight ()
    {
        return _region.height;
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, float alpha)
    {
        super.render(renderer, x, y, alpha);
        _image.render(renderer, _region.x, _region.y,
                      _region.width, _region.height, x, y, alpha);
    }

    protected Image _image;
    protected Rectangle _region;
}
