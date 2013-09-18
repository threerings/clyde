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

import java.awt.event.MouseWheelEvent;

import java.util.ArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.base.Predicates;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform2D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.tudey.client.util.ShapeConfigElement;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.config.ShapeConfig;

/**
 * The eraser tool.
 */
public class Eraser extends EditorTool
    implements ChangeListener
{
    /**
     * Creates the eraser tool.
     */
    public Eraser (SceneEditor editor)
    {
        super(editor);

        // make the rectangle non-obnoxious
        ShapeConfig.Rectangle rect = (ShapeConfig.Rectangle)_options.shape;
        rect.width = .99f;
        rect.height = .99f;

        // create and add the editor panel
        EditorPanel epanel = new EditorPanel(editor);
        add(epanel);
        epanel.setObject(_options);
        epanel.addChangeListener(this);
    }

    // documentation inherited from interface ChangeListener
    public void stateChanged (ChangeEvent event)
    {
        _options.shape.invalidate();
        _cursor.setConfig(_options.shape, true);
    }

    @Override
    public void init ()
    {
        _cursor = new ShapeConfigElement(_editor);
        _cursor.setConfig(_options.shape, true);
        _cursor.getColor().set(1f, 0.75f, 0.75f, 1f);
    }

    @Override
    public void tick (float elapsed)
    {
        updateCursor();
    }

    @Override
    public void composite ()
    {
        if (_cursorVisible) {
            _cursor.composite();
        }
    }

    @Override
    public void mouseWheelMoved (MouseWheelEvent event)
    {
        // adjust in terms of coarse (ninety degree) or fine increments
        if (_cursorVisible) {
            float increment = event.isShiftDown() ? FINE_ROTATION_INCREMENT : FloatMath.HALF_PI;
            _angle = (Math.round(_angle / increment) + event.getWheelRotation()) * increment;
        }
    }

    /**
     * Updates the entry transform and cursor visibility based on the location of the mouse cursor.
     */
    protected void updateCursor ()
    {
        if (!(_cursorVisible = getMousePlaneIntersection(_isect) && !_editor.isSpecialDown())) {
            return;
        }
        // snap to tile grid if shift not held down
        if (!_editor.isShiftDown()) {
            _isect.x = FloatMath.floor(_isect.x) + 0.5f;
            _isect.y = FloatMath.floor(_isect.y) + 0.5f;
        }
        Transform3D transform = _cursor.getTransform();
        transform.getTranslation().set(_isect.x, _isect.y, _editor.getGrid().getZ());
        transform.getRotation().fromAngleAxis(_angle, Vector3f.UNIT_Z);

        // if the button is down, erase
        if (_editor.isFirstButtonDown()) {
            _transform.getTranslation().set(_isect.x, _isect.y);
            _transform.setRotation(_angle);
            _shape = _options.shape.getShape().transform(_transform, _shape);
            _scene.getEntries(
                _shape, Predicates.and(_options.filter, _editor.getLayerPredicate()), _entries);
            _editor.removeEntries(_entries);
            _entries.clear();
        }
    }

    /**
     * Allows us to edit the tool options.
     */
    protected static class Options extends DeepObject
        implements Exportable
    {
        /** The shape of the eraser. */
        @Editable
        public ShapeConfig shape = new ShapeConfig.Rectangle();

        /** The filter that determines what we want to erase. */
        @Editable
        public Filter filter = new Filter();
    }

    /** The eraser options. */
    protected Options _options = new Options();

    /** The cursor. */
    protected ShapeConfigElement _cursor;

    /** Whether or not the cursor is in the window. */
    protected boolean _cursorVisible;

    /** The angle about the z axis. */
    protected float _angle;

    /** The shape transform. */
    protected Transform2D _transform = new Transform2D(Transform2D.RIGID);

    /** The transformed shape. */
    protected Shape _shape;

    /** Holds the result of an intersection test. */
    protected Vector3f _isect = new Vector3f();

    /** Holds the entries intersecting the cursor. */
    protected ArrayList<Entry> _entries = new ArrayList<Entry>();
}
