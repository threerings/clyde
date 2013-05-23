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

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.math.Vector3f;

import com.threerings.opengl.gui.util.Rectangle;

import com.threerings.tudey.client.util.RectangleElement;
import com.threerings.tudey.config.WallConfig;
import com.threerings.tudey.util.Coord;
import com.threerings.tudey.util.CoordSet;
import com.threerings.tudey.util.TilePainter;
import com.threerings.tudey.util.TudeySceneMetrics;

/**
 * The wall brush tool.
 */
public class WallBrush extends ConfigTool<WallConfig>
{
    /**
     * Creates the wall brush tool.
     */
    public WallBrush (SceneEditor editor)
    {
        super(editor, WallConfig.class, new WallReference());
    }

    @Override
    public void init ()
    {
        _inner = new RectangleElement(_editor, true);
        _inner.getColor().set(1f, 1f, 0f, 1f);
        _outer = new RectangleElement(_editor, true);
        _outer.getColor().set(0.5f, 0.5f, 0f, 1f);
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
            _inner.composite();
            _outer.composite();
        }
    }

    @Override
    public void mousePressed (MouseEvent event)
    {
        int button = event.getButton();
        boolean paint = (button == MouseEvent.BUTTON1), erase = (button == MouseEvent.BUTTON3);
        if ((paint || erase) && _cursorVisible) {
            paintWall(erase, true);
        }
    }

    @Override
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
        if (!(_cursorVisible = getMousePlaneIntersection(_isect) && !_editor.isSpecialDown())) {
            return;
        }
        WallReference wref = (WallReference)_eref;
        int iwidth = TudeySceneMetrics.getTileWidth(wref.width, wref.height, _rotation);
        int iheight = TudeySceneMetrics.getTileHeight(wref.width, wref.height, _rotation);
        int owidth = iwidth + 2, oheight = iheight + 2;

        int x = Math.round(_isect.x - iwidth*0.5f), y = Math.round(_isect.y - iheight*0.5f);
        if (_editor.isShiftDown()) {
            if (_constraint == DirectionalConstraint.HORIZONTAL) {
                y = _location.y;
            } else if (_constraint == DirectionalConstraint.VERTICAL) {
                x = _location.x;
            } else { // _constraint == null
                if (x != _location.x && y == _location.y) {
                    _constraint = DirectionalConstraint.HORIZONTAL;
                } else if (x == _location.x && y != _location.y) {
                    _constraint = DirectionalConstraint.VERTICAL;
                }
            }
        } else {
            _constraint = null;
        }
        _location.set(x, y);
        _inner.getRegion().set(x, y, iwidth, iheight);
        _outer.getRegion().set(x - 1, y - 1, owidth, oheight);

        int elevation = _editor.getGrid().getElevation();
        _inner.setElevation(elevation);
        _outer.setElevation(elevation);

        // if we are dragging, consider performing another paint operation
        boolean paint = _editor.isFirstButtonDown(), erase = _editor.isThirdButtonDown();
        if ((paint || erase) && !_inner.getRegion().equals(_lastPainted)) {
            paintWall(erase, false);
        }
    }

    /**
     * Paints the cursor region with wall.
     *
     * @param erase if true, erase the region by painting with the null wall type.
     * @param revise if true, replace existing wall tiles with different variants.
     */
    protected void paintWall (boolean erase, boolean revise)
    {
        TilePainter painter = new TilePainter(_editor.getConfigManager(), _scene, _editor);
        Rectangle region = _inner.getRegion();
        painter.paintWall(
            new CoordSet(region), _eref.getReference(),
            _editor.getGrid().getElevation(), erase, revise);
        _lastPainted.set(region);
    }

    /**
     * Allows us to edit the wall reference.
     */
    protected static class WallReference extends EditableReference<WallConfig>
    {
        /** The wall reference. */
        @Editable(nullable=true)
        public ConfigReference<WallConfig> wall;

        /** The width of the brush. */
        @Editable(min=1, hgroup="d")
        public int width = 1;

        /** The height of the brush. */
        @Editable(min=1, hgroup="d")
        public int height = 1;

        @Override
        public ConfigReference<WallConfig> getReference ()
        {
            return wall;
        }

        @Override
        public void setReference (ConfigReference<WallConfig> ref)
        {
            wall = ref;
        }
    }

    /** The inner and outer cursors. */
    protected RectangleElement _inner, _outer;

    /** Whether or not the cursor is in the window. */
    protected boolean _cursorVisible;

    /** The directional constraint, if any. */
    protected DirectionalConstraint _constraint;

    /** The location of the cursor. */
    protected Coord _location = new Coord();

    /** The rotation of the cursor. */
    protected int _rotation;

    /** The last painted region. */
    protected Rectangle _lastPainted = new Rectangle();

    /** Holds the result on an intersection test. */
    protected Vector3f _isect = new Vector3f();
}
