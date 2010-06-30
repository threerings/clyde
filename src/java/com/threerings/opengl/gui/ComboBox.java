//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;

/**
 * Displays a selected value and allows that value to be changed by selecting from a popup menu.
 */
public class ComboBox extends Label
{
    /** Used for displaying a label that is associated with a particular non-displayable value. */
    public static class Item
        implements Comparable<Item>
    {
        public Object value;

        public Item (Object value, String label) {
            this.value = value;
            _label = label;
        }

        public String toString () {
            return _label;
        }

        public boolean equals (Object other) {
            Item oitem = (Item)other;
            return (value == null) ? (oitem.value == null) : value.equals(oitem.value);
        }

        public int compareTo (Item other) {
            return _label.compareTo(other._label);
        }

        protected String _label;
    }

    /**
     * Creates an empty combo box.
     */
    public ComboBox (GlContext ctx)
    {
        super(ctx, "");
        setFit(Fit.TRUNCATE);
    }

    /**
     * Creates a combo box with the supplied set of items. The result of {@link Object#toString}
     * for each item will be displayed in the list.
     */
    public ComboBox (GlContext ctx, Object[] items)
    {
        super(ctx, "");
        setItems(items);
    }

    /**
     * Creates a combo box with the supplied set of items. The result of {@link Object#toString}
     * for each item will be displayed in the list.
     */
    public ComboBox (GlContext ctx, Iterable<?> items)
    {
        super(ctx, "");
        setItems(items);
    }

    /**
     * Appends an item to our list of items. The result of {@link Object#toString} for the item
     * will be displayed in the list.
     */
    public void addItem (Object item)
    {
        addItem(_items.size(), item);
    }

    /**
     * Inserts an item into our list of items at the specified position (zero being before all
     * other items and so forth).  The result of {@link Object#toString} for the item will be
     * displayed in the list.
     */
    public void addItem (int index, Object item)
    {
        _items.add(index, new ComboMenuItem(_ctx, item));
        clearCache();
    }

    /**
     * Replaces any existing items in this combo box with the supplied items.
     */
    public void setItems (Iterable<?> items)
    {
        clearItems();
        for (Object item : items) {
            addItem(item);
        }
    }

    /**
     * Replaces any existing items in this combo box with the supplied items.
     */
    public void setItems (Object[] items)
    {
        clearItems();
        for (int ii = 0; ii < items.length; ii++) {
            addItem(items[ii]);
        }
    }

    /**
     * Returns the index of the selected item or -1 if no item is selected.
     */
    public int getSelectedIndex ()
    {
        return _selidx;
    }

    /**
     * Returns the selected item or null if no item is selected.
     */
    public Object getSelectedItem ()
    {
        return getItem(_selidx);
    }

    /**
     * Requires that the combo box be configured with {@link Item} items, returns the {@link
     * Item#value} of the currently selected item.
     */
    public Object getSelectedValue ()
    {
        return getValue(_selidx);
    }

    /**
     * Selects the item with the specified index.
     */
    public void selectItem (int index)
    {
        selectItem(index, 0L, 0);
    }

    /**
     * Selects the item with the specified index. <em>Note:</em> the supplied item is compared with
     * the item list using {@link Object#equals}.
     */
    public void selectItem (Object item)
    {
        int selidx = -1;
        for (int ii = 0, ll = _items.size(); ii < ll; ii++) {
            ComboMenuItem mitem = _items.get(ii);
            if (mitem.item.equals(item)) {
                selidx = ii;
                break;
            }
        }
        selectItem(selidx);
    }

    /**
     * Requires that the combo box be configured with {@link Item} items, selects the item with a
     * {@link Item#value} equal to the supplied value.
     */
    public void selectValue (Object value)
    {
        // Item.equals only compares the values
        selectItem(new Item(value, ""));
    }

    /**
     * Returns the number of items in this combo box.
     */
    public int getItemCount ()
    {
        return _items.size();
    }

    /**
     * Returns the item at the specified index.
     */
    public Object getItem (int index)
    {
        return (index < 0 || index >= _items.size()) ? null : _items.get(index).item;
    }

    /**
     * Returns the value at the specified index, the item must be an instance of {@link Item}.
     */
    public Object getValue (int index)
    {
        return (index < 0 || index >= _items.size()) ? null : ((Item)_items.get(index).item).value;
    }

    /**
     * Removes all items from this combo box.
     */
    public void clearItems ()
    {
        clearCache();
        _items.clear();
        _selidx = -1;
    }

