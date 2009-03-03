//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.tudey.client.util;

import java.lang.ref.SoftReference;

import java.nio.ByteBuffer;

import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.config.ConfigManager;

import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.TextureUnit;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.Coord;
import com.threerings.tudey.util.CoordIntMap;
import com.threerings.tudey.util.TudeyContext;

/**
 * Maintains a simple map of the scene than can be rendered to the UI.
 */
public class SceneMap
    implements PlaceView, TudeySceneModel.Observer
{
    /**
     * Creates a new scene map.
     */
    public SceneMap (TudeyContext ctx, TudeySceneView view)
    {
        _ctx = ctx;
        _view = view;
        setColors(Color4f.GRAY, Color4f.WHITE);
    }

    /**
     * Sets the colors to use to represent floors and walls.
     */
    public void setColors (Color4f floor, Color4f wall)
    {
        _floorColor = getBytes(floor);
        _wallColor = getBytes(wall);
    }

    /**
     * Renders a section of the map.
     */
    public void render (
        int sx, int sy, int swidth, int sheight, int tx, int ty,
        int twidth, int theight, float alpha)
    {
        Renderer renderer = _ctx.getRenderer();
        renderer.setColorState(alpha, alpha, alpha, alpha);

        // iterate over all intersecting blocks
        int xmin = sx >> TEXTURE_POT, ymin = sy >> TEXTURE_POT;
        int xmax = (sx + swidth - 1) >> TEXTURE_POT, ymax = (sy + sheight - 1) >> TEXTURE_POT;
        for (int yy = ymin; yy <= ymax; yy++) {
            for (int xx = xmin; xx <= xmax; xx++) {
                TextureUnit[] units = _units.get(_coord.set(xx, yy));
                if (units == null) {
                    continue;
                }
                // find the intersection of the block with the rendered section
                int bx1 = xx << TEXTURE_POT, bx2 = (xx + 1) << TEXTURE_POT;
                int by1 = yy << TEXTURE_POT, by2 = (yy + 1) << TEXTURE_POT;
                int ix1 = Math.max(sx, bx1), ix2 = Math.min(sx + swidth, bx2);
                int iy1 = Math.max(sy, by1), iy2 = Math.min(sy + sheight, by2);
                float ls = (ix1 - bx1) / (float)TEXTURE_SIZE;
                float us = (ix2 - bx1) / (float)TEXTURE_SIZE;
                float lt = (iy1 - by1) / (float)TEXTURE_SIZE;
                float ut = (iy2 - by1) / (float)TEXTURE_SIZE;
                float lx = (ix1 - sx) * twidth / swidth + tx;
                float ux = (ix2 - sx) * twidth / swidth + tx;
                float ly = (iy1 - sy) * theight / sheight + ty;
                float uy = (iy2 - sy) * theight / sheight + ty;
                renderer.setTextureState(units);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(ls, lt);
                GL11.glVertex2f(lx, ly);
                GL11.glTexCoord2f(us, lt);
                GL11.glVertex2f(ux, ly);
                GL11.glTexCoord2f(us, ut);
                GL11.glVertex2f(ux, uy);
                GL11.glTexCoord2f(ls, ut);
                GL11.glVertex2f(lx, uy);
                GL11.glEnd();
            }
        }
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _sceneModel = (TudeySceneModel)_ctx.getSceneDirector().getScene().getSceneModel();
        _sceneModel.addObserver(this);
        build();
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        _sceneModel.removeObserver(this);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (Entry entry)
    {
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (Entry oentry, Entry nentry)
    {
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (Entry oentry)
    {
    }

    /**
     * Builds the map.
     */
    protected void build ()
    {
        CoordIntMap types = new CoordIntMap();
        for (Entry entry : _sceneModel.getEntries()) {
            addEntry(types, entry);
        }
        for (Coord coord : types.keySet()) {
            _coord.set(coord.x >> TEXTURE_POT, coord.y >> TEXTURE_POT);
            if (!_units.containsKey(_coord)) {
                _units.put((Coord)_coord.clone(), buildUnits(types, _coord.x, _coord.y));
            }
        }
    }

    /**
     * Adds the specified entry to the map.
     */
    protected void addEntry (CoordIntMap types, Entry entry)
    {
        if (!(entry instanceof TileEntry)) {
            return; // nothing for now
        }
        TileEntry tentry = (TileEntry)entry;
        TileConfig.Original config = tentry.getConfig(_sceneModel.getConfigManager());
        tentry.getRegion(config, _region);
        for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
            for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                int flags = tentry.getCollisionFlags(config, xx, yy);
                types.put(xx, yy, flags);
            }
        }
    }

    /**
     * Builds and returns the texture at the specified coordinates.
     */
    protected TextureUnit[] buildUnits (CoordIntMap types, int tx, int ty)
    {
        ByteBuffer buf = getBuffer(TEXTURE_SIZE * TEXTURE_SIZE * 4);
        for (int yy = ty << TEXTURE_POT, yymax = yy + TEXTURE_SIZE; yy < yymax; yy++) {
            for (int xx = tx << TEXTURE_POT, xxmax = xx + TEXTURE_SIZE; xx < xxmax; xx++) {
                int type = types.get(xx, yy);
                if (type == -1) {
                    buf.put(EMPTY_COLOR);
                } else if (type == 0) {
                    buf.put(_floorColor);
                } else {
                    buf.put(_wallColor);
                }
            }
        }
        buf.rewind();
        Texture2D texture = new Texture2D(_ctx.getRenderer());
        texture.setFilters(GL11.GL_NEAREST, GL11.GL_NEAREST);
        texture.setImage(
            0, GL11.GL_RGBA, TEXTURE_SIZE, TEXTURE_SIZE, false,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        return new TextureUnit[] { new TextureUnit(texture) };
    }

    /**
     * Retrieves/creates a byte buffer of the requested size.
     */
    protected ByteBuffer getBuffer (int size)
    {
        ByteBuffer buf = (_buf == null) ? null : _buf.get();
        if (buf == null || buf.capacity() < size) {
            _buf = new SoftReference<ByteBuffer>(buf = BufferUtils.createByteBuffer(size));
        }
        return buf;
    }

    /**
     * Converts the specified color into a byte array.
     */
    protected static byte[] getBytes (Color4f color)
    {
        return new byte[] {
            (byte)Math.round(color.r * 255f),
            (byte)Math.round(color.g * 255f),
            (byte)Math.round(color.b * 255f),
            (byte)Math.round(color.a * 255f) };
    }

    /** The application context. */
    protected TudeyContext _ctx;

    /** The scene view. */
    protected TudeySceneView _view;

    /** The scene model. */
    protected TudeySceneModel _sceneModel;

    /** The map texture units. */
    protected Map<Coord, TextureUnit[]> _units = Maps.newHashMap();

    /** The RGBA color to use for floors. */
    protected byte[] _floorColor;

    /** The RGBA color to use for walls. */
    protected byte[] _wallColor;

    /** Reusable coordinate object. */
    protected Coord _coord = new Coord();

    /** Region object to reuse. */
    protected Rectangle _region = new Rectangle();

    /** Buffer to reuse. */
    protected SoftReference<ByteBuffer> _buf;

    /** The size of the map textures as a power of two. */
    protected static final int TEXTURE_POT = 8;

    /** The size of the map textures. */
    protected static final int TEXTURE_SIZE = (1 << TEXTURE_POT);

    /** The color to use for empty locations. */
    protected static final byte[] EMPTY_COLOR = new byte[] { 0x0, 0x0, 0x0, 0x0 };
}
