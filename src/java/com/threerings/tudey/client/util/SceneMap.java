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

import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.config.ConfigManager;
import com.threerings.math.FloatMath;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.opengl.gui.Image;
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
import com.threerings.tudey.shape.Polygon;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.space.SpaceElement;
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
        float sx, float sy, float swidth, float sheight, int tx, int ty,
        int twidth, int theight, float alpha, Image mask)
    {
        Renderer renderer = _ctx.getRenderer();
        renderer.setColorState(alpha, alpha, alpha, alpha);

        // prepare the mask image if any
        float mwidth = 1f, mheight = 1f;
        if (mask != null) {
            Texture2D mtex = mask.getTexture(renderer);
            _masked[1].setTexture(mtex);
            mwidth = mask.getWidth() / (float)mtex.getWidth();
            mheight = mask.getHeight() / (float)mtex.getHeight();
        }

        // iterate over all intersecting blocks
        int xmin = (int)FloatMath.floor(sx / TEXTURE_SIZE);
        int ymin = (int)FloatMath.floor(sy / TEXTURE_SIZE);
        int xmax = (int)FloatMath.floor((sx + swidth - 1f) / TEXTURE_SIZE);
        int ymax = (int)FloatMath.floor((sy + sheight - 1f) / TEXTURE_SIZE);
        for (int yy = ymin; yy <= ymax; yy++) {
            for (int xx = xmin; xx <= xmax; xx++) {
                Texture2D texture = _textures.get(_coord.set(xx, yy));
                if (texture == null) {
                    continue;
                }
                // find the intersection of the block with the rendered section
                float bx1 = xx << TEXTURE_POT, bx2 = (xx + 1) << TEXTURE_POT;
                float by1 = yy << TEXTURE_POT, by2 = (yy + 1) << TEXTURE_POT;
                float ix1 = Math.max(sx, bx1), ix2 = Math.min(sx + swidth, bx2);
                float iy1 = Math.max(sy, by1), iy2 = Math.min(sy + sheight, by2);

                // compute the texture coordinates within the block
                float ls = (ix1 - bx1) / TEXTURE_SIZE;
                float us = (ix2 - bx1) / TEXTURE_SIZE;
                float lt = (iy1 - by1) / TEXTURE_SIZE;
                float ut = (iy2 - by1) / TEXTURE_SIZE;

                // compute the proportional coordinates
                float lx = (ix1 - sx) / (float)swidth;
                float ux = (ix2 - sx) / (float)swidth;
                float ly = (iy1 - sy) / (float)sheight;
                float uy = (iy2 - sy) / (float)sheight;

                // now the mask coordinates
                float mls = lx * mwidth;
                float mus = ux * mwidth;
                float mlt = ly * mheight;
                float mut = uy * mheight;

                // now the onscreen location
                lx = lx * twidth + tx;
                ux = ux * twidth + tx;
                ly = ly * theight + ty;
                uy = uy * theight + ty;

                // prepare the texture units
                TextureUnit[] units = (mask == null) ? _unmasked : _masked;
                units[0].setTexture(texture);

                // render the block
                renderer.setTextureState(units);
                GL11.glBegin(GL11.GL_QUADS);
                if (mask == null) {
                    GL11.glTexCoord2f(ls, lt);
                    GL11.glVertex2f(lx, ly);
                    GL11.glTexCoord2f(us, lt);
                    GL11.glVertex2f(ux, ly);
                    GL11.glTexCoord2f(us, ut);
                    GL11.glVertex2f(ux, uy);
                    GL11.glTexCoord2f(ls, ut);
                    GL11.glVertex2f(lx, uy);
                } else {
                    GL11.glTexCoord2f(ls, lt);
                    ARBMultitexture.glMultiTexCoord2fARB(
                        ARBMultitexture.GL_TEXTURE1_ARB, mls, mlt);
                    GL11.glVertex2f(lx, ly);
                    GL11.glTexCoord2f(us, lt);
                    ARBMultitexture.glMultiTexCoord2fARB(
                        ARBMultitexture.GL_TEXTURE1_ARB, mus, mlt);
                    GL11.glVertex2f(ux, ly);
                    GL11.glTexCoord2f(us, ut);
                    ARBMultitexture.glMultiTexCoord2fARB(
                        ARBMultitexture.GL_TEXTURE1_ARB, mus, mut);
                    GL11.glVertex2f(ux, uy);
                    GL11.glTexCoord2f(ls, ut);
                    ARBMultitexture.glMultiTexCoord2fARB(
                        ARBMultitexture.GL_TEXTURE1_ARB, mls, mut);
                    GL11.glVertex2f(lx, uy);
                }
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
        addEntry(entry, true);

    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        removeEntry(oentry, true);
        addEntry(nentry, true);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (Entry oentry)
    {
        removeEntry(oentry, true);
    }

    /**
     * Builds the map.
     */
    protected void build ()
    {
        for (Entry entry : _sceneModel.getEntries()) {
            addEntry(entry, false);
        }
        for (Coord coord : _types.keySet()) {
            _coord.set(coord.x >> TEXTURE_POT, coord.y >> TEXTURE_POT);
            if (!_textures.containsKey(_coord)) {
                buildTexture(_coord.x, _coord.y);
            }
        }
    }

    /**
     * Adds the specified entry to the map.
     *
     * @param retexture if true, update the texture as well.
     */
    protected void addEntry (Entry entry, boolean retexture)
    {
        if (entry instanceof TileEntry) {
            TileEntry tentry = (TileEntry)entry;
            TileConfig.Original config = tentry.getConfig(_sceneModel.getConfigManager());
            tentry.getRegion(config, _region);
            for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
                for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                    int flags = tentry.getCollisionFlags(config, xx, yy);
                    int type = Math.max(_types.get(xx, yy), flags);
                    _types.put(xx, yy, type);
                    if (retexture) {
                        updateTexture(xx, yy, type);
                    }
                }
            }
            return;
        }
        int flags = entry.getCollisionFlags(_sceneModel.getConfigManager());
        if (flags == 0) {
            return;
        }
        Shape shape = entry.createShape(_sceneModel.getConfigManager());
        if (shape == null) {
            return;
        }
        Rect bounds = shape.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        int minx = (int)FloatMath.floor(min.x);
        int maxx = (int)FloatMath.floor(max.x);
        int miny = (int)FloatMath.floor(min.y);
        int maxy = (int)FloatMath.floor(max.y);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                updateQuad(xx, yy);
                if (shape.intersects(_quad)) {
                    int type = Math.max(_types.get(xx, yy), flags);
                    _types.put(xx, yy, type);
                    if (retexture) {
                        updateTexture(xx, yy, type);
                    }
                }
            }
        }
    }

    /**
     * Removes the specified entry from the map.
     *
     * @param retexture if true, update the texture as well.
     */
    protected void removeEntry (Entry entry, boolean retexture)
    {
        if (entry instanceof TileEntry) {
            TileEntry tentry = (TileEntry)entry;
            TileConfig.Original config = tentry.getConfig(_sceneModel.getConfigManager());
            tentry.getRegion(config, _region);
            for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
                for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                    updateQuad(xx, yy);
                    update(xx, yy, retexture);
                }
            }
            return;
        }
        int flags = entry.getCollisionFlags(_sceneModel.getConfigManager());
        if (flags == 0) {
            return;
        }
        Shape shape = entry.createShape(_sceneModel.getConfigManager());
        if (shape == null) {
            return;
        }
        Rect bounds = shape.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        int minx = (int)FloatMath.floor(min.x);
        int maxx = (int)FloatMath.floor(max.x);
        int miny = (int)FloatMath.floor(min.y);
        int maxy = (int)FloatMath.floor(max.y);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                updateQuad(xx, yy);
                if (shape.intersects(_quad)) {
                    update(xx, yy, retexture);
                }
            }
        }
    }

    /**
     * Updates the specified location.
     *
     * @param retexture if true, update the texture as well.
     */
    protected void update (int x, int y, boolean retexture)
    {
        int type = -1;
        TileEntry tentry = _sceneModel.getTileEntry(x, y);
        ConfigManager cfgmgr = _sceneModel.getConfigManager();
        if (tentry != null) {
            type = tentry.getCollisionFlags(tentry.getConfig(cfgmgr), x, y);
        }
        _sceneModel.getSpace().getIntersecting(_quad, _elements);
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            SpaceElement element = _elements.get(ii);
            int flags = ((Entry)element.getUserObject()).getCollisionFlags(cfgmgr);
            if (flags != 0) {
                type = Math.max(type, flags);
            }
        }
        _elements.clear();
        _types.put(x, y, type);
        if (retexture) {
            updateTexture(x, y, type);
        }
    }

    /**
     * Updates the coordinates of the quad to encompass the specified grid cell.
     */
    protected void updateQuad (int x, int y)
    {
        float lx = x, ly = y, ux = lx + 1f, uy = ly + 1f;
        _quad.getVertex(0).set(lx, ly);
        _quad.getVertex(1).set(ux, ly);
        _quad.getVertex(2).set(ux, uy);
        _quad.getVertex(3).set(lx, uy);
        _quad.getBounds().getMinimumExtent().set(lx, ly);
        _quad.getBounds().getMaximumExtent().set(ux, uy);
    }

    /**
     * Updates the texture at the specified coordinates.
     */
    protected void updateTexture (int x, int y, int type)
    {
        int tx = x >> TEXTURE_POT, ty = y >> TEXTURE_POT;
        Texture2D texture = _textures.get(_coord.set(tx, ty));
        if (texture == null) {
            buildTexture(tx, ty);
            return;
        }
        ByteBuffer buf = getBuffer(4);
        putColor(buf, type);
        buf.rewind();
        texture.setSubimage(
            0, x & TEXTURE_MASK, y & TEXTURE_MASK, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
    }

    /**
     * Builds and maps the texture at the specified coordinates.
     */
    protected void buildTexture (int tx, int ty)
    {
        ByteBuffer buf = getBuffer(TEXTURE_SIZE * TEXTURE_SIZE * 4);
        for (int yy = ty << TEXTURE_POT, yymax = yy + TEXTURE_SIZE; yy < yymax; yy++) {
            for (int xx = tx << TEXTURE_POT, xxmax = xx + TEXTURE_SIZE; xx < xxmax; xx++) {
                putColor(buf, _types.get(xx, yy));
            }
        }
        buf.rewind();
        Texture2D texture = new Texture2D(_ctx.getRenderer());
        texture.setFilters(GL11.GL_NEAREST, GL11.GL_NEAREST);
        texture.setImage(
            0, GL11.GL_RGBA, TEXTURE_SIZE, TEXTURE_SIZE, false,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        _textures.put(new Coord(tx, ty), texture);
    }

    /**
     * Puts the color corresponding to the specified type into the supplied buffer.
     */
    protected void putColor (ByteBuffer buf, int type)
    {
         if (type == -1) {
            buf.put(EMPTY_COLOR);
         } else if (type == 0) {
            buf.put(_floorColor);
         } else {
            buf.put(_wallColor);
         }
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

    /** Stores the "type" of each location (empty, floor, wall, etc). */
    protected CoordIntMap _types = new CoordIntMap();

    /** The map textures. */
    protected Map<Coord, Texture2D> _textures = Maps.newHashMap();

    /** The RGBA color to use for floors. */
    protected byte[] _floorColor;

    /** The RGBA color to use for walls. */
    protected byte[] _wallColor;

    /** Reusable texture unit array for unmasked rendering. */
    protected TextureUnit[] _unmasked = new TextureUnit[] { new TextureUnit() };

    /** Reusable texture unit array for masked rendering. */
    protected TextureUnit[] _masked = new TextureUnit[] { new TextureUnit(), new TextureUnit() };

    /** Reusable coordinate object. */
    protected Coord _coord = new Coord();

    /** Region object to reuse. */
    protected Rectangle _region = new Rectangle();

    /** Used to store tile shapes for intersecting testing. */
    protected Polygon _quad = new Polygon(4);

    /** Buffer to reuse. */
    protected SoftReference<ByteBuffer> _buf;

    /** Holds elements during intersection testing. */
    protected List<SpaceElement> _elements = Lists.newArrayList();

    /** The size of the map textures as a power of two. */
    protected static final int TEXTURE_POT = 8;

    /** The size of the map textures. */
    protected static final int TEXTURE_SIZE = (1 << TEXTURE_POT);

    /** The mask for texture locations. */
    protected static final int TEXTURE_MASK = TEXTURE_SIZE - 1;

    /** The color to use for empty locations. */
    protected static final byte[] EMPTY_COLOR = new byte[] { 0x0, 0x0, 0x0, 0x0 };
}
