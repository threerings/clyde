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

import com.threerings.config.ConfigReference;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.MouseWheelListener;
import com.threerings.opengl.gui.layout.BorderLayout;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;

import static com.threerings.opengl.Log.log;

/**
 * Provides a scrollable clipped view on a sub-heirarchy of components.
 */
public class ScrollPane extends Container
{
    /**
     * Non-initializing constructor for config init.
     */
    public ScrollPane (GlContext ctx)
    {
        super(ctx, new BorderLayout(0, 0));
    }

    /**
     * Initializing constructor.
     */
    public ScrollPane (GlContext ctx, Component child)
    {
        this(ctx, child, true, false);
    }

    /**
     * Initializing constructor.
     */
    public ScrollPane (GlContext ctx, Component child, boolean vert, boolean horiz)
    {
        this(ctx, child, vert, horiz, -1);
    }

    /**
     * Initializing constructor.
     */
    public ScrollPane (
        GlContext ctx, Component child, boolean vert, boolean horiz, int snap)
    {
        this(ctx, child, vert, horiz, snap, false);
    }

    /**
     * Initializing constructor.
     */
    public ScrollPane (
        GlContext ctx, Component child, boolean vert, boolean horiz, int snap, boolean buttons)
    {
        super(ctx, new BorderLayout(0, 0));
        init(child, vert, horiz, snap, buttons);
    }

    /**
     * Initialization for Scroll Pane
     */
    public void init (Component child, boolean vert, boolean horiz, int snap, boolean buttons)
    {
        add(_vport = new Viewport(_ctx, child, vert, horiz, snap),
                BorderLayout.CENTER);
        _showAlways = buttons;
        if (vert) {
            if (buttons) {
                add(_vlbtn = new ScrollButton(_ctx, true, ScrollBar.VERTICAL,
                        _vport.getVModel()), BorderLayout.NORTH);
                add(_vmbtn = new ScrollButton(_ctx, false, ScrollBar.VERTICAL,
                        _vport.getVModel()), BorderLayout.SOUTH);
            } else {
                add(_vbar = new ScrollBar(_ctx, ScrollBar.VERTICAL,
                        _vport.getVModel()), BorderLayout.EAST);
            }
        }
        if (horiz) {
            if (buttons) {
                add(_hlbtn = new ScrollButton(_ctx, true, ScrollBar.HORIZONTAL,
                        _vport.getHModel()), BorderLayout.WEST);
                add(_hmbtn = new ScrollButton(_ctx, false, ScrollBar.HORIZONTAL,
                        _vport.getHModel()), BorderLayout.EAST);
            } else {
                add(_hbar = new ScrollBar(_ctx, ScrollBar.HORIZONTAL,
                        _vport.getHModel()), BorderLayout.SOUTH);
            }
        }
    }

    /**
     * Returns a reference to the child of this scroll pane.
     */
    public Component getChild ()
    {
        return _vport.getTarget();
    }

    /**
     * Returns a reference to the vertical scroll bar.
     */
    public ScrollBar getVerticalScrollBar ()
    {
        return _vbar;
    }

    /**
     * Returns a reference to the horizontal scroll bar.
     */
    public ScrollBar getHorizontalScrollBar ()
    {
        return _hbar;
    }

    /**
     * Toggles the scroll bar policy.  If set to true, the bars will always
     * show.  If set to false, the bars will only show when needed.
     */
    public void setShowScrollbarAlways (boolean showAlways)
    {
        if (_showAlways != showAlways && _vlbtn == null && _hlbtn == null) {
            _showAlways = showAlways;
            invalidate();
        }
    }

    /**
     * Configures the style of the viewport (the non-scrolling container that will hold the
     * scrolling contents).
     */
    public void setViewportStyleConfig (ConfigReference<StyleConfig> ref)
    {
        _vport.setStyleConfig(ref);
    }

