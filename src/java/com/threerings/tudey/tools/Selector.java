//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.Polygon;

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
    }

    @Override // documentation inherited
    public void sceneChanged (TudeySceneModel scene)
    {
        super.sceneChanged(scene);
        _editor.setSelection(null);
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        updateSelection();
    }

    @Override // documentation inherited
    public void mousePressed (MouseEvent event)
    {
        if (event.getButton() == MouseEvent.BUTTON1 && !_editor.isControlDown()) {
            _editor.setSelection(null);
        }
    }

    @Override // documentation inherited
    public void mouseDragged (MouseEvent event)
    {
        if (!_dragging && _editor.isFirstButtonDown() && !_editor.isControlDown() &&
                getMousePlaneIntersection(_isect)) {
            // store the anchor point
            _anchor.set(_isect);
            _dragging = true;
        }
    }

    /**
     * Updates the selection based on the position of the mouse cursor.
     */
    protected void updateSelection ()
    {
        if (!(_dragging && _editor.isFirstButtonDown() && getMousePlaneIntersection(_isect) &&
                !_editor.isControlDown())) {
            _dragging = false;
            return;
        }
        // snap to tile grid if shift not held down
        boolean gx = (_isect.x >= _anchor.x), gy = (_isect.y >= _anchor.y);
        Vector2f corner = _selection.getVertex(0);
        if (!_editor.isShiftDown()) {
            corner.x = gx ? FloatMath.floor(_anchor.x) + 0.01f : FloatMath.ceil(_anchor.x) - 0.01f;
            corner.y = gy ? FloatMath.floor(_anchor.y) + 0.01f : FloatMath.ceil(_anchor.y) - 0.01f;
            _isect.x = gx ? FloatMath.ceil(_isect.x) - 0.01f : FloatMath.floor(_isect.x) + 0.01f;
            _isect.y = gy ? FloatMath.ceil(_isect.y) - 0.01f : FloatMath.floor(_isect.y) + 0.01f;
        } else {
            corner.set(_anchor.x, _anchor.y);
        }
        // adjust the ordering to ensure ccw orientation
        _selection.getVertex(2).set(_isect.x, _isect.y);
        if (gx ^ gy) {
            _selection.getVertex(1).set(corner.x, _isect.y);
            _selection.getVertex(3).set(_isect.x, corner.y);
        } else {
            _selection.getVertex(1).set(_isect.x, corner.y);
            _selection.getVertex(3).set(corner.x, _isect.y);
        }
        _selection.updateBounds();
        _editor.setSelection(_selection);
    }

    /** The selection shape. */
    protected Polygon _selection = new Polygon(4);

    /** Whether we are currently dragging out. */
    protected boolean _dragging;

    /** The anchor point, when dragging. */
    protected Vector3f _anchor = new Vector3f();

    /** Holds the result on an intersection test. */
    protected Vector3f _isect = new Vector3f();
}
