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

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.layout.LayoutManager;

/**
 * A window that is popped up to display something like a menu or a
 * tooltip or some other temporary, modal overlaid display.
 */
public class PopupWindow extends Window
{
    public PopupWindow (GlContext ctx, Window parent, LayoutManager layout)
    {
        super(ctx, layout);
        _parentWindow = parent;
    }

    @Override
    public boolean shouldShadeBehind ()
    {
        return false;
    }

    /**
     * Sizes the window to its preferred size and then displays it at the
     * specified coordinates extending either above the location or below
     * as specified. The window position may be adjusted if it does not
     * fit on the screen at the specified coordinates.
     */
    public void popup (int x, int y, boolean above)
    {
        setLayer(_parentWindow.getLayer());
        // add ourselves to the interface hierarchy if we're not already
        if (_root == null) {
            _parentWindow.getRoot().addWindow(this);
        }

        // size and position ourselves appropriately
        packAndFit(x, y, above);
    }

    /**
     * Called after we have been added to the display hierarchy to pack and
     * position this popup window.
     */
    protected void packAndFit (int x, int y, boolean above)
    {
        pack();

        // adjust x and y to ensure that we fit on the screen
        int width = _root.getDisplayWidth();
        int height = _root.getDisplayHeight();
        x = Math.min(width - getWidth(), x);
        y = above ?
            Math.min(height - getHeight(), y) : Math.max(0, y - getHeight());
        setLocation(x, y);
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/PopupWindow";
    }
}
