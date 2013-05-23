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

    public PopupMenu (GlContext ctx, Window parent, int rows, int columns)
    {
        super(ctx, parent, null);
        setPreferredDimensions(rows, columns);
        _modal = true;
    }

    /**
     * Adds the supplied item to this menu.
     */
    public void addMenuItem (MenuItem item)
    {
        getItemContainer().add(item, GroupLayout.FIXED);
    }

    /**
     * Sets the preferred number of rows and columns.  Will relayout the existing menu items into
     * the preferred number of columns, but may add more columns if the menu will not fit in
     * the vertical space.
     *
     * @param rows the number of rows to display at one time, or zero if unlimited.
     * @param columns the number of columns to display.
     */
    public void setPreferredDimensions (int rows, int columns)
    {
        columns = Math.max(1, columns);
        if (rows == _rows && columns == _columns) {
            return;
        }
        _rows = rows;
        _columns = columns;
        ArrayList<Component> children = new ArrayList<Component>(_children);
        removeAll();
        Container cont;
        if (_rows == 0) {
            cont = this;
        } else {
            setLayoutManager(GroupLayout.makeVStretch());
            ScrollPane pane = new ScrollPane(_ctx, cont = new Container(_ctx));
            add(pane);
            pane.setShowScrollbarAlways(false);
        }
        if (_columns == 1) {
            cont.setLayoutManager(GroupLayout.makeVStretch().setGap(0));
        } else {
            cont.setLayoutManager(new TableLayout(_columns, 0, 5));
        }
        for (Component child : children) {
            cont.add(child);
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

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/PopupMenu";
    }

    // documentation inherited
    protected void packAndFit (int x, int y, boolean above)
    {
        int width = _root.getDisplayWidth();
        int height = _root.getDisplayHeight();

        // adjust the preferred size of the scroll pane if necessary
        if (_rows != 0) {
            ScrollPane pane = (ScrollPane)getComponent(0);
            Container cont = (Container)pane.getChild();
            if (cont.getComponentCount() > _rows) {
                int theight = 0;
                for (int ii = 0; ii < _rows; ii++) {
                    theight += cont.getComponent(ii).getPreferredSize(-1, -1).height;
                }
                pane.setPreferredSize(-1, theight);
            }
        }

        // determine whether we can fit in the window
        ArrayList<Component> children = null;
        int columns = _columns;
        do {
            Dimension d = getPreferredSize(-1, -1);
            if (d.height > height && _rows == 0) {
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

        // pack with a rough estimate of the preferred size, then lay out and pack with
        // the actual size
        pack();
        validate();
        pack();

        // adjust x and y to ensure that we fit on the screen
        x = Math.min(width - getWidth(), x);
        y = above ?
            Math.min(height - getHeight(), y) : Math.max(0, y - getHeight());
        setLocation(x, y);
    }

    /**
     * Returns a reference to the container holding the menu items.
     */
    protected Container getItemContainer ()
    {
        return (_rows == 0) ? this : (Container)((ScrollPane)getComponent(0)).getChild();
    }

    /**
     * Called by any child {@link MenuItem}s when they are selected.
     */
    protected void itemSelected (MenuItem item, long when, int modifiers)
    {
        emitEvent(new ActionEvent(item, when, modifiers, item.getAction(), item.getArgument()));
        dismiss();
    }

    protected int _rows, _columns;
}
