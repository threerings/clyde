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

import com.threerings.config.ConfigReference;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.ChangeEvent;
import com.threerings.opengl.gui.event.ChangeListener;
import com.threerings.opengl.gui.event.MouseAdapter;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.event.MouseListener;
import com.threerings.opengl.gui.event.MouseWheelListener;

import com.threerings.opengl.gui.layout.BorderLayout;
import com.threerings.opengl.gui.util.Insets;

/**
 * Displays a scroll bar for all your horizontal and vertical scrolling
 * needs.
 */
public class ScrollBar extends Container
    implements UIConstants
{
    /**
     * Creates a vertical scroll bar with the default range, value and
     * extent.
     */
    public ScrollBar (GlContext ctx)
    {
        this(ctx, VERTICAL);
    }

    /**
     * Creates a scroll bar with the default range, value and extent.
     */
    public ScrollBar (GlContext ctx, int orientation)
    {
        this(ctx, orientation, 0, 100, 0, 10);
    }

    /**
     * Creates a scroll bar with the specified orientation, range, value
     * and extent.
     */
    public ScrollBar (GlContext ctx, int orientation, int min, int value, int extent, int max)
    {
        this(ctx, orientation, new BoundedRangeModel(min, value, extent, max));
    }

    /**
     * Creates a scroll bar with the specified orientation which will
     * interact with the supplied model.
     */
    public ScrollBar (GlContext ctx, int orientation, BoundedRangeModel model)
    {
        super(ctx, new BorderLayout());
        _horz = (orientation == HORIZONTAL);
        _model = model;
        _model.addChangeListener(_updater);
        _wheelListener = _model.createWheelListener();

        // create our buttons and backgrounds
        _well = new Component(_ctx);
        _well.addListener(_wellListener);
        _thumb = new Component(_ctx);
        _thumb.addListener(_thumbListener);
        _less = new Button(_ctx, "");
        _less.addListener(_buttoner);
        _more = new Button(_ctx, "");
        _more.addListener(_buttoner);

        add(_well, BorderLayout.CENTER);
        add(_thumb, BorderLayout.IGNORE);
        add(_less, _horz ? BorderLayout.WEST : BorderLayout.NORTH);
        add(_more, _horz ? BorderLayout.EAST : BorderLayout.SOUTH);
    }

    /**
     * Returns a reference to the scrollbar's range model.
     */
    public BoundedRangeModel getModel ()
    {
        return _model;
    }

    @Override
    public void setStyleConfig (ConfigReference<StyleConfig> ref)
    {
        String prefix = (ref == null)
            ? getDefaultStyleConfig()
            : ref.getName();
        // set the styles of our sub-components
        _well.setStyleConfig(prefix + "Well");
        _thumb.setStyleConfig(prefix + "Thumb");
        _less.setStyleConfig(prefix + "Less");
        _more.setStyleConfig(prefix + "More");

        super.setStyleConfig(ref);
    }

    @Override
    protected void wasAdded ()
    {
        super.wasAdded();

        // listen for mouse wheel events
        addListener(_wheelListener);
    }

    @Override
    protected void wasRemoved ()
    {
        super.wasRemoved();

        removeListener(_wheelListener);
    }

    @Override
    public void setEnabled (boolean enabled)
    {
        super.setEnabled(enabled);
        update();
    }

    // documentation inherited
    public Component getHitComponent (int mx, int my)
    {
        // the thumb takes priority over the well
        if (_thumb.getHitComponent(mx - _x, my - _y) != null) {
            return _thumb;
        }
        return super.getHitComponent(mx, my);
    }

    /**
     * Recomputes and repositions the scroll bar thumb to reflect the
     * current configuration of the model.
     */
    protected void update ()
    {
        if (!isAdded()) {
            return;
        }

        // enable/disable the buttons
        int value = _model.getValue();
        int extent = _model.getExtent();
        int range = _model.getRange();
        boolean enabled = isEnabled();
        _less.setEnabled(enabled && value > _model.getMinimum());
        _more.setEnabled(enabled && value + extent < _model.getMaximum());
        _thumb.setEnabled(enabled && range > extent);

        Insets winsets = _well.getInsets();
        int tx = 0, ty = 0;
        int twidth = _well.getWidth() - winsets.getHorizontal();
        int theight = _well.getHeight() - winsets.getVertical();
        range = Math.max(1, range); // avoid div0

        if (_horz) {
            int wellSize = twidth;
            twidth = extent * wellSize / range;
            if (twidth < THUMB_RATIO * theight) {
                wellSize -= (THUMB_RATIO * theight - twidth);
                twidth = THUMB_RATIO * theight;
            }
            tx = value * wellSize / range;
        } else {
            int wellSize = theight;
            theight = extent * wellSize / range;
            if (theight < THUMB_RATIO * twidth) {
                wellSize -= (THUMB_RATIO * twidth - theight);
                theight = THUMB_RATIO * twidth;
            }
            ty = (range - extent - value) * wellSize / range;
        }
        _thumb.setBounds(_well.getX() + winsets.left + tx,
                         _well.getY() + winsets.bottom + ty, twidth, theight);
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/ScrollBar" + (_horz ? "H" : "V");
    }

    // documentation inherited
    protected void layout ()
    {
        super.layout();

        // reposition our thumb
        update();
    }

    protected ChangeListener _updater = new ChangeListener() {
        public void stateChanged (ChangeEvent event) {
            update();
        }
    };

    protected MouseListener _wellListener = new MouseAdapter() {
        @Override
        public void mousePressed (MouseEvent event) {
            // if we're above the thumb, scroll up by a page, if we're
            // below, scroll down a page
            int mx = event.getX() - getAbsoluteX(),
                my = event.getY() - getAbsoluteY(), dv = 0;
            if (_horz) {
                if (mx < _thumb.getX()) {
                    dv = -1;
                } else if (mx > _thumb.getX() + _thumb.getWidth()) {
                    dv = 1;
                }
            } else {
                if (my < _thumb.getY()) {
                    dv = 1;
                } else if (my > _thumb.getY() + _thumb.getHeight()) {
                    dv = -1;
                }
            }
            if (dv != 0) {
                dv *= Math.max(1, _model.getExtent());
                _model.setValue(_model.getValue() + dv);
            }
            event.consume();
        }

        @Override
        public void mouseDragged (MouseEvent event) {
            event.consume();
        }
    };

    protected MouseAdapter _thumbListener = new MouseAdapter() {
        @Override
        public void mousePressed (MouseEvent event) {
            _sv = _model.getValue();
            _sx = event.getX() - getAbsoluteX();
            _sy = event.getY() - getAbsoluteY();
            event.consume();
        }

        @Override
        public void mouseDragged (MouseEvent event) {
            int dv = 0;
            if (_horz) {
                int mx = event.getX() - getAbsoluteX();
                dv = (mx - _sx) * _model.getRange() /
                    (_well.getWidth() - _well.getInsets().getHorizontal());
            } else {
                int my = event.getY() - getAbsoluteY();
                dv = (_sy - my) * _model.getRange() /
                    (_well.getHeight() - _well.getInsets().getVertical());
            }

            if (dv != 0) {
                _model.setValue(_sv + dv);
            }
            event.consume();
        }

        protected int _sx, _sy, _sv;
    };

    protected ActionListener _buttoner = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            int delta = ((_less == event.getSource()) ? -1 : 1) * _model.getScrollIncrement();
            _model.setValue(_model.getValue() + delta);
        }
    };

    protected BoundedRangeModel _model;
    protected boolean _horz;

    protected Button _less, _more;
    protected Component _well, _thumb;

    protected MouseWheelListener _wheelListener;

    protected static final int THUMB_RATIO = 2;
}
