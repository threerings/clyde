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

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.icon.Icon;
import com.threerings.opengl.gui.util.Dimension;

/**
 * Displays a selected value and allows that value to be changed by selecting from a popup menu.
 */
public class ComboBox<T> extends Label
    implements Selectable<T>
{
    /**
     * Formats items added to a ComboBox for display.
     */
    public interface Formatter<T>
    {
        /**
         * Get the label (may not be null) for the specified item.
         */
        String getText (@Nullable T o);

        /**
         * Get the icon, or null, for the specified item.
         */
        @Nullable Icon getIcon (@Nullable T o);

        // TODO: Style?
    }

    /**
     * The default formatter, which may be extended to customize behavior.
     *
     * getText() will return the toString() value of the object, or "" if the object
     *    is an Icon instance or null.
     * <br><br>
     * getIcon() will return null unless the object is an Icon instance,
     *    in which case it is returned.
     */
    public static class DefaultFormatter<T>
        implements Formatter<T>
    {
        /** A sharable instance of the default formatter. */
        public static final DefaultFormatter<Object> INSTANCE = new DefaultFormatter<Object>();

        // from Formatter
        public String getText (T o)
        {
            return ((o == null) || (o instanceof Icon)) ? "" : String.valueOf(o);
        }

        // from Formatter
        public Icon getIcon (T o)
        {
            return (o instanceof Icon) ? (Icon)o : null;
        }
    }

    /**
     * Creates an empty combo box.
     */
    public ComboBox (GlContext ctx)
    {
        this(ctx, ImmutableList.<T>of());
    }

    /**
     * Creates a combo box with the supplied items, formatted with the default formatter.
     */
    public ComboBox (GlContext ctx, Iterable<? extends T> items)
    {
        this(ctx, items, DefaultFormatter.INSTANCE);
    }

    /**
     * Creates a combo box with the supplied items, and formatted according to the formatter.
     */
    public ComboBox (GlContext ctx, Iterable<? extends T> items, Formatter<? super T> formatter)
    {
        super(ctx, "");
        setFit(Fit.TRUNCATE);
        setItems(items, formatter);
    }

    /**
     * Removes all items from this combo box.
     */
    public void clearItems ()
    {
        clearCache();
        _items.clear();
        // if we previously had a selection, be sure to clear the label
        setSelectedIndex(-1);
    }

    /**
     * Replaces any existing items in this combo box with the supplied items.
     */
    public void setItems (Iterable<? extends T> items)
    {
        setItems(items, DefaultFormatter.INSTANCE);
    }

    /**
     * Replaces any existing items in this combo box with the supplied items.
     */
    public void setItems (Iterable<? extends T> items, Formatter<? super T> formatter)
    {
        clearItems();
        for (T item : items) {
            addItem(item, formatter);
        }
    }

    /**
     * Appends an item to our list of items. The item will be formatted using the default formatter.
     */
    public void addItem (T item)
    {
        addItem(item, DefaultFormatter.INSTANCE);
    }

    /**
     * Appends an item to our list of items.
     */
    public void addItem (T item, Formatter<? super T> formatter)
    {
        addItem(item, formatter.getText(item), formatter.getIcon(item));
    }

    /**
     * Appends an item to our list of items, with the specified text label and no icon.
     */
    public void addItem (T item, String text)
    {
        addItem(item, text, null);
    }

    /**
     * Appends an item to our list of items, with the specified text label and icon.
     */
    public void addItem (T item, String text, @Nullable Icon icon)
    {
        addItem(_items.size(), item, text, icon);
    }

    /**
     * Adds an item at the specified index, formatted with the default formatter.
     */
    public void addItem (int index, T item)
    {
        addItem(index, item, DefaultFormatter.INSTANCE);
    }

    /**
     * Adds an item at the specified index, formatted with the specified formatter.
     */
    public void addItem (int index, T item, Formatter<? super T> formatter)
    {
        addItem(index, item, formatter.getText(item), formatter.getIcon(item));
    }

    /**
     * Adds an item at the specified index, with the specified text label and no icon.
     */
    public void addItem (int index, T item, String text)
    {
        addItem(index, item, text, null);
    }

    /**
     * Adds an item at the specified index, with the specified text label and icon.
     */
    public void addItem (int index, T item, String text, @Nullable Icon icon)
    {
        Preconditions.checkNotNull(text);
        ComboMenuItem<T> menuItem = new ComboMenuItem<T>(_ctx, item, text, icon);
        _items.add(index, menuItem);
        // maybe adjust the selected index
        if (index <= _selidx) {
            _selidx++;

        } else if (_items.size() == 1) {
            // first item added: select it
            setSelectedIndex(0);
        }
        clearCache();
    }

    // from Selectable<T>
    public T getSelected ()
    {
        return getItem(_selidx);
    }

    // from Selectable<T>
    public void setSelected (T item)
    {
        setSelectedIndex(getItems().indexOf(item));
    }

    // from Selectable<T>
    public int getSelectedIndex ()
    {
        return _selidx;
    }

    // from Selectable<T>
    public void setSelectedIndex (int index)
    {
        selectItem(index, System.currentTimeMillis(), 0);
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
    public T getItem (int index)
    {
        return invalidIndex(index) ? null : _items.get(index).item;
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
                return true;

            case MouseEvent.MOUSE_RELEASED:
                return true;
            }
        }

        return super.dispatchEvent(event);
    }

    @Override
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
            for (ComboMenuItem<T> mitem : _items) {
                label.setIcon(mitem.getIcon());
                label.setText(mitem.getText());
                Dimension lsize = label.computePreferredSize(-1, -1);
                _psize.width = Math.max(_psize.width, lsize.width);
                _psize.height = Math.max(_psize.height, lsize.height);
            }
        }
        return new Dimension(_psize);
    }

    protected void selectItem (int index, long when, int modifiers)
    {
        if (invalidIndex(index)) {
            index = -1;
        }
        if (_selidx == index) {
            return;
        }

        _selidx = index;
        Object selItem;
        if (index == -1) {
            this.setIcon(null);
            this.setText("");
            selItem = null;

        } else {
            ComboMenuItem<T> selected = _items.get(index);
            this.setIcon(selected.getIcon());
            this.setText(selected.getText());
            selItem = selected.item;
        }
        emitEvent(new ActionEvent(this, when, modifiers, SELECT, selItem));
    }

    protected void clearCache ()
    {
        if (_menu != null) {
            _menu.removeAll();
            _menu = null;
        }
        _psize = null;
    }

    protected boolean invalidIndex (int index)
    {
        return (index < 0 || index >= _items.size());
    }

    /**
     * Get the raw list of items.
     */
    protected List<T> getItems ()
    {
        return Lists.transform(_items,
            new Function<ComboMenuItem<T>, T>() {
                public T apply (ComboMenuItem<T> item) {
                    return item.item;
                }
            });
    }

    /**
     * Popup portion, allowing selection from amongst the items.
     */
    protected class ComboPopupMenu extends PopupMenu
    {
        public ComboPopupMenu (GlContext ctx, int rows, int columns) {
            super(ctx, ComboBox.this.getWindow(), rows, columns);
            for (int ii = 0; ii < _items.size(); ii++) {
                addMenuItem(_items.get(ii));
            }
        }

        @Override
        protected void itemSelected (MenuItem item, long when, int modifiers) {
            selectItem(_items.indexOf(item), when, modifiers);
            dismiss();
        }

        @Override
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

        @Override
        protected Dimension computePreferredSize (int whint, int hhint) {
            // prefer a size that is at least as wide as the combobox from which we will popup
            Dimension d = super.computePreferredSize(whint, hhint);
            d.width = Math.max(d.width, ComboBox.this.getWidth() - getInsets().getHorizontal());
            return d;
        }
    }

    /**
     * Extends MenuItem with a reference to the underlying data item.
     */
    protected static class ComboMenuItem<T> extends MenuItem
    {
        /** The item data. */
        public T item;

        public ComboMenuItem (GlContext ctx, T item, String text, Icon icon)
        {
            super(ctx, text, icon, "select");
            this.item = item;
        }
    }

    /** The index of the currently selected item. */
    protected int _selidx = -1;

    /** The list of items in this combo box. */
    protected List<ComboMenuItem<T>> _items = Lists.newArrayList();

    /** A cached popup menu containing our items. */
    protected ComboPopupMenu _menu;

    /** Our cached preferred size. */
    protected Dimension _psize;

    /** Our preferred number of rows and columns for the popup menu. */
    protected int _rows, _columns;
}
