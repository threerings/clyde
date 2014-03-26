package com.threerings.tudey.client.util;

import java.lang.ref.SoftReference;

import java.nio.ByteBuffer;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.List;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.threerings.config.ConfigManager;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.math.FloatMath;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.opengl.gui.Image;
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

import com.threerings.tudey.space.SpaceElement;

import com.threerings.tudey.util.TudeyContext;
import com.threerings.tudey.util.Coord;
import com.threerings.tudey.util.CoordIntMap;

import static com.threerings.ClydeLog.log;

/**
 * Maintains a large map of the scene than can be rendered to the UI.
 */
public class LargeSceneMap
    implements PlaceView, TudeySceneModel.Observer
{
    /**
     * Creates a new large sceen map
     */
    public LargeSceneMap (TudeyContext ctx, TudeySceneView view)
    {
        _ctx = ctx;
        _view = view;
        setColors(Color4f.GRAY, Color4f.WHITE, Color4f.RED);
    }

    /**
     * Sets the colors to use to represent floors and walls.
     */
    public void setColors (Color4f floor, Color4f wall, Color4f walkable)
    {
        _floor = floor;
        _wall = wall;
        _walkable = walkable;
    }

    /**
     * Set the mask for collision flags we actually care about.
     */
    public void setCollisionFlagMask (int flagMask)
    {
        _flagMask = flagMask;
    }

    public void rebuildMap ()
    {
        clearMap();
        build();
    }

    /**
     * Sets the scene model and rebuilds the map.
     */
    public void setSceneModel (TudeySceneModel sceneModel)
    {
        _sceneModel = sceneModel;
        rebuildMap();
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
            renderer.setMatrixMode(GL11.GL_MODELVIEW);
        }
    }

    public void addEntrance (int x, int y)
    {
        _entrances.add(new Coord(x, y));
    }

    /**
     * Updates the textures and buffers at the coordinates.
     */
    public void updateLocations (Set<Coord> coords)
    {
        for (Coord coord : coords) {
            updateBuffer(coord);
        }
        createTextures();
    }

    /**
     * Clears the map.
     */
    public void clearMap ()
    {
        _buffers.clear();
        _textures.clear();
        _traversable.clear();
        _missing.clear();
        _types.clear();
        _entrances.clear();
    }

    /**
     * Does a breadth-first traversal of the ground tiles "graph" starting at the entrance.
     */
    public void populateTraversable ()
    {
        if (_entrances.isEmpty()) {
            return;
        }
        _traversable.clear();

        ArrayDeque<Coord> queue = new ArrayDeque<Coord>();

        queue.addAll(_entrances);

        Coord coord = queue.poll();
        while (coord != null) {
            for (int ii = 0; ii < 4; ii++) {
                Coord newCoord = new Coord(coord.x + XS[ii], coord.y + YS[ii]);
                Integer flag = _types.get(newCoord);
                if (flag == null) {
                    logMissingTile(newCoord);
                } else if (flag.equals(0) && _traversable.add(newCoord)) {
                    queue.add(newCoord);
                }
            }
            coord = queue.poll();
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
        clearMap();
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (TudeySceneModel.Entry entry)
    {
        addEntry(entry);
        updateLocations(_types.keySet());
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (TudeySceneModel.Entry oentry, TudeySceneModel.Entry nentry)
    {
        removeEntry(oentry);
        addEntry(nentry);
        updateLocations(_types.keySet());
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (TudeySceneModel.Entry oentry)
    {
        removeEntry(oentry);
        updateLocations(_types.keySet());
    }

    /** Returns the set of missing tiles. */
    public Set<Coord> getMissing ()
    {
        return _missing;
    }

    /** Logs the missing tile. */
    protected void logMissingTile (Coord coord)
    {
        _missing.add(coord);
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

    /** The size of the buffers in bytes. */
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
            addEntry(entry);
        }

        populateTraversable();
        updateLocations(_types.keySet());
    }

    /** The alpha the x, y location should be rendered at. */
    protected float getAlphaAtLocation (Coord coord)
    {
        return 1f;
    }

    /**
     *  Adds the specific entry to the map.
     */
    protected void addEntry (TudeySceneModel.Entry entry)
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

        int flags = _flagMask & entry.getCollisionFlags(_sceneModel.getConfigManager());
        if (entry instanceof TudeySceneModel.PlaceableEntry) {
            TudeySceneModel.PlaceableEntry pentry =
                (TudeySceneModel.PlaceableEntry)entry;
            PlaceableConfig.Original config = pentry.getConfig(_sceneModel.getConfigManager());
            if (config.defaultEntrance) {
                Vector2f trans = pentry.getTranslation(_sceneModel.getConfigManager());
                addEntrance((int)trans.x, (int)trans.y);
            }
            if (flags == 0 && !config.floorTile) {
                return;
            }
        } else if (flags == 0) {
            return;
        }
        Shape shape = entry.createShape(_sceneModel.getConfigManager());
        if (shape == null) {
            return;
        }
        Rect bounds = shape.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        int minx = FloatMath.ifloor(min.x);
        int maxx = FloatMath.ifloor(max.x);
        int miny = FloatMath.ifloor(min.y);
        int maxy = FloatMath.ifloor(max.y);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                if (intersects(xx, yy, shape)) {
                    int type = Math.max(_types.get(xx, yy), flags);
                    _types.put(xx, yy, type);
                }
            }
        }
    }

    /**
     *  Removes the specific entry to the map.
     */
    protected void removeEntry (TudeySceneModel.Entry entry)
    {
        if (entry instanceof TudeySceneModel.TileEntry) {
            TudeySceneModel.TileEntry tentry = (TudeySceneModel.TileEntry)entry;
            TileConfig.Original config = tentry.getConfig(_sceneModel.getConfigManager());
            tentry.getRegion(config, _region);
            for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
                for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                    updateQuad(xx, yy);
                    update(xx, yy);
                }
            }
            return;
        }

        int flags = _flagMask & entry.getCollisionFlags(_sceneModel.getConfigManager());
        if (flags == 0) {
            return;
        }
        Shape shape = entry.createShape(_sceneModel.getConfigManager());
        if (shape == null) {
            return;
        }
        Rect bounds = shape.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        int minx = FloatMath.ifloor(min.x);
        int maxx = FloatMath.ifloor(max.x);
        int miny = FloatMath.ifloor(min.y);
        int maxy = FloatMath.ifloor(max.y);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                if (intersects(xx, yy, shape)) {
                    updateQuad(xx, yy);
                    update(xx, yy);
                }
            }
        }
    }

    /**
     * Updates the specified location.
     */
    protected void update (int x, int y)
    {
        int type = NULL_VALUE;
        TudeySceneModel.TileEntry tentry = _sceneModel.getTileEntry(x, y);
        ConfigManager cfgmgr = _sceneModel.getConfigManager();
        if (tentry != null) {
            type = tentry.getCollisionFlags(tentry.getConfig(cfgmgr), x, y);
        }
        _sceneModel.getSpace().getIntersecting(_quad, _elements);
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            SpaceElement element = _elements.get(ii);
            int flags = _flagMask &
                ((TudeySceneModel.Entry)element.getUserObject()).getCollisionFlags(cfgmgr);
            if (flags != 0) {
                type = Math.max(type, flags);
            }
        }
        _elements.clear();
        _types.put(x, y, type);
    }

    /**
     * Updates the buffer for the specific location.
     */
    protected void updateBuffer (Coord coord)
    {
        int x = coord.x;
        int y = coord.y;
        int tx = (int)Math.floor((float)x / getBufferWidth());
        int ty = (int)Math.floor((float)y / getBufferWidth());
        int bx = (((x % getBufferWidth()) + getBufferWidth()) % getBufferWidth());
        int by = (((y % getBufferWidth()) + getBufferWidth()) % getBufferWidth());

        putColor(getBuffer(tx, ty), _types.get(x, y), bx, by,
            getAlphaAtLocation(coord), _traversable.contains(coord));
    }

    /**
     * Creates all the textures.
     */
    protected void createTextures ()
    {
        for (Map.Entry<Coord, ByteBuffer> entry : _buffers.entrySet()) {
            Texture2D texture = _textures.get(entry.getKey());
            if (texture == null) {
                texture = new Texture2D(_ctx.getRenderer());
                texture.setFilters(GL11.GL_NEAREST, GL11.GL_NEAREST);
                texture.setWrap(GL11.GL_CLAMP, GL11.GL_CLAMP);
                _textures.put(entry.getKey(), texture);
            }
            texture.setImage(
                0, GL11.GL_RGBA, getBufferWidth(), getBufferWidth(), false,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, entry.getValue());
        }
    }

    /**
     * Puts the color corresponding to the specified type into the supplied buffer.
     */
    protected void putColor (ByteBuffer buf, int type, int x, int y, float alpha, boolean walkable)
    {
        int offset = (y * getBufferWidth() + x) * 4;
        byte[] color = getColor(offset, type, alpha, walkable);

        buf.position(offset);
        buf.put(color);
        buf.rewind();
    }

    protected byte[] getColor (int offset, int type, float alpha, boolean walkable)
    {
        if (walkable){
            return getBytes(_walkable, alpha);
        } else if (type == NULL_VALUE) {
            return EMPTY_COLOR;
        } else if (type == 0) {
            return getBytes(_floor, alpha);
        } else {
            return getBytes(_wall, alpha);
        }
    }

    /**
     * Get buffer at location.
     */
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

    /**
     * Updates the coordinates of the quad to encompass the specified grid cell.
     */
    protected void updateQuad (int x, int y)
    {
        updateQuad((float)x, (float)y, x + 1f, y + 1f);
    }

    protected void updateQuad(float lx, float ly, float ux, float uy)
    {
        _quad.getVertex(0).set(lx, ly);
        _quad.getVertex(1).set(ux, ly);
        _quad.getVertex(2).set(ux, uy);
        _quad.getVertex(3).set(lx, uy);
        _quad.getBounds().getMinimumExtent().set(lx, ly);
        _quad.getBounds().getMaximumExtent().set(ux, uy);
    }

    protected boolean intersects (int x, int y, Shape shape)
    {

        Polygon rect = new Polygon();
        float length = 1f / SUB_DIVISION;

        int intersectionCount = 0;
        int nonIntersectionCount = 0;
        int intersectsThreshold = ((SUB_DIVISION * SUB_DIVISION) / 2);

        for (int ii = 0; ii < SUB_DIVISION; ii++) {
            float xmin = x + (length * ii);
            float xmax = x + (length * (ii + 1));

            for (int jj = 0; jj < SUB_DIVISION; jj++) {
                float ymin = y + (length * jj);
                float ymax = y + (length * (jj + 1));

                updateQuad(xmin, ymin, xmax, ymax);
                if (shape.intersects(_quad)) {
                    intersectionCount++;
                    if (intersectionCount > intersectsThreshold) {
                        return true;
                    }
                } else {
                    nonIntersectionCount++;
                    if (nonIntersectionCount > intersectsThreshold) {
                        return false;
                    }
                }
            }
        }

        return intersectionCount > intersectsThreshold;
    }

    /**
     * Get the byte values for the color at the specified alpha.
     */
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
    protected CoordIntMap _types = new CoordIntMap(3, NULL_VALUE);

    /** Stores the coordinates where a tile has been deemed "missing". */
    protected Set<Coord> _missing = Sets.newHashSet();

    /** The map textures. */
    protected Map<Coord, Texture2D> _textures = Maps.newHashMap();

    /** The RGBA color to use for floors. */
    protected Color4f _floor;

    /** The RGBA color to use for walls. */
    protected Color4f _wall;

    /** The RBGA color used for floors that are walkable. */
    protected Color4f _walkable;

    /** A mask of the collision flags we actually care about. */
    protected int _flagMask = ~0;

    /** Reusable texture unit array for unmasked rendering. */
    protected TextureUnit[] _unmasked = new TextureUnit[] { new TextureUnit() };

    /** Reusable texture unit array for masked rendering. */
    protected TextureUnit[] _masked = new TextureUnit[] { new TextureUnit(), new TextureUnit() };

    /** Reusable coordinate object. */
    protected Coord _coord = new Coord();

    /** The default entrances for the map. */
    protected Set<Coord> _entrances = Sets.newHashSet();

    /** A set containing the coordinates which can be reached. */
    protected Set<Coord> _traversable = Sets.newHashSet();

    /** Region object to reuse. */
    protected Rectangle _region = new Rectangle();

    /** Used to store tile shapes for intersecting testing. */
    protected Polygon _quad = new Polygon(4);

    /** Holds elements during intersection testing. */
    protected List<SpaceElement> _elements = Lists.newArrayList();

    /** How fine we divide the rectangle when checking for intersections. */
    protected static final int SUB_DIVISION = 3;

    /** The color to use for empty locations. */
    protected static final byte[] EMPTY_COLOR = new byte[] { 0x0, 0x0, 0x0, 0x0 };

    /** The value for null in CoordIntMap. */
    protected static final int NULL_VALUE = -3;

    /** Coordinate modifiers for getting up, down, left, right tiles. */
    protected static final int[] XS = {1, 0, -1, 0};
    protected static final int[] YS = {0, -1, 0, 1};
}
