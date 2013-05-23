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

import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.event.MouseWheelListener;
import com.threerings.opengl.gui.event.ChangeListener;
import com.threerings.opengl.gui.event.ChangeEvent;
import com.threerings.opengl.gui.layout.BorderLayout;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.gui.layout.GroupLayout;

/**
 * Provides a scrollable, lazily instantiated component view of values
 */
public abstract class ScrollingList<V, C extends Component> extends Container
{
    /**
     * Instantiates an empty {@link ScrollingList}.
     */
    public ScrollingList (GlContext ctx)
    {
        this(ctx, null);
    }

    /**
     * Instantiates a {@link ScrollingList} with an initial value collection.
     */
    public ScrollingList (GlContext ctx, Collection<? extends V> values)
    {
        super(ctx, new BorderLayout(0, 0));

        _values = new ArrayList<Entry<V, C>>();
        if (values != null) {
            for (V value : values) {
                _values.add(new Entry<V, C>(value));
            }
        }
        _lastBottom = 0;

        // we'll set up our model in layout()
        _model = new BoundedRangeModel(0, 0, 1, 1);

        // create our viewport and scrollbar
        add(_vport = new Viewport(ctx), BorderLayout.CENTER);
        _model.addChangeListener(_vport);
        add(_vbar = new ScrollBar(_ctx, ScrollBar.VERTICAL, _model), BorderLayout.EAST);
    }

    /**
     * Appends a value to our list, possibly scrolling our view to display it.
     */
    public void addValue (V value, boolean snapToBottom)
    {
        addValue(_values.size(), value, snapToBottom);
    }

    /**
     * Inserts a value into our list at the specified position.
     */
    public void addValue (int index, V value)
    {
        addValue(index, value, false);
    }

    /**
     * Inserts a collection of values into our list.
     */
    public void addValues (Collection<? extends V> values)
    {
        addValues(_values.size(), values);
    }

    /**
     * Inserts a collection of values into our list starting at the specified position.
     */
    public void addValues (int index, Collection<? extends V> values)
    {
        for (V value : values) {
            _values.add(index++, new Entry<V, C>(value));
        }
        _vport.invalidate();
    }

    /**
     * Clears all the current values and any related components.
     */
    public void removeValues ()
    {
        _values.clear();
        _model.setValue(0);
        _vport.removeAll();
        _vport.invalidate();
    }

    /**
     * Returns the index of a value.
     */
    public int getIndex (V value)
    {
        for (int ii = 0, nn = _values.size(); ii < nn; ii++) {
            if (_values.get(ii).value == value) {
                return ii;
            }
        }
        return -1;
    }

    /**
     * Removes values from the top of the list.
     */
    public void removeValuesFromTop (int num)
    {
        removeValuesAt(0, num);
    }

    /**
     * Removes values starting at a specific index.
     */
    public void removeValuesAt (int index, int num)
    {
        num = Math.min(index + num, _values.size());
        for (int ii = index; ii < num; ii++) {
            Entry<V, C> value = _values.remove(index);
            _vport.remove(value.component);
        }
        _vport.invalidate();
    }

    /**
     * Snaps the view so the value is at the top.
     */
    public void snapToValue (V value)
    {
        _vport.invalidateAndSnapTo(value);
    }

    /**
     * Must be implemented by subclasses to instantiate the correct Component
     * subclass for a given list value.
     */
    protected abstract C createComponent (V value);

    /**
     * Calculates the height of an entry.
     */
    protected void computeHeight (Entry<V, C> entry, Container container)
    {
        if (entry.height < 0) {
            if (entry.component == null) {
                entry.component = createComponent(entry.value);
            }
            boolean remove = false;
            if (!entry.component.isAdded()) {
                container.add(entry.component);
                remove = true;
            }
            int twidth = container.getWidth() - container.getInsets().getHorizontal();
            entry.height =
                entry.component.getPreferredSize(twidth, 0).height;
            if (remove) {
                container.remove(entry.component);
            }
        }
    }

    /**
     * Adds a value to the list and snaps to the bottom of the list if desired.
     */
    protected void addValue (int index, V value, boolean snap)
    {
        _values.add(index, new Entry<V, C>(value));
        if (snap) {
            _vport.invalidateAndSnap();
        } else {
            _vport.invalidate();
        }
    }

