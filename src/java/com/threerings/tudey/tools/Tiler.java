//
// $Id$

package com.threerings.tudey.tools;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import com.samskivert.util.Predicate;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;

import com.threerings.opengl.gui.util.Point;

import com.threerings.tudey.client.cursor.TileCursor;
import com.threerings.tudey.client.sprite.TileSprite;
import com.threerings.tudey.client.sprite.Sprite;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;

/**
 * Places individual tiles.
 */
public class Tiler extends ConfigTool<TileConfig>
{
    /**
     * Creates the placer tool.
     */
    public Tiler (SceneEditor editor)
    {
        super(editor, TileConfig.class, new TileReference());
    }

    @Override // documentation inherited
    public void init ()
    {
        _cursor = new TileCursor(_editor, _editor.getView(), _entry);
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        if (_editor.isThirdButtonDown()) {
            _editor.deleteMouseObject(SceneEditor.TILE_SPRITE_FILTER);
        }
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
            placeEntry();
        }
    }

    @Override // documentation inherited
    public void mouseWheelMoved (MouseWheelEvent event)
    {
        if (_cursorVisible) {
            _entry.rotation = (_entry.rotation + event.getWheelRotation()) & 0x03;
        }
    }

    /**
     * Updates the entry transform and cursor visibility based on the location of the mouse cursor.
     */
    protected void updateCursor ()
    {
        if (!(_cursorVisible = _entry.tile != null &&
                getMousePlaneIntersection(_isect) && !_editor.isControlDown())) {
            return;
        }
        _entry.getLocation().set((int)FloatMath.floor(_isect.x), (int)FloatMath.floor(_isect.y));
        _entry.elevation = _editor.getGrid().getElevation();
        _cursor.update(_entry);

        // if we are dragging, consider performing another placement
        if (_editor.isFirstButtonDown() && !_entry.getLocation().equals(_lastPlacement)) {
            placeEntry();
        }
    }

    /**
     * Places the current entry.
     */
    protected void placeEntry ()
    {
        _scene.addEntry((TileEntry)_entry.clone());
        _lastPlacement.set(_entry.getLocation());
    }

    @Override // documentation inherited
    protected void referenceChanged (ConfigReference<TileConfig> ref)
    {
        _entry.tile = ref;
    }

    /**
     * Allows us to edit the tile reference.
     */
    protected static class TileReference extends EditableReference<TileConfig>
    {
        /** The tile reference. */
        @Editable(nullable=true)
        public ConfigReference<TileConfig> tile;

        @Override // documentation inherited
        public ConfigReference<TileConfig> getReference ()
        {
            return tile;
        }

        @Override // documentation inherited
        public void setReference (ConfigReference<TileConfig> ref)
        {
            tile = ref;
        }
    }

    /** The prototype tile. */
    protected TileEntry _entry = new TileEntry();

    /** The cursor. */
    protected TileCursor _cursor;

    /** Whether or not the cursor is in the window. */
    protected boolean _cursorVisible;

    /** The location at which we last placed. */
    protected Point _lastPlacement = new Point();

    /** Holds the result on an intersection test. */
    protected Vector3f _isect = new Vector3f();
}
