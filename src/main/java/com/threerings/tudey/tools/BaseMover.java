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

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import java.util.ArrayList;

import com.threerings.math.FloatMath;
import com.threerings.math.Rect;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;

import com.threerings.tudey.client.cursor.SelectionCursor;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;
import com.threerings.tudey.util.TudeySceneMetrics;

/**
 * The base class of {@link Mover} and {@link Palette}.
 */
public abstract class BaseMover extends EditorTool
{
    /**
     * Creates the base mover tool.
     */
    public BaseMover (SceneEditor editor)
    {
        super(editor);
    }

    /**
     * Clears out anything being moved.
     */
    public void clear ()
    {
        move();
    }

    /**
     * Requests to start moving the specified entries.
     */
    public void move (Entry... entries)
    {
        // make sure some entries exist
        _entries = new Entry[entries.length];
        if (entries.length == 0) {
            return;
        }

        // clone the entries, find the bounds, and see if any are tiles
        Rect bounds = new Rect(), ebounds = new Rect();
        int minElevation = Integer.MAX_VALUE, maxElevation = Integer.MIN_VALUE;
        _tiles = false;
        for (int ii = 0; ii < entries.length; ii++) {
            Entry entry = _entries[ii] = (Entry)entries[ii].clone();
            _tiles |= (entry instanceof TileEntry);
            entry.getBounds(_editor.getConfigManager(), ebounds);
            bounds.addLocal(ebounds);
            int elevation = entry.getElevation();
            if (elevation != Integer.MIN_VALUE) {
                minElevation = Math.min(minElevation, elevation);
                maxElevation = Math.max(maxElevation, elevation);
            }
        }
        // find the center and elevation
        bounds.getCenter(_center);
        calculateElevation(minElevation, maxElevation);

        // reset the angle
        _angle = 0f;
    }

    @Override
    public void init ()
    {
        _cursor = new SelectionCursor(_editor, _editor.getView());
    }

    @Override
    public void tick (float elapsed)
    {
        updateCursor();
        if (_cursorVisible) {
            _cursor.tick(elapsed);
        }
    }

    @Override
    public void composite ()
    {
        if (_cursorVisible) {
            _cursor.composite();
        }
    }

    @Override
    public void mousePressed (MouseEvent event)
    {
        if (_cursorVisible && event.getButton() == MouseEvent.BUTTON1 &&
                !_editor.isSpecialDown()) {
            placeEntries();
        }
    }

    @Override
    public void mouseWheelMoved (MouseWheelEvent event)
    {
        // adjust in terms of coarse (ninety degree) or fine increments
        if (_cursorVisible) {
            float increment = (_tiles || !event.isShiftDown()) ?
                FloatMath.HALF_PI : FINE_ROTATION_INCREMENT;
            _angle = (Math.round(_angle / increment) + event.getWheelRotation()) * increment;
        }
    }

    /**
     * Updates the entry transform and cursor visibility based on the location of the mouse cursor.
     */
    protected void updateCursor ()
    {
        if (!(_cursorVisible = (_entries.length > 0) && getMousePlaneIntersection(_isect) &&
                !_editor.isSpecialDown())) {
            return;
        }
        Vector2f rcenter = _center.rotate(_angle);
        _isect.x -= rcenter.x;
        _isect.y -= rcenter.y;
        if (_tiles || !_editor.isShiftDown()) {
            _isect.x = Math.round(_isect.x);
            _isect.y = Math.round(_isect.y);
        }
        _transform.getTranslation().set(_isect.x, _isect.y,
            TudeySceneMetrics.getTileZ(_editor.getGrid().getElevation() - _elevation));
        _transform.getRotation().fromAngleAxis(_angle, Vector3f.UNIT_Z);

        // transform the entries and update the cursor
        _cursor.update(_tentries = transform(_entries, _transform));

        // erase if the third button is down
        if (_editor.isThirdButtonDown()) {
            _scene.getEntries(_cursor.getShape(), _editor.getLayerPredicate(), _underneath);
            _editor.removeEntries(_underneath);
            _underneath.clear();
        }
    }

    /**
     * Places the transformed entries in the scene.
     *
     * @return an array containing the entries as placed.
     */
    protected Entry[] placeEntries ()
    {
        Entry[] placed = new Entry[_tentries.length];
        for (int ii = 0, nn = _tentries.length; ii < nn; ii++) {
            placed[ii] = (Entry)_tentries[ii].clone();
        }
        _editor.overwriteEntries(placed);
        return placed;
    }

    /**
     * Transforms the supplied entries, returning a new entry array containing the results.
     */
    protected Entry[] transform (Entry[] entries, Transform3D transform)
    {
        Entry[] tentries = new Entry[entries.length];
        for (int ii = 0, nn = entries.length; ii < nn; ii++) {
            Entry tentry = tentries[ii] = (Entry)entries[ii].clone();
            tentry.transform(_editor.getConfigManager(), transform);
        }
        return tentries;
    }

    /**
     * Calculates the elevation.
     */
    public void calculateElevation (int minElevation, int maxElevation)
    {
        _elevation = (minElevation < maxElevation) ? (minElevation + maxElevation)/2 : 0;
        _elevation += _editor.getGrid().getElevation();
    }

    /** The cursor representing the selection that we're moving. */
    protected SelectionCursor _cursor;

    /** The (untransformed) entries that we're moving. */
    protected Entry[] _entries = new Entry[0];

    /** The transformed entries. */
    protected Entry[] _tentries;

    /** Whether or not any of the entries are tiles (in which case we must stay aligned). */
    protected boolean _tiles;

    /** The center of the entries. */
    protected Vector2f _center = new Vector2f();

    /** The entries' elevation. */
    protected int _elevation;

    /** The selection transform. */
    protected Transform3D _transform = new Transform3D(Transform3D.RIGID);

    /** Whether or not the cursor is in the window. */
    protected boolean _cursorVisible;

    /** The angle about the z axis. */
    protected float _angle;

    /** Holds the result of an intersection test. */
    protected Vector3f _isect = new Vector3f();

    /** Holds the entries intersecting the cursor. */
    protected ArrayList<Entry> _underneath = new ArrayList<Entry>();
}
