//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import java.util.ArrayList;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.layout.GroupLayout;
import com.threerings.opengl.gui.layout.TableLayout;
import com.threerings.opengl.gui.util.Dimension;

/**
 * Displays a popup menu of items, one of which can be selected.
 */
public class PopupMenu extends PopupWindow
{
    public PopupMenu (GlContext ctx, Window parent)
    {
        this(ctx, parent, false);
    }

    public PopupMenu (GlContext ctx, Window parent, boolean horizontal)
    {
        super(ctx, parent, null);
        setLayoutManager(
                (horizontal ? GroupLayout.makeHStretch() : GroupLayout.makeVStretch()).setGap(0));
        _columns = 1;
        _modal = true;
    }

    public PopupMenu (GlContext ctx, Window parent, int columns)
    {
        super(ctx, parent, null);
        setPreferredColumns(columns);
        _modal = true;
    }

    /**
     * Adds the supplied item to this menu.
     */
    public void addMenuItem (MenuItem item)
    {
        // nothing more complicated needs to be done, yay!
        add(item, GroupLayout.FIXED);
    }

    /**
     * Sets the preferred number of columns.  Will relayout the existing menu items into the
     * preferred number of columns, but may add more columns if the menu will not fit in
     * the vertical space.
     */
    public void setPreferredColumns (int columns)
    {
        columns = Math.max(1, columns);
        if (columns == _columns) {
            return;
        }
        _columns = columns;
        ArrayList<Component> children = new ArrayList<Component>(_children);
        removeAll();
        if (_columns == 1) {
            setLayoutManager(GroupLayout.makeVStretch().setGap(0));
        } else {
            setLayoutManager(new TableLayout(_columns, 0, 5));
        }
        for (Component child : children) {
            add(child);
        }
    }

    // documentation inherited
    public boolean dispatchEvent (Event event)
    {
        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            // if the mouse clicked outside of our window bounds, dismiss
            // ourselves
            if (mev.getType() == MouseEvent.MOUSE_PRESSED &&
                getHitComponent(mev.getX(), mev.getY()) == null) {
                dismiss();
                return true;
            }
        }
        return super.dispatchEvent(event);
    }

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return "Default/PopupMenu";
    }

    // documentation inherited
    protected void packAndFit (int x, int y, boolean above)
    {
        int width = _root.getDisplayWidth();
        int height = _root.getDisplayHeight();

        // determine whether we can fit in the window
        ArrayList<Component> children = null;
        int columns = _columns;
        do {
            Dimension d = getPreferredSize(-1, -1);
            if (d.height > height) {
                // remove our children, switch to a table layout and readd
                if (children == null) {
                    children = new ArrayList<Component>(_children);
                }
                removeAll();
                setLayoutManager(new TableLayout(++columns, 0, 5));
                for (int ii = 0; ii < children.size(); ii++) {
                    add(children.get(ii));
                }
            } else {
                break;
            }
        } while (columns < 4);

        // now actually lay ourselves out
        pack();

        // adjust x and y to ensure that we fit on the screen
        x = Math.min(width - getWidth(), x);
        y = above ?
            Math.min(height - getHeight(), y) : Math.max(0, y - getHeight());
        setLocation(x, y);
    }

    /**
     * Called by any child {@link MenuItem}s when they are selected.
     */
    protected void itemSelected (MenuItem item, long when, int modifiers)
    {
        emitEvent(new ActionEvent(item, when, modifiers, item.getAction()));
        dismiss();
    }

    protected int _columns;
}