    @Override
    public void layout ()
    {
        if (_layingOut || _vport.getTarget().isValid()) {
            super.layout();
            return;
        }
        _layingOut = true;
        boolean hadVBar = false, hadHBar = false;
        if (_vbar != null) {
            _vport.storeOldV();
            if (_showAlways && _vbar.getParent() == null) {
                add(_vbar, BorderLayout.EAST);
            } else if (!_showAlways && _vbar.getParent() != null) {
                remove(_vbar);
                hadVBar = true;
            }
        }
        if (_vlbtn != null) {
            _vport.storeOldV();
            if (_vlbtn.getParent() == null) {
                add(_vlbtn, BorderLayout.NORTH);
                add(_vmbtn, BorderLayout.SOUTH);
            }
        }
        if (_hbar != null) {
            _vport.storeOldH();
            if (_showAlways && _hbar.getParent() == null) {
                add(_hbar, BorderLayout.SOUTH);
            } else if (!_showAlways && _hbar.getParent() != null) {
                remove(_hbar);
                hadHBar = true;
            }
        }
        if (_hlbtn != null) {
            _vport.storeOldH();
            if (_hlbtn.getParent() == null) {
                add(_hlbtn, BorderLayout.WEST);
                add(_hmbtn, BorderLayout.EAST);
            }
        }
        validate();
        if (_showAlways) {
            _layingOut = false;
            return;
        }
        boolean hadded = false, vadded = false;
        // Add a horizontal bar if needed
        if (_hbar != null) {
            BoundedRangeModel hmodel = _hbar.getModel();
            if (hmodel.getExtent() != hmodel.getRange()) {
                add(_hbar, BorderLayout.SOUTH);
                validate();
                hadded = true;
            }
        }
        // Add a vertical bar if needed
        if (_vbar != null) {
            BoundedRangeModel vmodel = _vbar.getModel();
            if (vmodel.getExtent() != vmodel.getRange()) {
                add(_vbar, BorderLayout.EAST);
                validate();
                vadded = true;
            }
        }
        // Check if adding the vertical bar now requires the horizontal bar
        // to be added
        if (vadded && !hadded && _hbar != null) {
            BoundedRangeModel hmodel = _hbar.getModel();
            if (hmodel.getExtent() != hmodel.getRange()) {
                add(_hbar, BorderLayout.SOUTH);
                validate();
                hadded = true;
            }
        }
        if (!_revalidating && (hadVBar != vadded || hadHBar != hadded)) {
            final Window window = getWindow();
            Root root;
            if (window != null && (root = window.getRoot()) != null) {
                _revalidating = true;
                root.rootInvalidated(new Validateable() {
                    @Override public boolean isAdded () {
                        return window.isAdded();
                    }
                    @Override public void validate () {
                        ScrollPane.this.invalidate();
                        window.validate();
                        _revalidating = false;
                    }
                });
            }
        }
        _layingOut = false;
    }

    /** Does all the heavy lifting for the {@link ScrollPane}. */
    protected static class Viewport extends Container
    {
        public Viewport (
            GlContext ctx, Component target, boolean vert, boolean horiz, int snap)
        {
            super(ctx);
            if (vert) {
                if (snap > 0) {
                    _vmodel = new BoundedSnappingRangeModel(0, 0, 10, 10, snap);
                } else {
                    _vmodel = new BoundedRangeModel(0, 0, 10, 10);
                }
            }
            if (horiz) {
                if (snap > 0) {
                    _hmodel = new BoundedSnappingRangeModel(0, 0, 10, 10, snap);
                } else {
                    _hmodel = new BoundedRangeModel(0, 0, 10, 10);
                }
            }
            add(_target = target);
        }

        /**
         * Returns a reference to the target of this viewport.
         */
        public Component getTarget ()
        {
            return _target;
        }

        /**
         * Returns the range model defined by this viewport's size and the
         * preferred size of its target component.
         */
        public BoundedRangeModel getVModel ()
        {
            return _vmodel;
        }

        /**
         * Returns the range model defined by this viewport's size and the
         * preferred size of its target component.
         */
        public BoundedRangeModel getHModel ()
        {
            return _hmodel;
        }

        /**
         * Stores our current vertical bound value.
         */
        public void storeOldV ()
        {
            _oldV = (_vmodel == null) ? 0 : _vmodel.getValue();
        }

        /**
         * Stores our current horizontal bound value.
         */
        public void storeOldH ()
        {
            _oldH = (_hmodel == null) ? 0 : _hmodel.getValue();
        }

        // documentation inherited
        public void layout ()
        {
            // resize our target component to the larger of our size and its
            // preferred size
            Insets insets = getInsets();
            int twidth = getWidth() - insets.getHorizontal();
            int theight = getHeight() - insets.getVertical();
            Dimension d = _target.getPreferredSize(twidth, theight);
            d.width = (_hmodel != null) ?
                Math.max(d.width, twidth) : twidth;
            d.height = (_vmodel != null) ?
                Math.max(d.height, theight) : theight;
            if (_target.getWidth() != d.width ||
                _target.getHeight() != d.height) {
                _target.setBounds(insets.left, insets.bottom, d.width,
                    d.height);
            }

            // lay out our target component
            _target.layout();

            // and recompute our scrollbar range
            if (_vmodel != null) {
                int extent = getHeight() - insets.getVertical();
                int value = Math.max(0,Math.min(_oldV, d.height - extent));
                _vmodel.setRange(0, value, extent, d.height);
            }
            if (_hmodel != null) {
                int extent = getWidth() - insets.getHorizontal();
                int value = Math.max(0,Math.min(_oldH, d.width - extent));
                _hmodel.setRange(0, value, extent, d.width);
            }
        }

        // documentation inherited
        public int getAbsoluteX ()
        {
            return super.getAbsoluteX() + getXOffset();
        }

        // documentation inherited
        public int getAbsoluteY ()
        {
            return super.getAbsoluteY() + getYOffset();
        }

        // documentation inherited
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
            mx -= (_x + getXOffset());
            my -= (_y + getYOffset());

