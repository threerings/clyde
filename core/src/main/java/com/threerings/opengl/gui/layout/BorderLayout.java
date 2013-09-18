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

package com.threerings.opengl.gui.layout;

import com.samskivert.util.ListUtil;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.Container;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;

/**
 * Lays out the children of a container around the borders and one in the
 * center. For example:
 * <pre>
 * +------------------------------------+
 * |               NORTH                |
 * +-----+------------------------+-----+
 * |     |                        |     |
 * |  W  |         CENTER         |  E  |
 * |     |                        |     |
 * +-----+------------------------+-----+
 * |               SOUTH                |
 * +------------------------------------+
 * </pre>
 */
public class BorderLayout extends LayoutManager
{
    /** A layout constraint. */
    public static final Integer NORTH = new Integer(0);

    /** A layout constraint. */
    public static final Integer SOUTH = new Integer(1);

    /** A layout constraint. */
    public static final Integer EAST = new Integer(2);

    /** A layout constraint. */
    public static final Integer WEST = new Integer(3);

    /** A layout constraint. */
    public static final Integer CENTER = new Integer(4);

    /** A layout constraint. */
    public static final Integer IGNORE = new Integer(5);

    /**
     * Creates a border layout with zero gap between the horizontal
     * components and zero gap between the vertical.
     */
    public BorderLayout ()
    {
        this(0, 0);
    }

    /**
     * Creates a border layout with the specified gap between the
     * horizontal components and the specified gap between the vertical.
     */
    public BorderLayout (int hgap, int vgap)
    {
        setGaps(hgap, vgap);
    }

    /**
     * Configures our inter-component gaps.
     */
    public void setGaps (int hgap, int vgap)
    {
        _hgap = hgap;
        _vgap = vgap;
    }

    // documentation inherited
    public void addLayoutComponent (Component comp, Object constraints)
    {
        if (constraints instanceof Integer) {
            if (constraints != IGNORE) {
                _components[((Integer)constraints).intValue()] = comp;
            }
        } else {
            throw new IllegalArgumentException(
                "Components added to a BorderLayout must have proper " +
                "constraints (eg. BorderLayout.NORTH).");
        }
    }

    // documentation inherited
    public void removeLayoutComponent (Component comp)
    {
        for (int ii = 0; ii < _components.length; ii++) {
            if (_components[ii] == comp) {
                _components[ii] = null;
                break;
            }
        }
    }

    @Override
    public Object getConstraints (Component comp)
    {
        int idx = ListUtil.indexOfRef(_components, comp);
        return (idx == -1) ? null : idx;
    }

    // documentation inherited
    public Dimension computePreferredSize (Container target, int whint, int hhint)
    {
        Dimension psize = new Dimension();
        int horizComps = 0, vertComps = 0;
        Component comp;
        for (Integer vert : VERTS) {
            comp = _components[vert.intValue()];
            if (comp != null && comp.isVisible()) {
                Dimension cpsize = comp.getPreferredSize(whint, -1);
                psize.width = Math.max(psize.width, cpsize.width);
                psize.height += cpsize.height;
                vertComps++;
                if (hhint > 0) {
                    hhint -= (cpsize.height + _vgap);
                }
            }
        }

        int centerWidth = 0, centerHeight = 0;
        for (Integer horiz : HORIZS) {
            comp = _components[horiz.intValue()];
            if (comp != null && comp.isVisible()) {
                Dimension cpsize = comp.getPreferredSize(-1, hhint);
                centerWidth += cpsize.width;
                centerHeight = Math.max(centerHeight, cpsize.height);
                horizComps++;
                if (whint > 0) {
                    whint -= (cpsize.width + _hgap);
                }
            }
        }

        comp = _components[CENTER.intValue()];
        if (comp != null && comp.isVisible()) {
            Dimension cpsize = comp.getPreferredSize(whint, hhint);
            centerWidth += cpsize.width;
            centerHeight = Math.max(centerHeight, cpsize.height);
            horizComps++;
        }
        centerWidth += Math.max(horizComps - 1, 0) * _hgap;
        if (centerHeight > 0) {
            vertComps++;
        }

        psize.width = Math.max(psize.width, centerWidth);
        psize.height += centerHeight;
        psize.height += Math.max(vertComps - 1, 0) * _vgap;

        return psize;
    }

    // documentation inherited
    public void layoutContainer (Container target)
    {
        // determine what we've got to work with
        Insets insets = target.getInsets();
        int x = insets.left, y = insets.bottom;
        int width = target.getWidth() - insets.getHorizontal();
        int height = target.getHeight() - insets.getVertical();

        Component comp = _components[SOUTH.intValue()];
        if (comp != null && comp.isVisible()) {
            Dimension cpsize = comp.getPreferredSize(width, -1);
            comp.setBounds(x, y, width, cpsize.height);
            y += (cpsize.height + _vgap);
            height -= (cpsize.height + _vgap);
        }

        comp = _components[NORTH.intValue()];
        if (comp != null && comp.isVisible()) {
            Dimension cpsize = comp.getPreferredSize(width, -1);
            comp.setBounds(x, target.getHeight() - insets.top - cpsize.height,
                           width, cpsize.height);
            height -= (cpsize.height + _vgap);
        }

        comp = _components[WEST.intValue()];
        if (comp != null && comp.isVisible()) {
            Dimension cpsize = comp.getPreferredSize(-1, -1);
            comp.setBounds(x, y, cpsize.width, height);
            x += (cpsize.width + _hgap);
            width -= (cpsize.width + _hgap);
        }

        comp = _components[EAST.intValue()];
        if (comp != null && comp.isVisible()) {
            Dimension cpsize = comp.getPreferredSize(-1, -1);
            comp.setBounds(target.getWidth() - insets.right - cpsize.width, y,
                           cpsize.width, height);
            width -= (cpsize.width + _hgap);
        }

        comp = _components[CENTER.intValue()];
        if (comp != null && comp.isVisible()) {
            comp.setBounds(x, y, width, height);
        }
    }

    protected int _hgap, _vgap;
    protected Component[] _components = new Component[5];

    protected static final Integer[] VERTS = { SOUTH, NORTH };
    protected static final Integer[] HORIZS = { EAST, WEST };
}
