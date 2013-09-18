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

import java.util.ArrayList;

import com.google.common.base.Predicates;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.tudey.client.util.ShapeSceneElement;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.shape.Polygon;
import com.threerings.tudey.util.TudeySceneMetrics;

/**
 * The selector tool.
 */
public class Selector extends EditorTool
{
    /**
     * Creates the selector tool.
     */
    public Selector (SceneEditor editor)
    {
        super(editor);

        // create and add the editor panel
        EditorPanel epanel = new EditorPanel(editor);
        add(epanel);
        epanel.setObject(_options);
    }

    @Override
    public void init ()
    {
        _cursor = new ShapeSceneElement(_editor, true);
        _cursor.setShape(new Polygon(4));
    }

    @Override
    public void tick (float elapsed)
    {
        updateSelection();
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
        if (event.getButton() != MouseEvent.BUTTON1 || _editor.isSpecialDown()) {
            return;
        }
        Entry entry = _editor.getMouseEntry();
        if (entry != null && _editor.isSelected(entry)) {
            _editor.moveSelection();
        } else if (getMousePlaneIntersection(_isect)) {
            _editor.clearSelection();
            _anchor.set(_isect);
            _dragging = true;
        }
    }

    @Override
    public void mouseReleased (MouseEvent event)
    {
        _editor.setSelectionAsRecent();
    }

    /**
     * Updates the selection based on the position of the mouse cursor.
     */
    protected void updateSelection ()
    {
        if (!(_cursorVisible = _dragging && _editor.isFirstButtonDown() &&
                getMousePlaneIntersection(_isect) && !_editor.isSpecialDown())) {
            _dragging = false;
            return;
        }
        // hold off displaying until the cursor is actually moved
        if (_anchor.equals(_isect)) {
            _cursorVisible = false;
        }
        // snap to tile grid if shift not held down
        boolean gx = (_isect.x >= _anchor.x), gy = (_isect.y >= _anchor.y);
        Polygon shape = (Polygon)_cursor.getShape();
        Vector2f corner = shape.getVertex(0);
        if (!_editor.isShiftDown()) {
            corner.x = gx ? FloatMath.floor(_anchor.x) + 0.01f : FloatMath.ceil(_anchor.x) - 0.01f;
            corner.y = gy ? FloatMath.floor(_anchor.y) + 0.01f : FloatMath.ceil(_anchor.y) - 0.01f;
            _isect.x = gx ? FloatMath.ceil(_isect.x) - 0.01f : FloatMath.floor(_isect.x) + 0.01f;
            _isect.y = gy ? FloatMath.ceil(_isect.y) - 0.01f : FloatMath.floor(_isect.y) + 0.01f;
        } else {
            corner.set(_anchor.x, _anchor.y);
        }
        // adjust the ordering to ensure ccw orientation
        shape.getVertex(2).set(_isect.x, _isect.y);
        if (gx ^ gy) {
            shape.getVertex(1).set(corner.x, _isect.y);
            shape.getVertex(3).set(_isect.x, corner.y);
        } else {
            shape.getVertex(1).set(_isect.x, corner.y);
            shape.getVertex(3).set(corner.x, _isect.y);
        }
        shape.updateBounds();

        // update the elevation
        _cursor.getTransform().getTranslation().z =
            TudeySceneMetrics.getTileZ(_editor.getGrid().getElevation());

        // update the selection
        _scene.getEntries(
            shape, Predicates.and(_options.filter, _editor.getLayerPredicate()), _entries);
        if (!keysEqual(_entries, _editor.getSelection())) {
            _editor.setSelection(_entries.toArray(new Entry[_entries.size()]));
        }
        _entries.clear();
    }

    /**
     * Determines whether the given list and array contain entries with the same keys in the same
     * order.
     */
    protected static boolean keysEqual (ArrayList<Entry> list, Entry[] array)
    {
        if (list.size() != array.length) {
            return false;
        }
        for (int ii = 0; ii < array.length; ii++) {
            if (!list.get(ii).getKey().equals(array[ii].getKey())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Allows us to edit the tool options.
     */
    protected static class Options extends DeepObject
        implements Exportable
    {
        /** The filter that determines which entries to select. */
        @Editable
        public Filter filter = new Filter();
    }

    /** The selector options. */
    protected Options _options = new Options();

    /** The selection cursor. */
    protected ShapeSceneElement _cursor;

    /** Whether or not the cursor is in the window. */
    protected boolean _cursorVisible;

    /** Whether we are currently dragging out. */
    protected boolean _dragging;

    /** The anchor point, when dragging. */
    protected Vector3f _anchor = new Vector3f();

    /** Holds the result on an intersection test. */
    protected Vector3f _isect = new Vector3f();

    /** Holds the results of a shape query. */
    protected ArrayList<Entry> _entries = new ArrayList<Entry>();
}
