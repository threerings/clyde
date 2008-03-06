//
// $Id$

package com.threerings.opengl.gui;

import java.util.ArrayList;

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
    public PopupMenu (Window parent)
    {
        this(parent, false);
    }

    public PopupMenu (Window parent, boolean horizontal)
    {
        super(parent, null);
        setLayoutManager(
                (horizontal ? GroupLayout.makeHStretch() : GroupLayout.makeVStretch()).setGap(0));
        _columns = 1;
        _modal = true;
    }

    public PopupMenu (Window parent, int columns)
    {
        super(parent, null);
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

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "popupmenu";
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
     * Called by any child {@link BMenuItem}s when they are selected.
     */
    protected void itemSelected (MenuItem item, long when, int modifiers)
    {
        emitEvent(new ActionEvent(item, when, modifiers, item.getAction()));
        dismiss();
    }

    protected int _columns;
}
