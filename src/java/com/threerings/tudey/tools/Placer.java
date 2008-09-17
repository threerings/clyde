//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.tudey.client.cursor.PlaceableCursor;
import com.threerings.tudey.config.PlaceableConfig;
import com.threerings.tudey.data.TudeySceneModel.PlaceableEntry;

/**
 * The placeable placer tool.
 */
public class Placer extends ConfigTool<PlaceableConfig>
{
    /**
     * Creates the placer tool.
     */
    public Placer (SceneEditor editor)
    {
        super(editor, PlaceableConfig.class, new PlaceableReference());
        _entry.transform.setType(Transform3D.UNIFORM);
    }

    @Override // documentation inherited
    public void init ()
    {
        _cursor = new PlaceableCursor(_editor, _editor.getView(), _entry);
    }

    @Override // documentation inherited
    public boolean allowsMouseCamera ()
    {
        return !_cursorVisible;
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
        if (_cursorVisible) {
            _editor.addEntry((PlaceableEntry)_entry.clone());
        }
    }

    @Override // documentation inherited
    public void mouseWheelMoved (MouseWheelEvent event)
    {
        _angle += event.getWheelRotation() *
            (event.isShiftDown() ? FINE_ROTATION_INCREMENT : FloatMath.HALF_PI);
    }

    /**
     * Updates the entry transform and cursor visibility based on the location of the mouse cursor.
     */
    protected void updateCursor ()
    {
        if (!(_cursorVisible = _entry.placeable != null &&
                getMousePlaneIntersection(_isect) && !_editor.isControlDown())) {
            return;
        }
        // snap to tile grid/ninety degree rotations if shift not held down
        if (!_editor.isShiftDown()) {
            _isect.x = FloatMath.floor(_isect.x) + 0.5f;
            _isect.y = FloatMath.floor(_isect.y) + 0.5f;
            _angle = Math.round(_angle / FloatMath.HALF_PI) * FloatMath.HALF_PI;
        }
        Transform3D transform = _entry.transform;
        transform.getTranslation().set(_isect.x, _isect.y, _editor.getGrid().getZ());
        transform.getRotation().fromAngleAxis(_angle, Vector3f.UNIT_Z);
        _cursor.update(_entry);
    }

    @Override // documentation inherited
    protected void referenceChanged (ConfigReference<PlaceableConfig> ref)
    {
        _entry.placeable = ref;
    }

    /**
     * Allows us to edit the placeable reference.
     */
    protected static class PlaceableReference extends EditableReference<PlaceableConfig>
    {
        /** The placeable reference. */
        @Editable(nullable=true)
        public ConfigReference<PlaceableConfig> placeable;

        @Override // documentation inherited
        public ConfigReference<PlaceableConfig> getReference ()
        {
            return placeable;
        }

        @Override // documentation inherited
        public void setReference (ConfigReference<PlaceableConfig> ref)
        {
            placeable = ref;
        }
    }

    /** The prototype entry. */
    protected PlaceableEntry _entry = new PlaceableEntry();

    /** The cursor. */
    protected PlaceableCursor _cursor;

    /** Whether or not the cursor is in the window. */
    protected boolean _cursorVisible;

    /** The angle about the z axis. */
    protected float _angle;

    /** Holds the result on an intersection test. */
    protected Vector3f _isect = new Vector3f();

    /** The fine rotation increment. */
    protected static final float FINE_ROTATION_INCREMENT = FloatMath.PI / 64f;
}
