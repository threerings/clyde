//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseWheelEvent;

import java.util.ArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

    @Override // documentation inherited
    public void init ()
    {
        _cursor = new ShapeConfigElement(_editor);
        _cursor.setConfig(_options.shape, true);
        _cursor.getColor().set(1f, 0.75f, 0.75f, 1f);
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        updateCursor();
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        if (_cursorVisible) {
            _cursor.enqueue();
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
        Transform3D transform = _cursor.getTransform();
        transform.getTranslation().set(_isect.x, _isect.y, _editor.getGrid().getZ());
        transform.getRotation().fromAngleAxis(_angle, Vector3f.UNIT_Z);

        // if the button is down, erase
        if (_editor.isFirstButtonDown()) {
            _transform.getTranslation().set(_isect.x, _isect.y);
            _transform.setRotation(_angle);
            _shape = _options.shape.getShape().transform(_transform, _shape);
            _scene.getEntries(_shape, _entries);
            for (int ii = 0, nn = _entries.size(); ii < nn; ii++) {
                Entry entry = _entries.get(ii);
                if (_options.filter.matches(entry)) {
                    _editor.removeEntry(entry.getKey());
                }
            }
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
