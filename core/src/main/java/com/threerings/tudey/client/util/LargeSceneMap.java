package com.threerings.tudey.client.util;

import java.lang.ref.SoftReference;

import java.nio.ByteBuffer;

import java.util.Map;
import java.util.List;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;

import com.threerings.config.ConfigManager;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.math.FloatMath;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Texture2D;
import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.Renderer;

import com.threerings.tudey.client.TudeySceneView;

import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.config.PlaceableConfig;

import com.threerings.tudey.data.TudeySceneModel;

import com.threerings.tudey.shape.Polygon;
import com.threerings.tudey.shape.Shape;

import com.threerings.tudey.util.TudeyContext;
import com.threerings.tudey.util.Coord;
import com.threerings.tudey.util.CoordIntMap;

public class LargeSceneMap
    implements PlaceView, TudeySceneModel.Observer
{
    public LargeSceneMap (TudeyContext ctx, TudeySceneView view)
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
        _floor = floor;
        _wall = wall;
    }

    /**
     * Set the mask for collision flags we actually care about.
     */
    public void setCollisionFlagMask (int flagMask)
    {
        _flagMask = flagMask;
    }

    /**
     * Renders a section of the map.
     */
    public void render (
        float sx, float sy, float swidth, float sheight, int tx, int ty,
        int twidth, int theight, float alpha)
    {
        Renderer renderer = _ctx.getRenderer();
        renderer.setColorState(alpha, alpha, alpha, alpha);

        for (Map.Entry<Coord, Texture2D> entry : _textures.entrySet()) {
            Texture2D texture = entry.getValue();
            Coord coord = entry.getKey();

            // find the intersection of the block with the rendered section
            float bx1 = coord.x << getBufferPot(), bx2 = (coord.x + 1) << getBufferPot();
            float by1 = coord.y << getBufferPot(), by2 = (coord.y + 1) << getBufferPot();
            float ix1 = Math.max(sx, bx1), ix2 = Math.min(sx + swidth, bx2);
            float iy1 = Math.max(sy, by1), iy2 = Math.min(sy + sheight, by2);

            // compute the texture coordinates within the block
            float ls = (ix1 - bx1) / getBufferWidth();
            float us = (ix2 - bx1) / getBufferWidth();
            float lt = (iy1 - by1) / getBufferWidth();
            float ut = (iy2 - by1) / getBufferWidth();

            // compute the proportional coordinates
            float lx = (ix1 - sx) / swidth;
            float ux = (ix2 - sx) / swidth;
            float ly = (iy1 - sy) / sheight;
            float uy = (iy2 - sy) / sheight;

            // now the onscreen location
            lx = lx * twidth + tx;
            ux = ux * twidth + tx;
            ly = ly * theight + ty;
            uy = uy * theight + ty;

            // prepare the texture units
            TextureUnit[] units = new TextureUnit[] { new TextureUnit() };
            units[0].setTexture(texture);

            // render the block
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
            renderer.setMatrixMode(GL11.GL_MODELVIEW);
        }
    }

    public void updateLocations (Set<Coord> coords)
    {
        for (Coord coord : coords) {
            updateBuffer(coord.x, coord.y);
        }
        createTextures();
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
        _buffers.clear();
        _textures.clear();
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (TudeySceneModel.Entry entry)
    {
        addEntry(entry, true);

    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (TudeySceneModel.Entry oentry, TudeySceneModel.Entry nentry)
    {
        removeEntry(oentry, true);
        addEntry(nentry, true);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (TudeySceneModel.Entry oentry)
    {
        removeEntry(oentry, true);
    }

    /** The size of the map textures as a power of two. */
    protected int getBufferPot ()
    {
        return 6;
    }

    /** The width of the map textures. */
    protected int getBufferWidth ()
    {
        return (1 << getBufferPot());
    }

    protected int getBufferSize ()
    {
        return getBufferWidth() * getBufferWidth() * 4;
    }

    /**
     * Builds the map.
     */
    protected void build ()
    {
        for (TudeySceneModel.Entry entry : _sceneModel.getEntries()) {
            addEntry(entry, false);
        }

        updateLocations(_types.keySet());
    }

    protected float getAlphaAtLocation (int x, int y)
    {
        return 1f;
    }

    protected void addEntry (TudeySceneModel.Entry entry, boolean retexture)
    {
        if (entry instanceof TudeySceneModel.TileEntry) {
            TudeySceneModel.TileEntry tentry = (TudeySceneModel.TileEntry)entry;
            TileConfig.Original config = tentry.getConfig(_sceneModel.getConfigManager());
            tentry.getRegion(config, _region);
            for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
                for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                    int flags = _flagMask & tentry.getCollisionFlags(config, xx, yy);
                    int type = Math.max(_types.get(xx, yy), flags);
                    _types.put(xx, yy, type);
                }
            }
            return;
        }
    }

    protected void removeEntry (TudeySceneModel.Entry entry, boolean retexture)
    {
        if (entry instanceof TudeySceneModel.TileEntry) {
            TudeySceneModel.TileEntry tentry = (TudeySceneModel.TileEntry)entry;
            TileConfig.Original config = tentry.getConfig(_sceneModel.getConfigManager());
            tentry.getRegion(config, _region);
            for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
                for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                    _types.put(xx, yy, -1);
                }
            }
            return;
        }
    }

    protected void updateBuffer (int x, int y)
    {
        int tx = (int)Math.floor((float)x / getBufferWidth());
        int ty = (int)Math.floor((float)y / getBufferWidth());
        int bx = (((x % getBufferWidth()) + getBufferWidth()) % getBufferWidth());
        int by = (((y % getBufferWidth()) + getBufferWidth()) % getBufferWidth());

        ByteBuffer buf = getBuffer(tx, ty);
        putColor(buf, _types.get(x, y), bx, by, getAlphaAtLocation(x, y));
    }

    protected void createTextures ()
    {
        for (Map.Entry<Coord, ByteBuffer> entry : _buffers.entrySet()) {
            Texture2D texture = new Texture2D(_ctx.getRenderer());
            texture.setFilters(GL11.GL_NEAREST, GL11.GL_NEAREST);
            texture.setWrap(GL11.GL_CLAMP, GL11.GL_CLAMP);
            texture.setImage(
                0, GL11.GL_RGBA, getBufferWidth(), getBufferWidth(), false,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, entry.getValue());

            _textures.put(entry.getKey(), texture);
        }
    }

    /**
     * Puts the color corresponding to the specified type into the supplied buffer.
     */
    protected void putColor (ByteBuffer buf, int type, int x, int y, float alpha)
    {
        int offset = (y * getBufferWidth() + x) * 4;
        byte[] byteColor;
        if (type == -1) {
            byteColor = EMPTY_COLOR;
        } else if (type == 0) {
            byteColor = getBytes(_floor, alpha);
        } else {
            byteColor = getBytes(_wall, alpha);
        }

        buf.position(offset);
        buf.put(byteColor);
        buf.rewind();
    }

    protected ByteBuffer getBuffer (int x, int y)
    {
        _coord.set(x, y);
        ByteBuffer buf = _buffers.get(_coord);
        if (buf == null) {
            buf = BufferUtils.createByteBuffer(getBufferSize());
            _buffers.put(_coord.clone(), buf);
        }
        return buf;
    }

    protected static byte[] getBytes (Color4f color, float alpha)
    {
        return new byte[] {
            (byte)Math.round(color.r * alpha * 255f),
            (byte)Math.round(color.g * alpha * 255f),
            (byte)Math.round(color.b * alpha * 255f),
            (byte)Math.round(color.a * alpha * 255f) };
    }

    protected Map<Coord, ByteBuffer> _buffers = Maps.newHashMap();

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

    protected Color4f _floor;

    /** The RGBA color to use for walls. */
    protected byte[] _wallColor;

    protected Color4f _wall;

    /** A mask of the collision flags we actually care about. */
    protected int _flagMask = ~0;

    /** Reusable coordinate object. */
    protected Coord _coord = new Coord();

    /** Region object to reuse. */
    protected Rectangle _region = new Rectangle();

    /** Used to store tile shapes for intersecting testing. */
    protected Polygon _quad = new Polygon(4);

    /** Buffer to reuse. */
    protected SoftReference<ByteBuffer> _buf;

    /** The color to use for empty locations. */
    protected static final byte[] EMPTY_COLOR = new byte[] { 0x0, 0x0, 0x0, 0x0 };
}