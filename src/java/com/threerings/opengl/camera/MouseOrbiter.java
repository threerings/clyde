//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.opengl.camera;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import com.threerings.math.FloatMath;

/**
 * Provides a means of manipulating an {@link OrbitCameraHandler} using the mouse.
 */
public class MouseOrbiter
    implements MouseMotionListener, MouseWheelListener
{
    /**
     * Creates new mouse orbiter for the specified camera handler.
     */
    public MouseOrbiter (OrbitCameraHandler camhand)
    {
        this(camhand, false);
    }

    /**
     * Creates a new mouse orbiter for the specified camera handler.
     *
     * @param panXY if true, limit panning to the XY plane.
     */
    public MouseOrbiter (OrbitCameraHandler camhand, boolean panXY)
    {
        _camhand = camhand;
        _panXY = panXY;
    }

    /**
     * Adds the orbiter to the specified component.
     */
    public void addTo (Component comp)
    {
        comp.addMouseMotionListener(this);
        comp.addMouseWheelListener(this);
    }

    /**
     * Removes the orbiter from the specified component.
     */
    public void removeFrom (Component comp)
    {
        comp.removeMouseMotionListener(this);
        comp.removeMouseWheelListener(this);
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseDragged (MouseEvent event)
    {
        int dx = event.getX() - _lx, dy = event.getY() - _ly;
        int modex = event.getModifiersEx();
        if ((modex & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            _camhand.orbit(-dx * _radiansPerPixel, dy * _radiansPerPixel);
        }
        if ((modex & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
            _camhand.zoom(dy * _unitsPerPixel);
        }
        if ((modex & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
            if (_panXY) {
                _camhand.panXY(-dx * _unitsPerPixel, dy * _unitsPerPixel);
            } else {
                _camhand.pan(-dx * _unitsPerPixel, dy * _unitsPerPixel);
            }
        }
        mouseMoved(event);
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseMoved (MouseEvent event)
    {
        _lx = event.getX();
        _ly = event.getY();
    }

    // documentation inherited from interface MouseWheelListener
    public void mouseWheelMoved (MouseWheelEvent event)
    {
        _camhand.zoom(event.getWheelRotation() * _unitsPerClick);
    }

    /** The camera handler we're controlling. */
    protected OrbitCameraHandler _camhand;

    /** The angular scale. */
    protected float _radiansPerPixel = FloatMath.PI / 1000f;

    /** The linear scale. */
    protected float _unitsPerPixel = 0.02f;

    /** The wheel scale. */
    protected float _unitsPerClick = 0.3f;

    /** Whether or not to restrict panning to the XY plane. */
    protected boolean _panXY;

    /** The last x and y position recorded. */
    protected int _lx, _ly;
}
