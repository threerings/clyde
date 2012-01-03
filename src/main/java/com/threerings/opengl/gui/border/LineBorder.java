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

package com.threerings.opengl.gui.border;

import org.lwjgl.opengl.GL11;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.util.Insets;

/**
 * Defines a border that displays a single line around the bordered component in a specified color.
 */
public class LineBorder extends Border
{
    public LineBorder (Color4f color)
    {
        this(color, 1);
    }

    public LineBorder (Color4f color, int width)
    {
        _color.set(color);
        _width = width;
    }

    @Override // from Border
    public Insets adjustInsets (Insets insets)
    {
        return new Insets(_width + insets.left, _width + insets.top,
                          _width + insets.right, _width + insets.bottom);
    }

    @Override // from Border
    public void render (Renderer renderer, int x, int y, int width, int height, float alpha)
    {
        super.render(renderer, x, y, width, height, alpha);
        float offset = _width / 2f;

        float a = _color.a * alpha;
        renderer.setColorState(_color.r * a, _color.g * a, _color.b * a, a);
        renderer.setTextureState(null);
        GL11.glLineWidth(_width);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(x + offset, y + offset);
        GL11.glVertex2f(x + width - offset, y + offset);
        GL11.glVertex2f(x + width - offset, y + height - offset);
        GL11.glVertex2f(x + offset, y + height - offset);
        GL11.glVertex2f(x + offset, y + offset);
        GL11.glEnd();
        GL11.glLineWidth(1f);
    }

    protected Color4f _color = new Color4f();
    protected int _width;

    protected static final Insets ONE_PIXEL_INSETS = new Insets(1, 1, 1, 1);
}
