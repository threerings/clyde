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

import com.threerings.opengl.renderer.Renderer;

import com.threerings.opengl.gui.util.Insets;

/**
 * Combines two borders into a single compound border.
 */
public class CompoundBorder extends Border
{
    public CompoundBorder (Border outer, Border inner)
    {
        _outer = outer;
        _inner = inner;
        _insets = outer.adjustInsets(Insets.ZERO_INSETS);
    }

    // documentation inherited
    public Insets adjustInsets (Insets insets)
    {
        return _outer.adjustInsets(_inner.adjustInsets(insets));
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, int width, int height,
        float alpha)
    {
        _outer.render(renderer, x, y, width, height, alpha);
        _inner.render(renderer, x + _insets.left, y + _insets.bottom,
                      width - _insets.getHorizontal(),
                      height - _insets.getVertical(), alpha);
    }

    protected Border _outer, _inner;
    protected Insets _insets;
}
