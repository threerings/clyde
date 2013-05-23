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

import java.util.Map;

import com.google.common.collect.Maps;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.Container;
import com.threerings.opengl.gui.util.Dimension;

/**
 * Lays out components based on anchors that match a point on the child component to a point on the
 * parent.
 */
public class AnchorLayout extends LayoutManager
{
    /** Anchors the lower-left of the component to the lower-left of the container. */
    public static final Anchor SOUTHWEST = new Anchor(0f, 0f, 0f, 0f, 0, 0, false);

    /** Anchors the lower-center of the component to the lower-center of the container. */
    public static final Anchor SOUTH = new Anchor(0.5f, 0f, 0.5f, 0f, 0, 0, false);

    /** Anchors the lower-right of the component to the lower-right of the container. */
    public static final Anchor SOUTHEAST = new Anchor(1f, 0f, 1f, 0f, 0, 0, false);

    /** Anchors the right-center of the component to the right-center of the container. */
    public static final Anchor EAST = new Anchor(1f, 0.5f, 1f, 0.5f, 0, 0, false);

    /** Anchors the upper-right of the component to the upper-right of the container. */
    public static final Anchor NORTHEAST = new Anchor(1f, 1f, 1f, 1f, 0, 0, false);

    /** Anchors the upper-center of the component to the upper-center of the container. */
    public static final Anchor NORTH = new Anchor(0.5f, 1f, 0.5f, 1f, 0, 0, false);

    /** Anchors the upper-left of the component to the upper-left of the container. */
    public static final Anchor NORTHWEST = new Anchor(0f, 1f, 0f, 1f, 0, 0, false);

    /** Anchors the left-center of the component to the left-center of the container. */
    public static final Anchor WEST = new Anchor(0f, 0.5f, 0f, 0.5f, 0, 0, false);

    /** Anchors the center of the component to the center of the container. */
    public static final Anchor CENTER = new Anchor(0.5f, 0.5f, 0.5f, 0.5f, 0, 0, false);

    /**
     * Represents the location of an anchor binding a point of the child component to a point on
     * the parent.
     */
    public static class Anchor
    {
        /** The proportional location of the anchor on the child component. */
        public float cx, cy;

        /** The proportional location of the anchor on the parent component. */
        public float px, py;

        /** If we use the container dimensions for sizing. */
        public boolean fitToContainer;

        /** The fixed offset from the parent anchor to the child anchor. */
        public int ox, oy;

        /**
         * Creates a new anchor.
         *
         * @param cx the proportional x location of the anchor on the child component.
         * @param cy the proportional y location of the anchor on the child component.
         * @param px the proportional x location of the anchor on the parent component.
         * @param py the proportional y location of the anchor on the parent component.
         */
        public Anchor (
                float cx, float cy, float px, float py, int ox, int oy, boolean fitToContainer)
        {
            this.cx = cx;
            this.cy = cy;
            this.px = px;
            this.py = py;
            this.ox = ox;
            this.oy = oy;
            this.fitToContainer = fitToContainer;
        }
    }

    @Override
    public void addLayoutComponent (Component comp, Object constraints)
    {
        Anchor anchor;
        if (constraints instanceof Anchor) {
            anchor = (Anchor)constraints;
        } else if (constraints == null) {
            anchor = CENTER;
        } else {
            throw new IllegalArgumentException(
                "Components must be added to an AnchorLayout with Anchor constraints.");
        }
        _anchors.put(comp, anchor);
    }

    @Override
    public void removeLayoutComponent (Component comp)
    {
        _anchors.remove(comp);
    }

    @Override
    public Object getConstraints (Component comp)
    {
        return _anchors.get(comp);
    }

    @Override
    public Dimension computePreferredSize (Container target, int whint, int hhint)
    {
        for (int ii = 0, nn = target.getComponentCount(); ii < nn; ii++) {
            Dimension size = target.getComponent(ii).getPreferredSize(whint, hhint);
            whint = Math.max(whint, size.width);
            hhint = Math.max(hhint, size.height);
        }
        return new Dimension(whint, hhint);
    }

    @Override
    public void layoutContainer (Container target)
    {
        int width = target.getWidth(), height = target.getHeight();
        for (int ii = 0, nn = target.getComponentCount(); ii < nn; ii++) {
            Component comp = target.getComponent(ii);
            if (!comp.isVisible()) {
                continue;
            }
            Anchor anchor = _anchors.get(comp);
            if (anchor == null) {
                continue;
            }
            int px = Math.round(width * anchor.px);
            int py = Math.round(height * anchor.py);
            Dimension size = anchor.fitToContainer ?
                comp.getPreferredSize(width, height) : comp.getPreferredSize(-1, -1);
            int cx = Math.round(size.width * anchor.cx);
            int cy = Math.round(size.height * anchor.cy);
            comp.setBounds(px - cx + anchor.ox, py - cy + anchor.oy, size.width, size.height);
        }
    }

    /** The anchors of the components to be layed out. */
    protected Map<Component, Anchor> _anchors = Maps.newIdentityHashMap();
}
