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
import java.awt.event.MouseWheelEvent;

import java.util.ArrayList;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.math.Vector3f;

import com.threerings.opengl.gui.util.Rectangle;

import com.threerings.tudey.client.cursor.TileCursor;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;
import com.threerings.tudey.util.Coord;

/**
 * Places individual tiles.
 */
public class TileBrush extends ConfigTool<TileConfig>
{
    /**
     * Creates the tile brush tool.
     */
    public TileBrush (SceneEditor editor)
    {
        super(editor, TileConfig.class, new TileReference());
    }

    /**
     * Sets the placement rotation.
     */
    public void setRotation (int rotation)
    {
        _entry.rotation = rotation;
    }

    @Override
    public void init ()
    {
        _cursor = new TileCursor(_editor, _editor.getView(), _entry);
    }

    @Override
    public void tick (float elapsed)
    {
        updateCursor();
        if (_cursorVisible) {
            _cursor.tick(elapsed);
        } else if (_editor.isThirdButtonDown() && !_editor.isSpecialDown()) {
            _editor.deleteMouseEntry(SceneEditor.TILE_ENTRY_FILTER);
        }
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
        int button = event.getButton();
        boolean paint = (button == MouseEvent.BUTTON1), erase = (button == MouseEvent.BUTTON3);
        if ((paint || erase) && _cursorVisible) {
            paintTile(erase);
        }
    }

    @Override
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
        if (!(_cursorVisible = (_entry.tile != null) &&
                getMousePlaneIntersection(_isect) && !_editor.isSpecialDown())) {
            return;
        }
        TileConfig.Original config = _entry.getConfig(_editor.getConfigManager());
        int width = _entry.getWidth(config), height = _entry.getHeight(config);
        Coord location = _entry.getLocation();
        int x = Math.round(_isect.x - width*0.5f);
        int y = Math.round(_isect.y - height*0.5f);
        if (_editor.isShiftDown()) {
            if (_constraint == DirectionalConstraint.HORIZONTAL) {
                y = location.y;
            } else if (_constraint == DirectionalConstraint.VERTICAL) {
                x = location.x;
            } else { // _constraint == null
                if (x != location.x && y == location.y) {
                    _constraint = DirectionalConstraint.HORIZONTAL;
                } else if (x == location.x && y != location.y) {
                    _constraint = DirectionalConstraint.VERTICAL;
                }
            }
        } else {
            _constraint = null;
        }
        location.set(x, y);
        _entry.elevation = _editor.getGrid().getElevation();
        _cursor.update(_entry);

        // if we are dragging, consider performing another placement
        boolean paint = _editor.isFirstButtonDown(), erase = _editor.isThirdButtonDown();
        if ((paint || erase) && !location.equals(_lastPlacement)) {
            if (erase) {
                paintTile(true);
            } else {
                // make sure we've moved at least one tile length in one direction
                if (Math.abs(location.x - _lastPlacement.x) >= width ||
                        Math.abs(location.y - _lastPlacement.y) >= height) {
                    paintTile(false);
                }
            }
        }
    }

    /**
     * Paints the current tile.
     *
     * @param erase if true, just erase the tiles under the entry.
     */
    protected void paintTile (boolean erase)
    {
        // remove any tiles underneath
        Rectangle region = new Rectangle();
        _entry.getRegion(_entry.getConfig(_editor.getConfigManager()), region);
        ArrayList<TileEntry> underneath = new ArrayList<TileEntry>();
        _scene.getTileEntries(region, underneath);
        _editor.removeEntries(underneath);
        // add the tile if we're not erasing
        if (!erase) {
            _editor.addEntries((TileEntry)_entry.clone());
        }
        _lastPlacement.set(_entry.getLocation());
    }

    @Override
    protected void referenceChanged (ConfigReference<TileConfig> ref)
    {
        super.referenceChanged(ref);
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

        @Override
        public ConfigReference<TileConfig> getReference ()
        {
            return tile;
        }

        @Override
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

    /** The directional constraint, if any. */
    protected DirectionalConstraint _constraint;

    /** The location at which we last placed. */
    protected Coord _lastPlacement = new Coord();

    /** Holds the result on an intersection test. */
    protected Vector3f _isect = new Vector3f();
}