            Component hit = null;
            for (int ii = 0, ll = getComponentCount(); ii < ll; ii++) {
                Component child = getComponent(ii);
                if ((hit = child.getHitComponent(mx, my)) != null) {
                    return hit;
                }
            }
            return this;
        }

        @Override
        public void scrollRectToVisible (int x, int y, int w, int h)
        {
            Insets insets = getInsets();
            if (_vmodel != null) {
                int dy = positionAdjustment(_vmodel.getExtent(), h,
                    (_vmodel.getMaximum() - (y - insets.bottom) - h) - _vmodel.getValue());
                if (dy != 0) {
                    _vmodel.setValue(_vmodel.getValue() + dy);
                }
            }
            if (_hmodel != null) {
                int dx = positionAdjustment(_hmodel.getExtent(), w,
                    x - insets.left - _hmodel.getValue());
                if (dx != 0) {
                    _hmodel.setValue(_hmodel.getValue() + dx);
                }
            }
        }

        /**
         * Helper for scrollRectToVisible, pretty much copied from JViewport.java.
         */
        protected int positionAdjustment (int parentExtent, int childExtent, int childPos)
        {
            //   +-----+
            //   | --- |     No Change
            //   +-----+
            if (childPos >= 0 && childExtent + childPos <= parentExtent)    {
                return 0;
            }

            //   +-----+
            //  ---------   No Change
            //   +-----+
            if (childPos <= 0 && childExtent + childPos >= parentExtent) {
                return 0;
            }

            //   +-----+          +-----+
            //   |   ----    ->   | ----|
            //   +-----+          +-----+
            if (childPos > 0 && childExtent <= parentExtent)    {
                return childPos - parentExtent + childExtent;
            }

            //   +-----+             +-----+
            //   |  --------  ->     |--------
            //   +-----+             +-----+
            if (childPos >= 0 && childExtent >= parentExtent)   {
                return childPos;
            }

            //   +-----+          +-----+
            // ----    |     ->   |---- |
            //   +-----+          +-----+
            if (childPos <= 0 && childExtent <= parentExtent)   {
                return childPos;
            }

            //   +-----+             +-----+
            //-------- |      ->   --------|
            //   +-----+             +-----+
            if (childPos < 0 && childExtent >= parentExtent)    {
                return childPos - parentExtent + childExtent;
            }

            return 0;
        }

        // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();
            if (_vmodel != null) {
                addListener(_wheelListener = _vmodel.createWheelListener());
            } else if (_hmodel != null) {
                addListener(_wheelListener = _hmodel.createWheelListener());
            }
        }

        // documentation inherited
        protected void wasRemoved ()
        {
            super.wasRemoved();
            if (_wheelListener != null) {
                removeListener(_wheelListener);
                _wheelListener = null;
            }
        }

        @Override
        protected String getDefaultStyleConfig ()
        {
            return "Default/Viewport";
        }

        // documentation inherited
        protected Dimension computePreferredSize (int whint, int hhint)
        {
            Dimension dim = new Dimension(_target.getPreferredSize(whint, hhint));
            // The viewport contents of the viewport can grow however large they want, but make
            // suer the viewport itself doesn't go beyond its desired size
            if (hhint != -1 && dim.height > hhint) {
                dim.height = hhint;
            }
            if (whint != -1 && dim.width > whint) {
                dim.width = whint;
            }
            return dim;
        }

        // documentation inherited
        protected void renderComponent (Renderer renderer)
        {
            // translate by our offset into the viewport
            Insets insets = getInsets();
            int yoffset = getYOffset();
            int xoffset = getXOffset();
            GL11.glTranslatef(xoffset, yoffset, 0);
            Rectangle oscissor = intersectScissor(
                renderer, _srect,
                (getAbsoluteX() + insets.left) - xoffset,
                (getAbsoluteY() + insets.bottom) - yoffset,
                _width - insets.getHorizontal(),
                _height - insets.getVertical());
            try {
                // and then render our target component
                _target.render(renderer);
            } finally {
                renderer.setScissor(oscissor);
                GL11.glTranslatef(-xoffset, -yoffset, 0);
            }
        }

        protected final int getYOffset ()
        {
            return _vmodel == null ? 0 : _vmodel.getValue() -
                (_vmodel.getMaximum() - _vmodel.getExtent());
        }

        protected final int getXOffset ()
        {
            return _hmodel == null ? 0 : -_hmodel.getValue();
        }

        protected BoundedRangeModel _vmodel, _hmodel;
        protected Component _target;
        protected MouseWheelListener _wheelListener;
        protected Rectangle _srect = new Rectangle();
        protected int _oldV, _oldH;
    }

    protected Viewport _vport;
    protected ScrollBar _vbar, _hbar;
    protected ScrollButton _vlbtn, _vmbtn, _hlbtn, _hmbtn;
    protected boolean _showAlways = true, _layingOut, _revalidating;
}
