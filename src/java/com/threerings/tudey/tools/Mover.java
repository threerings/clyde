//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import com.threerings.math.FloatMath;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.tudey.client.cursor.SelectionCursor;

/**
 * The mover tool.
 */
public class Mover extends EditorTool
{
    /**
     * Creates the mover tool.
     */
    public Mover (SceneEditor editor)
    {
        super(editor);
    }

    @Override // documentation inherited
    public void init ()
    {
        _cursor = new SelectionCursor(_editor, _editor.getView());
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        updateCursor();
        if (_cursorVisible) {
            _cursor.tick(elapsed);
        }
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        if (_cursorVisible) {
            _cursor.enqueue();
        }
    }

    @Override // documentation inherited
    public void mousePressed (MouseEvent event)
    {
        if (event.getButton() == MouseEvent.BUTTON1 && _cursorVisible) {

        }
    }

    @Override // documentation inherited
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
        if (!(_cursorVisible = getMousePlaneIntersection(_isect) && !_editor.isControlDown())) {
            return;
        }
        // snap to tile grid if shift not held down
        if (!_editor.isShiftDown()) {
            _isect.x = FloatMath.floor(_isect.x) + 0.5f;
            _isect.y = FloatMath.floor(_isect.y) + 0.5f;
        }
        _transform.getTranslation().set(_isect.x, _isect.y, _editor.getGrid().getZ());
        _transform.getRotation().fromAngleAxis(_angle, Vector3f.UNIT_Z);
    }

    /** The cursor representing the selection that we're moving. */
    protected SelectionCursor _cursor;

    /** Whether or not the cursor is in the window. */
    protected boolean _cursorVisible;

    /** The angle about the z axis. */
    protected float _angle;

    /** The selection transform. */
    protected Transform3D _transform = new Transform3D(Transform3D.RIGID);

    /** Holds the result of an intersection test. */
    protected Vector3f _isect = new Vector3f();
}
