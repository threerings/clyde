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

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.layout.LayoutManager;

/**
 * A window that automatically stretches to cover the entire render surface.
 */
public class StretchWindow extends Window
    implements Renderer.Observer
{
    public StretchWindow (GlContext ctx, LayoutManager layout)
    {
        super(ctx, layout);
    }

    // documentation inherited from interface Renderer.Observer
    public void sizeChanged (int width, int height)
    {
        setSize(width, height);
    }

    @Override
    protected void wasAdded ()
    {
        super.wasAdded();

        // update size and register as observer
        if (isStretching()) {
            Renderer renderer = _ctx.getRenderer();
            setSize(renderer.getWidth(), renderer.getHeight());
            renderer.addObserver(this);
        }
    }

    @Override
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctx.getRenderer().removeObserver(this); // safe to remove even if never added
    }

    /**
     * Are we actually using the stretching capability?
     */
    protected boolean isStretching ()
    {
        return true;
    }
}
