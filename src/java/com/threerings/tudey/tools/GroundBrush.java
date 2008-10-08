//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.math.Vector3f;

import com.threerings.opengl.gui.util.Rectangle;

import com.threerings.tudey.client.util.GridBox;
import com.threerings.tudey.config.GroundConfig;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;
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
        _inner.getColor().set(0f, 1f, 0f, 1f);
        _outer = new GridBox(_editor);
        _outer.getColor().set(0f, 0.5f, 0f, 1f);
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
    public void mousePressed (MouseEvent event)
    {
        int button = event.getButton();
        boolean paint = (button == MouseEvent.BUTTON1), erase = (button == MouseEvent.BUTTON3);
        if ((paint || erase) && _cursorVisible) {
            paintGround(erase, true);
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
        if (!(_cursorVisible = getMousePlaneIntersection(_isect) && !_editor.isControlDown())) {
            return;
        }
        GroundReference gref = (GroundReference)_eref;
        int vwidth = gref.width - 1, vheight = gref.height - 1;
        int iwidth = TudeySceneMetrics.getTileWidth(vwidth, vheight, _rotation);
        int iheight = TudeySceneMetrics.getTileHeight(vwidth, vheight, _rotation);
        int owidth = iwidth + 2, oheight = iheight + 2;

        int x = Math.round(_isect.x - iwidth*0.5f), y = Math.round(_isect.y - iheight*0.5f);
        _inner.getRegion().set(x, y, iwidth, iheight);
        _outer.getRegion().set(x - 1, y - 1, owidth, oheight);

        int elevation = _editor.getGrid().getElevation();
        _inner.setElevation(elevation);
        _outer.setElevation(elevation);

        // if we are dragging, consider performing another paint operation
        boolean paint = _editor.isFirstButtonDown(), erase = _editor.isThirdButtonDown();
        if ((paint || erase) && !_inner.getRegion().equals(_lastPainted)) {
            paintGround(erase, false);
        }
    }

    /**
     * Paints the cursor region with ground.
     *
     * @param erase if true, erase the region by painting with the null ground type.
     * @param revise if true, replace existing ground tiles with different variants.
     */
    protected void paintGround (boolean erase, boolean revise)
    {
        ConfigReference<GroundConfig> ref = erase ? null : _eref.getReference();
        String ground = (ref == null) ? null : ref.getName();
        Rectangle iregion = _inner.getRegion();
        _lastPainted.set(iregion);

        // get all potentially affected tiles
        Rectangle oregion = _outer.getRegion();
        ArrayList<TileEntry> affected = new ArrayList<TileEntry>();
        _scene.getTileEntries(oregion, affected);

        // step over the painted vertices
        for (int yy = iregion.y, yymax = yy + iregion.height; yy <= yymax; yy++) {
            for (int xx = iregion.x, xxmax = xx + iregion.width; xx <= xxmax; xx++) {

            }
        }
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

    /** The last painted region. */
    protected Rectangle _lastPainted = new Rectangle();

    /** Holds the result on an intersection test. */
    protected Vector3f _isect = new Vector3f();
}