    /**
     * Sets the preferred number of columns in the popup menu.
     */
    public void setPreferredDimensions (int rows, int columns)
    {
        _rows = rows;
        _columns = columns;
        if (_menu != null) {
            _menu.setPreferredDimensions(rows, columns);
        }
    }

    @Override // from Component
    public boolean dispatchEvent (Event event)
    {
        if (event instanceof MouseEvent && isEnabled()) {
            MouseEvent mev = (MouseEvent)event;
            switch (mev.getType()) {
            case MouseEvent.MOUSE_PRESSED:
                if (_menu == null) {
                    _menu = new ComboPopupMenu(_ctx, _rows, _columns);
                }
                _menu.popup(getAbsoluteX(), getAbsoluteY(), false);
                break;

            case MouseEvent.MOUSE_RELEASED:
                break;

            default:
                return super.dispatchEvent(event);
            }

            return true;
        }

        return super.dispatchEvent(event);
    }

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return "Default/ComboBox";
    }

    @Override // from Component
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        // our preferred size is based on the widest of our items; computing this is rather
        // expensive, so we cache it like we do the menu
        if (_psize == null) {
            _psize = new Dimension();
            LabelRenderer label = new LabelRenderer(this);
            for (ComboMenuItem mitem : _items) {
                if (mitem.item instanceof Icon) {
                    label.setIcon((Icon)mitem.item);
                } else {
                    label.setText(mitem.item == null ? "" : mitem.item.toString());
                }
                Dimension lsize = label.computePreferredSize(-1, -1);
                _psize.width = Math.max(_psize.width, lsize.width);
                _psize.height = Math.max(_psize.height, lsize.height);
            }
        }
        return new Dimension(_psize);
    }

    protected void selectItem (int index, long when, int modifiers)
    {
        if (_selidx == index) {
            return;
        }

        _selidx = index;
        Object item = getSelectedItem();
        if (item instanceof Icon) {
            setIcon((Icon)item);
        } else {
            setText(item == null ? "" : item.toString());
        }
        emitEvent(new ActionEvent(this, when, modifiers, "selectionChanged"));
    }

    protected void clearCache ()
    {
        if (_menu != null) {
            _menu.removeAll();
            _menu = null;
        }
        _psize = null;
    }

    protected class ComboPopupMenu extends PopupMenu
    {
        public ComboPopupMenu (GlContext ctx, int rows, int columns) {
            super(ctx, ComboBox.this.getWindow(), rows, columns);
            for (int ii = 0; ii < _items.size(); ii++) {
                addMenuItem(_items.get(ii));
            }
        }

        protected void itemSelected (MenuItem item, long when, int modifiers) {
            selectItem(_items.indexOf(item), when, modifiers);
            dismiss();
        }

        protected void packAndFit (int x, int y, boolean above) {
            super.packAndFit(x, y, above);

            // ensure selected item is visible
            if (_rows == 0) {
                return;
            }
            ScrollPane pane = (ScrollPane)getComponent(0);
            Container cont = (Container)pane.getChild();
            int height = 0;
            for (int ii = 0; ii < _selidx; ii++) {
                height += cont.getComponent(ii).getPreferredSize(-1, -1).height;
            }
            BoundedRangeModel model = pane.getVerticalScrollBar().getModel();
            height = Math.min(height, model.getMaximum() - model.getExtent());
            model.setRange(model.getMinimum(), height, model.getExtent(), model.getMaximum());
        }

        protected Dimension computePreferredSize (int whint, int hhint) {
            // prefer a size that is at least as wide as the combobox from which we will popup
            Dimension d = super.computePreferredSize(whint, hhint);
            d.width = Math.max(d.width, ComboBox.this.getWidth() - getInsets().getHorizontal());
            return d;
        }
    };

    protected class ComboMenuItem extends MenuItem
    {
        public Object item;

        public ComboMenuItem (GlContext ctx, Object item)
        {
            super(ctx, null, null, "select");
            if (item instanceof Icon) {
                setIcon((Icon)item);
            } else {
                setText(item.toString());
            }
            this.item = item;
        }
    }

    /** The index of the currently selected item. */
    protected int _selidx = -1;

    /** The list of items in this combo box. */
    protected ArrayList<ComboMenuItem> _items = new ArrayList<ComboMenuItem>();

    /** A cached popup menu containing our items. */
    protected ComboPopupMenu _menu;

    /** Our cached preferred size. */
    protected Dimension _psize;

    /** Our preferred number of rows and columns for the popup menu. */
    protected int _rows, _columns;
}
