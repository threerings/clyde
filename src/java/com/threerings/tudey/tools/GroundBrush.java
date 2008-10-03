//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseWheelEvent;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;

import com.threerings.tudey.client.util.GridBox;
import com.threerings.tudey.config.GroundConfig;
import com.threerings.tudey.util.TudeySceneMetrics;

/**
 * The ground brush tool.
 */
public class GroundBrush extends ConfigTool<GroundConfig>
{
    /**
     * Creates the ground brush tool.
     */
    public GroundBrush (SceneEditor editor)
    {
        super(editor, GroundConfig.class, new GroundReference());
    }

    @Override // documentation inherited
    public void init ()
    {
        _inner = new GridBox(_editor);
        _inner.getColor().set(0f, 0.75f, 0f, 1f);
        _outer = new GridBox(_editor);
        _outer.getColor().set(0f, 1f, 0f, 1f);
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
            _inner.enqueue();
            _outer.enqueue();
        }
    }

    @Override // documentation inherited
    public void mouseWheelMoved (MouseWheelEvent event)
    {
        if (_cursorVisible) {
            _rotation = (_rotation + event.getWheelRotation()) & 0x03;
        }
    }

    /**
     * Updates the entry transform and cursor visibility based on the location of the mouse cursor.
     */
    protected void updateCursor ()
    {
        GroundReference gref = (GroundReference)_eref;
        if (!(_cursorVisible = gref.ground != null && getMousePlaneIntersection(_isect) &&
                !_editor.isControlDown())) {
            return;
        }
        int width = TudeySceneMetrics.getTileWidth(gref.width, gref.height, _rotation);
        int height = TudeySceneMetrics.getTileHeight(gref.width, gref.height, _rotation);
        int iwidth = width - 1, iheight = height - 1;
        int owidth = width + 1, oheight = height + 1;

        int x = Math.round(_isect.x - iwidth*0.5f), y = Math.round(_isect.y - iheight*0.5f);
        _inner.getRegion().set(x, y, iwidth, iheight);
        _outer.getRegion().set(x - 1, y - 1, owidth, oheight);

        int elevation = _editor.getGrid().getElevation();
        _inner.setElevation(elevation);
        _outer.setElevation(elevation);
    }

    /**
     * Allows us to edit the ground reference.
     */
    protected static class GroundReference extends EditableReference<GroundConfig>
    {
        /** The ground reference. */
        @Editable(nullable=true)
        public ConfigReference<GroundConfig> ground;

        /** The width of the brush. */
        @Editable(min=1, hgroup="d")
        public int width = 1;

        /** The height of the brush. */
        @Editable(min=1, hgroup="d")
        public int height = 1;

        @Override // documentation inherited
        public ConfigReference<GroundConfig> getReference ()
        {
            return ground;
        }

        @Override // documentation inherited
        public void setReference (ConfigReference<GroundConfig> ref)
        {
            ground = ref;
        }
    }

    /** The inner and outer cursors. */
    protected GridBox _inner, _outer;

    /** Whether or not the cursor is in the window. */
    protected boolean _cursorVisible;

    /** The rotation of the cursor. */
    protected int _rotation;

    /** Holds the result on an intersection test. */
    protected Vector3f _isect = new Vector3f();
}
