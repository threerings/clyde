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

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.layout.BorderLayout;

/**
 * Displays a scroll button for all your horizontal and vertical scrolling
 * needs.
 */
public class ScrollButton extends Container
    implements UIConstants
{
    /**
     * Creates a vertical scroll button with the default range, value and
     * extent.
     */
    public ScrollButton (GlContext ctx, boolean less)
    {
        this(ctx, less, VERTICAL);
    }

    /**
     * Creates a scroll button with the default range, value and extent.
     */
    public ScrollButton (GlContext ctx, boolean less, int orientation)
    {
        this(ctx, less, orientation, 0, 100, 0, 10);
    }

    /**
     * Creates a scroll button with the specified orientation, range, value
     * and extent.
     */
    public ScrollButton (
            GlContext ctx, boolean less, int orientation, int min, int value, int extent, int max)
    {
        this(ctx, less, orientation, new BoundedRangeModel(min, value, extent, max));
    }

    /**
     * Creates a scroll button with the specified orientation which will
     * interact with the supplied model.
     */
    public ScrollButton (GlContext ctx, boolean less, int orientation, BoundedRangeModel model)
    {
        super(ctx, new BorderLayout());
        _orient = orientation;
        _model = model;
        _less = less;
    }

    /**
     * Returns a reference to the scrollbar's range model.
     */
    public BoundedRangeModel getModel ()
    {
        return _model;
    }

    @Override
    protected void wasAdded ()
    {
        super.wasAdded();

        // create our buttons and backgrounds
        String oprefix = "Default/ScrollBar" + ((_orient == ScrollBar.HORIZONTAL) ? "H" : "V");
        _btn = new Button(_ctx, "");
        _btn.setStyleConfig(oprefix + (_less ? "Less" : "More"));
        add(_btn, BorderLayout.CENTER);
        _btn.addListener(_buttoner);
        _btn.setAction(_less ? "less" : "more");
    }

    @Override
    protected void wasRemoved ()
    {
        super.wasRemoved();

        if (_btn != null) {
            remove(_btn);
            _btn = null;
        }
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/ScrollBar";
    }

    protected ActionListener _buttoner = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            int delta = _model.getScrollIncrement();
            if (event.getAction().equals("less")) {
                _model.setValue(_model.getValue() - delta);
            } else {
                _model.setValue(_model.getValue() + delta);
            }
        }
    };

    protected BoundedRangeModel _model;
    protected int _orient;
    protected boolean _less;

    protected Button _btn;
}
