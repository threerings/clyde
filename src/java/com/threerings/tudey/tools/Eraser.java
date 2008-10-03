//
// $Id$

package com.threerings.tudey.tools;

import com.threerings.editor.Editable;
import com.threerings.editor.swing.EditorPanel;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.tudey.client.util.GridBox;

/**
 * The eraser tool.
 */
public class Eraser extends EditorTool
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
    }

    @Override // documentation inherited
    public void init ()
    {
        _cursor = new GridBox(_editor);
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

    /**
     * Updates the entry transform and cursor visibility based on the location of the mouse cursor.
     */
    protected void updateCursor ()
    {
        if (!(_cursorVisible = getMousePlaneIntersection(_isect) && !_editor.isControlDown())) {
            return;
        }
        _cursor.getRegion().set(
            (int)FloatMath.floor(_isect.x) - _options.width/2,
            (int)FloatMath.floor(_isect.y) - _options.height/2,
            _options.width, _options.height);
        _cursor.setElevation(_editor.getGrid().getElevation());
    }

    /**
     * Allows us to edit the tool options.
     */
    protected static class Options extends DeepObject
        implements Exportable
    {
        /** The width of the eraser. */
        @Editable(min=1, hgroup="d")
        public int width = 1;

        /** The height of the eraser. */
        @Editable(min=1, hgroup="d")
        public int height = 1;
    }

    /** The eraser options. */
    protected Options _options = new Options();

    /** The cursor. */
    protected GridBox _cursor;

    /** Whether or not the cursor is in the window. */
    protected boolean _cursorVisible;

    /** Holds the result on an intersection test. */
    protected Vector3f _isect = new Vector3f();
}