    /** Does all the heavy lifting for the {@link ScrollingList}. */
    protected class Viewport extends Container
        implements ChangeListener
    {
        public Viewport (GlContext ctx)
        {
            super(ctx, GroupLayout.makeVert(
                GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        }

        /**
         * Sets the gap.
         */
        public void setGap (int gap)
        {
            ((GroupLayout)getLayoutManager()).setGap(gap);
        }

        /**
         * Returns a reference to the vertical scroll bar.
         */
        public ScrollBar getVerticalScrollBar ()
        {
            return _vbar;
        }

        /**
         * Recomputes our layout and snaps to the bottom if we were at the
         * bottom previously.
         */
        public void invalidateAndSnap ()
        {
            _snap =
                _model.getValue() + _model.getExtent() >= _model.getMaximum();
            invalidate();
        }

        /**
         * Recomputes our layout and snaps to the specified value.
         */
        public void invalidateAndSnapTo (V value)
        {
            _snapValue = value;
            invalidate();
        }

        @Override
        protected void wasAdded ()
        {
            super.wasAdded();
            addListener(_wheelListener = _model.createWheelListener());
        }

        @Override // from Component
        protected void wasRemoved ()
        {
            super.wasRemoved();
            if (_wheelListener != null) {
                removeListener(_wheelListener);
                _wheelListener = null;
            }
        }

        // from interface ChangeListener
        public void stateChanged (ChangeEvent event)
        {
            invalidate();
        }

        @Override // from Component
        public void invalidate ()
        {
            // if we're not attached, don't worry about it
            Window window;
            Root root;
            if (!_valid || (window = getWindow()) == null ||
                (root = window.getRoot()) == null) {
                return;
            }

            _valid = false;
            root.rootInvalidated(this);
        }

        @Override // from Component
        public void layout ()
        {
            int theight = getHeight() - getInsets().getVertical();
            int gap = ((GroupLayout)getLayoutManager()).getGap();

            // first make sure all of our entries have been measured and
            // compute our total height and extent
            int totheight = 0;
            int snapheight = 0;
            for (int ii = 0, nn = _values.size(); ii < nn; ii++) {
                Entry<V, C> entry = _values.get(ii);
                computeHeight(entry, this);
                if (entry.value == _snapValue) {
                    snapheight = totheight + gap * ii;
                }
                totheight += entry.height;
            }
            if (_values.size() > 1) {
                totheight += (gap * _values.size()-1);
            }
            int extent = Math.min(theight, totheight);

            // if our most recent value was added with _snap then we scroll to
            // the bottom on this layout and clear our snappy flag
            int value = _model.getValue();
            if (_snap) {
                value = totheight-extent;
                _snap = false;
            } else if (_snapValue != null) {
                _snapValue = null;
                value = Math.min(totheight - extent, snapheight);
                _model.setValue(value);
            }

            // if our extent or total height have changed, update the model
            // (because we're currently invalid, the resulting call to
            // invalidate() will have no effect)
            if (extent != _model.getExtent() ||
                totheight != _model.getMaximum()) {
                value = Math.min(value, totheight - extent);
                _model.setRange(0, value, extent, totheight);
            }

            // now back up from the last component until we reach the first one
            // that's in view
            _offset = _model.getValue();
            int compIx = 0;
            for (int ii = 0; ii < _values.size(); ii++) {
                Entry<V, C> entry = _values.get(ii);
                if (_offset < entry.height) {
                    compIx = ii;
                    break;
                }
                _offset -= (entry.height + gap);

                // remove and clear out the components before our top component
                if (entry.component != null) {
                    if (entry.component.isAdded()) {
                        remove(entry.component);
                    }
                    entry.component = null;
                }
            }

            // compensate for the partially visible topmost component
            extent += _offset;

            // now add components until we use up our extent
            int topIx = compIx;
            while (compIx < _values.size() && extent > 0) {
                Entry<V, C> entry = _values.get(compIx);
                if (entry.component == null) {
                    entry.component = createComponent(entry.value);
                }
                if (!entry.component.isAdded()) {
                    add(compIx-topIx, entry.component);
                }
                extent -= (entry.height + gap);
                compIx++;
            }

            // lastly remove any components below the last visible component
            while (compIx < _values.size()) {
                Entry<V, C> entry = _values.get(compIx);
                if (entry.component != null) {
                    if (entry.component.isAdded()) {
                        remove(entry.component);
                    }
                    entry.component = null;
                }
                compIx++;
            }

            // now have the layout manager layout our added components
            super.layout();
        }

        @Override // from Component
        protected void renderComponent (Renderer renderer)
        {
            Insets insets = getInsets();
            GL11.glTranslatef(0, _offset, 0);
            Rectangle oscissor = intersectScissor(
                renderer, _srect,
                getAbsoluteX() + insets.left,
                getAbsoluteY() + insets.bottom - _offset,
                _width - insets.getHorizontal(),
                _height - insets.getVertical());
            try {
                // render our children
                for (int ii = 0, ll = getComponentCount(); ii < ll; ii++) {
                    getComponent(ii).render(renderer);
                }
            } finally {
                renderer.setScissor(oscissor);
                GL11.glTranslatef(0, -_offset, 0);
            }
        }

        @Override
        public int getAbsoluteY ()
        {
            return super.getAbsoluteY() + _offset;
        }

        @Override
        public Component getHitComponent (int mx, int my)
        {
            // if we're not within our bounds, we needn't check our target
            Insets insets = getInsets();
            if ((mx < _x + insets.left) || (my < _y + insets.bottom) ||
                (mx >= _x + _width - insets.right) ||
                (my >= _y + _height - insets.top)) {
                return null;
            }

            // translate the coordinate into our children's coordinates
            mx -= _x;
            my -= (_y + _offset);

            Component hit = null;
            for (int ii = 0, ll = getComponentCount(); ii < ll; ii++) {
                Component child = getComponent(ii);
                if ((hit = child.getHitComponent(mx, my)) != null) {
                    return hit;
                }
            }
            return this;
        }

        protected int _offset;
        protected boolean _snap;
        protected V _snapValue;
        protected Rectangle _srect = new Rectangle();
    }

    /** Used to track the total height of our entries. */
    protected static class Entry<V, C extends Component>
    {
        public C component;
        public V value;
        public int height = -1;
        public Entry (V value) {
            this.value = value;
        }
    }

    protected MouseWheelListener _wheelListener;
    protected BoundedRangeModel _model;
    protected List<Entry<V, C>> _values;
    protected Viewport _vport;
    protected ScrollBar _vbar;
    protected int _lastBottom;

    protected static final int EXTENT = 2;
}
