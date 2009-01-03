//
// $Id$

package com.threerings.tudey.data;

import java.io.IOException;

import java.lang.ref.SoftReference;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.WeakHashMap;

import com.google.common.collect.Maps;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ObserverList;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.whirled.data.SceneModel;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;
import com.threerings.export.util.ExportUtil;
import com.threerings.math.FloatMath;
import com.threerings.math.Matrix4f;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepUtil;

import com.threerings.opengl.gui.util.Rectangle;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.cursor.AreaCursor;
import com.threerings.tudey.client.cursor.EntryCursor;
import com.threerings.tudey.client.cursor.PathCursor;
import com.threerings.tudey.client.cursor.PlaceableCursor;
import com.threerings.tudey.client.cursor.TileCursor;
import com.threerings.tudey.client.sprite.AreaSprite;
import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.GlobalSprite;
import com.threerings.tudey.client.sprite.PathSprite;
import com.threerings.tudey.client.sprite.PlaceableSprite;
import com.threerings.tudey.client.sprite.TileSprite;
import com.threerings.tudey.config.AreaConfig;
import com.threerings.tudey.config.HandlerConfig;
import com.threerings.tudey.config.PathConfig;
import com.threerings.tudey.config.PlaceableConfig;
import com.threerings.tudey.config.SceneGlobalConfig;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.shape.Compound;
import com.threerings.tudey.shape.Point;
import com.threerings.tudey.shape.Polygon;
import com.threerings.tudey.shape.Segment;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.ShapeElement;
import com.threerings.tudey.space.HashSpace;
import com.threerings.tudey.space.SpaceElement;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.Coord;
import com.threerings.tudey.util.CoordIntMap;
import com.threerings.tudey.util.CoordIntMap.CoordIntEntry;
import com.threerings.tudey.util.TudeyContext;
import com.threerings.tudey.util.TudeySceneMetrics;

import static com.threerings.tudey.Log.*;

/**
 * Contains a representation of a Tudey scene.
 */
public class TudeySceneModel extends SceneModel
    implements ActorAdvancer.Environment, Exportable
{
    /**
     * An interface for objects interested in changes to the scene model.
     */
    public interface Observer
    {
        /**
         * Notes that an entry has been added to the scene.
         */
        public void entryAdded (Entry entry);

        /**
         * Notes that an entry has been updated within the scene.
         */
        public void entryUpdated (Entry oentry, Entry nentry);

        /**
         * Notes that an entry has been removed from the scene.
         */
        public void entryRemoved (Entry oentry);
    }

    /**
     * An entry in the scene.
     */
    public static abstract class Entry extends DeepObject
        implements Exportable
    {
        /**
         * Returns the key for this entry.
         */
        public abstract Object getKey ();

        /**
         * Sets this entry's config reference.
         */
        public abstract void setReference (ConfigReference reference);

        /**
         * Returns a reference to this entry's config reference.
         */
        public abstract ConfigReference getReference ();

        /**
         * Returns the elevation of the entry, or {@link Integer#MIN_VALUE} for none.
         */
        public int getElevation ()
        {
            return Integer.MIN_VALUE;
        }

        /**
         * Finds the bounds of the entry.
         */
        public void getBounds (ConfigManager cfgmgr, Rect result)
        {
            result.setToEmpty();
        }

        /**
         * Returns the entry's collision flags.
         */
        public int getCollisionFlags (ConfigManager cfgmgr)
        {
            return 0;
        }

        /**
         * Returns the name of the server-side logic class to use for the entry, or
         * <code>null</code> for none.
         */
        public abstract String getLogicClassName (ConfigManager cfgmgr);

        /**
         * Returns the entry's tag (or the empty string for none).
         */
        public abstract String getTag (ConfigManager cfgmgr);

        /**
         * Returns the entry's handler configs, if any.
         */
        public abstract HandlerConfig[] getHandlers (ConfigManager cfgmgr);

        /**
         * Returns the entry's approximate translation.
         */
        public Vector2f getTranslation (ConfigManager cfgmgr)
        {
            return Vector2f.ZERO;
        }

        /**
         * Returns the entry's approximate rotation.
         */
        public float getRotation (ConfigManager cfgmgr)
        {
            return 0f;
        }

        /**
         * Creates the shape for this entry, or returns <code>null</code> for none.
         */
        public Shape createShape (ConfigManager cfgmgr)
        {
            return null;
        }

        /**
         * Transforms the entry.
         */
        public void transform (ConfigManager cfgmgr, Transform3D xform)
        {
            // nothing by default
        }

        /**
         * Creates the space element for this entry (or returns <code>null</code> for none).
         */
        public SpaceElement createElement (ConfigManager cfgmgr)
        {
            return null;
        }

        /**
         * Creates a cursor for this entry (or returns <code>null</code> for none).
         */
        public EntryCursor createCursor (TudeyContext ctx, TudeySceneView view)
        {
            return null;
        }

        /**
         * Creates a sprite for this entry.
         */
        public abstract EntrySprite createSprite (TudeyContext ctx, TudeySceneView view);
    }

    /**
     * A tile entry.  Tiles are identified by their locations.
     */
    public static class TileEntry extends Entry
    {
        /** The configuration of the tile. */
        @Editable(nullable=true)
        public ConfigReference<TileConfig> tile;

        /** The tile's elevation. */
        @Editable
        public int elevation;

        /** The tile's rotation. */
        @Editable(min=0, max=3)
        public int rotation;

        /**
         * Returns a reference to the tile's location.
         */
        public Coord getLocation ()
        {
            return _location;
        }

        /**
         * Returns the encoded form of this tile entry.
         *
         * @param idx the tile config index.
         */
        public int encode (int idx)
        {
            return (idx << 16) | ((elevation & 0x3FFF) << 2) | rotation;
        }

        /**
         * Resolves the tile's configuration.
         */
        public TileConfig.Original getConfig (ConfigManager cfgmgr)
        {
            TileConfig config = cfgmgr.getConfig(TileConfig.class, tile);
            TileConfig.Original original = (config == null) ? null : config.getOriginal(cfgmgr);
            return (original == null) ? TileConfig.NULL_ORIGINAL : original;
        }

        /**
         * Populates the supplied transform with the transform of this tile.
         *
         * @param config the resolved configuration of the tile.
         */
        public void getTransform (TileConfig.Original config, Transform3D result)
        {
            config.getTransform(_location.x, _location.y, elevation, rotation, result);
        }

        /**
         * Populates the supplied rectangle with the region covered by this tile.
         */
        public void getRegion (TileConfig.Original config, Rectangle result)
        {
            config.getRegion(_location.x, _location.y, rotation, result);
        }

        /**
         * Returns the width of this tile after rotation.
         */
        public int getWidth (TileConfig.Original config)
        {
            return config.getWidth(rotation);
        }

        /**
         * Returns the height of this tile after rotation.
         */
        public int getHeight (TileConfig.Original config)
        {
            return config.getHeight(rotation);
        }

        /**
         * Returns the tile's collision flags at the specified coordinates.
         */
        public int getCollisionFlags (TileConfig.Original config, int x, int y)
        {
            return config.getCollisionFlags(_location.x, _location.y, rotation, x, y);
        }

        @Override // documentation inherited
        public Object getKey ()
        {
            return _location;
        }

        @Override // documentation inherited
        public void setReference (ConfigReference reference)
        {
            @SuppressWarnings("unchecked") ConfigReference<TileConfig> ref =
                (ConfigReference<TileConfig>)reference;
            tile = ref;
        }

        @Override // documentation inherited
        public ConfigReference getReference ()
        {
            return tile;
        }

        @Override // documentation inherited
        public int getElevation ()
        {
            return elevation;
        }

        @Override // documentation inherited
        public void getBounds (ConfigManager cfgmgr, Rect result)
        {
            TileConfig.Original config = getConfig(cfgmgr);
            result.getMinimumExtent().set(_location.x, _location.y);
            result.getMaximumExtent().set(
                _location.x + getWidth(config), _location.y + getHeight(config));
        }

        @Override // documentation inherited
        public String getLogicClassName (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).getLogicClassName();
        }

        @Override // documentation inherited
        public String getTag (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).tag;
        }

        @Override // documentation inherited
        public HandlerConfig[] getHandlers (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).handlers;
        }

        @Override // documentation inherited
        public Vector2f getTranslation (ConfigManager cfgmgr)
        {
            TileConfig.Original config = getConfig(cfgmgr);
            return new Vector2f(
                _location.x + getWidth(config) * 0.5f, _location.y + getHeight(config) * 0.5f);
        }

        @Override // documentation inherited
        public float getRotation (ConfigManager cfgmgr)
        {
            return rotation * FloatMath.HALF_PI;
        }

        @Override // documentation inherited
        public Shape createShape (ConfigManager cfgmgr)
        {
            TileConfig.Original config = getConfig(cfgmgr);
            float lx = _location.x, ly = _location.y;
            float ux = lx + getWidth(config), uy = ly + getHeight(config);
            return new Polygon(
                new Vector2f(lx, ly), new Vector2f(ux, ly),
                new Vector2f(ux, uy), new Vector2f(lx, uy));
        }

        @Override // documentation inherited
        public void transform (ConfigManager cfgmgr, Transform3D xform)
        {
            // update the elevation
            xform.update(Transform3D.RIGID);
            elevation += TudeySceneMetrics.getTileElevation(xform.getTranslation().z);

            // find the transformed location of the tile center
            TileConfig.Original config = getConfig(cfgmgr);
            Vector3f center = xform.transformPointLocal(new Vector3f(
                _location.x + getWidth(config) * 0.5f,
                _location.y + getHeight(config) * 0.5f, 0f));

            // adjust the rotation
            int irot = Math.round(xform.getRotation().getRotationZ() / FloatMath.HALF_PI);
            rotation = (rotation + irot) & 0x03;

            // update the location
            _location.set(
                Math.round(center.x - getWidth(config) * 0.5f),
                Math.round(center.y - getHeight(config) * 0.5f));
        }

        @Override // documentation inherited
        public EntryCursor createCursor (TudeyContext ctx, TudeySceneView view)
        {
            return new TileCursor(ctx, view, this);
        }

        @Override // documentation inherited
        public EntrySprite createSprite (TudeyContext ctx, TudeySceneView view)
        {
            return new TileSprite(ctx, view, this);
        }

        /** The location of the tile. */
        protected Coord _location = new Coord();
    }

    /**
     * An entry identified by an integer id.
     */
    public static abstract class IdEntry extends Entry
        implements Comparable<IdEntry>
    {
        /**
         * Sets the entry's unique identifier.
         */
        public void setId (int id)
        {
            _id = id;
        }

        /**
         * Returns the entry's unique identifier.
         */
        public int getId ()
        {
            return _id;
        }

        // documentation inherited from interface Comparable
        public int compareTo (IdEntry other)
        {
            return _id - other._id;
        }

        @Override // documentation inherited
        public Object getKey ()
        {
            return _id;
        }

        /** The entry's unique identifier. */
        protected int _id;
    }

    /**
     * A global entry.
     */
    public static class GlobalEntry extends IdEntry
    {
        /** The configuration of the global. */
        @Editable(nullable=true)
        public ConfigReference<SceneGlobalConfig> sceneGlobal;

        @Override // documentation inherited
        public void setReference (ConfigReference reference)
        {
            @SuppressWarnings("unchecked") ConfigReference<SceneGlobalConfig> ref =
                (ConfigReference<SceneGlobalConfig>)reference;
            sceneGlobal = ref;
        }

        @Override // documentation inherited
        public ConfigReference getReference ()
        {
            return sceneGlobal;
        }

        @Override // documentation inherited
        public String getLogicClassName (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).getLogicClassName();
        }

        @Override // documentation inherited
        public String getTag (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).tag;
        }

        @Override // documentation inherited
        public HandlerConfig[] getHandlers (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).handlers;
        }

        @Override // documentation inherited
        public EntrySprite createSprite (TudeyContext ctx, TudeySceneView view)
        {
            return new GlobalSprite(ctx, view, this);
        }

        /**
         * Returns the path config implementation.
         */
        protected SceneGlobalConfig.Original getConfig (ConfigManager cfgmgr)
        {
            SceneGlobalConfig config = cfgmgr.getConfig(SceneGlobalConfig.class, sceneGlobal);
            SceneGlobalConfig.Original original = (config == null) ?
                null : config.getOriginal(cfgmgr);
            return (original == null) ? SceneGlobalConfig.NULL_ORIGINAL : original;
        }
    }

    /**
     * A placeable entry.
     */
    public static class PlaceableEntry extends IdEntry
    {
        /** The configuration of the placeable. */
        @Editable(nullable=true)
        public ConfigReference<PlaceableConfig> placeable;

        /** The transform of the placeable. */
        @Editable(step=0.01)
        public Transform3D transform = new Transform3D();

        @Override // documentation inherited
        public void setReference (ConfigReference reference)
        {
            @SuppressWarnings("unchecked") ConfigReference<PlaceableConfig> ref =
                (ConfigReference<PlaceableConfig>)reference;
            placeable = ref;
        }

        @Override // documentation inherited
        public ConfigReference getReference ()
        {
            return placeable;
        }

        @Override // documentation inherited
        public int getElevation ()
        {
            transform.update(Transform3D.RIGID);
            return TudeySceneMetrics.getTileElevation(transform.getTranslation().z);
        }

        @Override // documentation inherited
        public void getBounds (ConfigManager cfgmgr, Rect result)
        {
            Shape shape = getConfig(cfgmgr).shape.getShape().transform(new Transform2D(transform));
            result.set(shape.getBounds());
        }

        @Override // documentation inherited
        public int getCollisionFlags (ConfigManager cfgmgr)
        {
            if (_collisionFlags == -1) {
                _collisionFlags = getConfig(cfgmgr).getCollisionFlags();
            }
            return _collisionFlags;
        }

        @Override // documentation inherited
        public String getLogicClassName (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).getLogicClassName();
        }

        @Override // documentation inherited
        public String getTag (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).tag;
        }

        @Override // documentation inherited
        public HandlerConfig[] getHandlers (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).handlers;
        }

        @Override // documentation inherited
        public Vector2f getTranslation (ConfigManager cfgmgr)
        {
            transform.update(Transform3D.RIGID);
            Vector3f translation = transform.getTranslation();
            return new Vector2f(translation.x, translation.y);
        }

        @Override // documentation inherited
        public float getRotation (ConfigManager cfgmgr)
        {
            transform.update(Transform3D.RIGID);
            return transform.getRotation().getRotationZ();
        }

        @Override // documentation inherited
        public Shape createShape (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).shape.getShape().transform(new Transform2D(transform));
        }

        @Override // documentation inherited
        public void transform (ConfigManager cfgmgr, Transform3D xform)
        {
            xform.compose(transform, transform);
        }

        @Override // documentation inherited
        public SpaceElement createElement (ConfigManager cfgmgr)
        {
            ShapeElement element = new ShapeElement(getConfig(cfgmgr).shape);
            element.getTransform().set(transform);
            element.updateBounds();
            element.setUserObject(this);
            return element;
        }

        @Override // documentation inherited
        public EntryCursor createCursor (TudeyContext ctx, TudeySceneView view)
        {
            return new PlaceableCursor(ctx, view, this);
        }

        @Override // documentation inherited
        public EntrySprite createSprite (TudeyContext ctx, TudeySceneView view)
        {
            return new PlaceableSprite(ctx, view, this);
        }

        /**
         * Returns the placeable config implementation.
         */
        protected PlaceableConfig.Original getConfig (ConfigManager cfgmgr)
        {
            PlaceableConfig config = cfgmgr.getConfig(PlaceableConfig.class, placeable);
            PlaceableConfig.Original original = (config == null) ?
                null : config.getOriginal(cfgmgr);
            return (original == null) ? PlaceableConfig.NULL_ORIGINAL : original;
        }

        /** The cached collision flags. */
        protected transient int _collisionFlags = -1;
    }

    /**
     * A path entry.
     */
    public static class PathEntry extends IdEntry
    {
        /** The configuration of the path. */
        @Editable(nullable=true)
        public ConfigReference<PathConfig> path;

        /** The path vertices. */
        @Editable
        public Vertex[] vertices = new Vertex[0];

        @Override // documentation inherited
        public void setReference (ConfigReference reference)
        {
            @SuppressWarnings("unchecked") ConfigReference<PathConfig> ref =
                (ConfigReference<PathConfig>)reference;
            path = ref;
        }

        @Override // documentation inherited
        public ConfigReference getReference ()
        {
            return path;
        }

        @Override // documentation inherited
        public void getBounds (ConfigManager cfgmgr, Rect result)
        {
            result.setToEmpty();
            for (Vertex vertex : vertices) {
                result.addLocal(vertex.createVector());
            }
        }

        @Override // documentation inherited
        public String getLogicClassName (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).getLogicClassName();
        }

        @Override // documentation inherited
        public String getTag (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).tag;
        }

        @Override // documentation inherited
        public HandlerConfig[] getHandlers (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).handlers;
        }

        @Override // documentation inherited
        public Vector2f getTranslation (ConfigManager cfgmgr)
        {
            return (vertices.length == 0) ? Vector2f.ZERO : vertices[0].createVector();
        }

        @Override // documentation inherited
        public float getRotation (ConfigManager cfgmgr)
        {
            return (vertices.length < 2) ? 0f : FloatMath.atan2(
                vertices[1].y - vertices[0].y, vertices[1].x - vertices[0].x);
        }

        @Override // documentation inherited
        public Shape createShape (ConfigManager cfgmgr)
        {
            switch (vertices.length) {
                case 0: return null;
                case 1: return new Point(vertices[0].createVector());
                default: return createShape(0, vertices.length - 1);
            }
        }

        @Override // documentation inherited
        public void transform (ConfigManager cfgmgr, Transform3D xform)
        {
            Matrix4f matrix = xform.update(Transform3D.AFFINE).getMatrix();
            for (Vertex vertex : vertices) {
                vertex.transform(matrix);
            }
        }

        @Override // documentation inherited
        public SpaceElement createElement (ConfigManager cfgmgr)
        {
            Shape shape = createShape(cfgmgr);
            if (shape == null) {
                return null;
            }
            ShapeElement element = new ShapeElement(shape);
            element.setUserObject(this);
            return element;
        }

        @Override // documentation inherited
        public EntryCursor createCursor (TudeyContext ctx, TudeySceneView view)
        {
            return new PathCursor(ctx, view, this);
        }

        @Override // documentation inherited
        public EntrySprite createSprite (TudeyContext ctx, TudeySceneView view)
        {
            return new PathSprite(ctx, view, this);
        }

        /**
         * Returns the path config implementation.
         */
        protected PathConfig.Original getConfig (ConfigManager cfgmgr)
        {
            PathConfig config = cfgmgr.getConfig(PathConfig.class, path);
            PathConfig.Original original = (config == null) ? null : config.getOriginal(cfgmgr);
            return (original == null) ? PathConfig.NULL_ORIGINAL : original;
        }

        /**
         * Creates a shape using the identified region of the vertices.
         */
        protected Shape createShape (int idx0, int idx1)
        {
            if (idx1 - idx0 == 1) {
                return new Segment(vertices[idx0].createVector(), vertices[idx1].createVector());
            }
            int mid = (idx0 + idx1) / 2;
            return new Compound(createShape(idx0, mid), createShape(mid, idx1));
        }
    }

    /**
     * An area entry.
     */
    public static class AreaEntry extends IdEntry
    {
        /** The configuration of the area. */
        @Editable(nullable=true)
        public ConfigReference<AreaConfig> area;

        /** The area vertices. */
        @Editable
        public Vertex[] vertices = new Vertex[0];

        @Override // documentation inherited
        public void setReference (ConfigReference reference)
        {
            @SuppressWarnings("unchecked") ConfigReference<AreaConfig> ref =
                (ConfigReference<AreaConfig>)reference;
            area = ref;
        }

        @Override // documentation inherited
        public ConfigReference getReference ()
        {
            return area;
        }

        @Override // documentation inherited
        public void getBounds (ConfigManager cfgmgr, Rect result)
        {
            result.setToEmpty();
            for (Vertex vertex : vertices) {
                result.addLocal(vertex.createVector());
            }
        }

        @Override // documentation inherited
        public String getLogicClassName (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).getLogicClassName();
        }

        @Override // documentation inherited
        public String getTag (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).tag;
        }

        @Override // documentation inherited
        public HandlerConfig[] getHandlers (ConfigManager cfgmgr)
        {
            return getConfig(cfgmgr).handlers;
        }

        @Override // documentation inherited
        public Vector2f getTranslation (ConfigManager cfgmgr)
        {
            if (vertices.length == 0) {
                return Vector2f.ZERO;
            }
            Vector2f result = new Vector2f();
            for (Vertex vertex : vertices) {
                result.addLocal(vertex.x, vertex.y);
            }
            return result.multLocal(1f / vertices.length);
        }

        @Override // documentation inherited
        public Shape createShape (ConfigManager cfgmgr)
        {
            switch (vertices.length) {
                case 0: return null;
                case 1: return new Point(vertices[0].createVector());
                case 2: return new Segment(vertices[0].createVector(), vertices[1].createVector());
                default:
                    Vector2f[] vectors = new Vector2f[vertices.length];
                    boolean reversed = isReversed();
                    for (int ii = 0; ii < vectors.length; ii++) {
                        int idx = reversed ? (vectors.length - ii - 1) : ii;
                        vectors[ii] = vertices[idx].createVector();
                    }
                    return new Polygon(vectors);
            }
        }

        @Override // documentation inherited
        public void transform (ConfigManager cfgmgr, Transform3D xform)
        {
            Matrix4f matrix = xform.update(Transform3D.AFFINE).getMatrix();
            for (Vertex vertex : vertices) {
                vertex.transform(matrix);
            }
        }

        @Override // documentation inherited
        public SpaceElement createElement (ConfigManager cfgmgr)
        {
            Shape shape = createShape(cfgmgr);
            if (shape == null) {
                return null;
            }
            ShapeElement element = new ShapeElement(shape);
            element.setUserObject(this);
            return element;
        }

        @Override // documentation inherited
        public EntryCursor createCursor (TudeyContext ctx, TudeySceneView view)
        {
            return new AreaCursor(ctx, view, this);
        }

        @Override // documentation inherited
        public EntrySprite createSprite (TudeyContext ctx, TudeySceneView view)
        {
            return new AreaSprite(ctx, view, this);
        }

        /**
         * Returns the area config implementation.
         */
        protected AreaConfig.Original getConfig (ConfigManager cfgmgr)
        {
            AreaConfig config = cfgmgr.getConfig(AreaConfig.class, area);
            AreaConfig.Original original = (config == null) ? null : config.getOriginal(cfgmgr);
            return (original == null) ? AreaConfig.NULL_ORIGINAL : original;
        }

        /**
         * Checks whether the vertices are given in the "wrong" (clockwise) winding order.
         */
        protected boolean isReversed ()
        {
            int cw = 0, ccw = 0;
            for (int ii = 0; ii < vertices.length; ii++) {
                Vertex v0 = vertices[ii];
                Vertex v1 = vertices[(ii + 1) % vertices.length];
                Vertex v2 = vertices[(ii + 2) % vertices.length];
                float x1 = v1.x - v0.x, y1 = v1.y - v0.y;
                float x2 = v2.x - v1.x, y2 = v2.y - v1.y;
                if (x1*y2 - y1*x2 < 0f) {
                    cw++;
                } else {
                    ccw++;
                }
            }
            return cw > ccw;
        }
    }

    /**
     * Represents a single vertex in a path or area.
     */
    public static class Vertex extends DeepObject
        implements Exportable
    {
        /** The vertex coordinates. */
        @Editable(step=0.01, hgroup="c")
        public float x, y, z;

        /**
         * Sets the coordinates of the vertex.
         */
        public void set (float x, float y, float z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * Creates a vector from this vertex.
         */
        public Vector2f createVector ()
        {
            return new Vector2f(x, y);
        }

        /**
         * Transforms this vertex in-place by the supplied matrix.
         */
        public void transform (Matrix4f matrix)
        {
            float ox = x, oy = y, oz = z;
            x = ox*matrix.m00 + oy*matrix.m10 + oz*matrix.m20 + matrix.m30;
            y = ox*matrix.m01 + oy*matrix.m11 + oz*matrix.m21 + matrix.m31;
            z = ox*matrix.m02 + oy*matrix.m12 + oz*matrix.m22 + matrix.m32;
        }
    }

    /**
     * Creates a new, empty scene model.
     */
    public TudeySceneModel ()
    {
        name = "";
        version = 1;
    }

    /**
     * Initializes the model.
     */
    public void init (ConfigManager cfgmgr)
    {
        _cfgmgr.init("scene", cfgmgr);

        // create the tile shadows now that we have the config manager
        for (CoordIntEntry entry : _tiles.coordIntEntrySet()) {
            TileEntry tentry = decodeTileEntry(entry.getKey(), entry.getIntValue());
            createShadow(tentry);
        }

        // likewise with the shapes
        for (Entry entry : _entries.values()) {
            addElement(entry);
        }
    }

    /**
     * Returns a reference to the scene's configuration manager.
     */
    public ConfigManager getConfigManager ()
    {
        return _cfgmgr;
    }

    /**
     * Adds an observer for scene changes.
     */
    public void addObserver (Observer observer)
    {
        _observers.add(observer);
    }

    /**
     * Removes a scene observer.
     */
    public void removeObserver (Observer observer)
    {
        _observers.remove(observer);
    }

    /**
     * Sets the scene's name and invalidates.
     */
    public void setName (String name)
    {
        this.name = name;
        invalidate();
    }

    /**
     * Sets the place config for the model.
     */
    public void setPlaceConfig (TudeySceneConfig config)
    {
        _placeConfig = config;
        invalidate();
    }

    /**
     * Returns a reference to the model's place config.
     */
    public TudeySceneConfig getPlaceConfig ()
    {
        return _placeConfig;
    }

    /**
     * Adds an entry to the scene, assigning it a unique id in the process if it is an
     * {@link IdEntry}.
     *
     * @return true if the entry was successfully added, false if there was already
     * an entry with the same key (in which case a warning will be logged).
     */
    public boolean addEntry (Entry entry)
    {
        return addEntry(entry, true);
    }

    /**
     * Adds an entry to the scene.
     *
     * @param assignId if true and the entry is an {@link IdEntry}, assign a unique id to
     * the entry.
     * @return true if the entry was successfully added, false if there was already
     * an entry with the same id (in which case a warning will be logged).
     */
    public boolean addEntry (final Entry entry, boolean assignId)
    {
        // assign id if appropriate
        if (assignId && entry instanceof IdEntry) {
            ((IdEntry)entry).setId(++_lastEntryId);
        }
        // add to map
        Entry oentry = add(entry);
        if (oentry != null) {
            log.warning("Attempted to replace existing entry.",
                "oentry", oentry, "nentry", entry);
            return false;
        }
        // notify the observers and report success
        _observers.apply(new ObserverList.ObserverOp<Observer>() {
            public boolean apply (Observer observer) {
                observer.entryAdded(entry);
                return true;
            }
        });
        return true;
    }

    /**
     * Updates an entry within the scene.
     *
     * @return a reference to the entry that was replaced, or <code>null</code> for none (in which
     * case a warning will be logged).
     */
    public Entry updateEntry (final Entry nentry)
    {
        // replace in map
        final Entry oentry = update(nentry);
        if (oentry == null) {
            log.warning("Attempted to update nonexistent entry.", "entry", nentry);
            return null;
        }
        // notify the observers and return the old entry
        _observers.apply(new ObserverList.ObserverOp<Observer>() {
            public boolean apply (Observer observer) {
                observer.entryUpdated(oentry, nentry);
                return true;
            }
        });
        return oentry;
    }

    /**
     * Removes an entry from the scene.
     *
     * @return a reference to the entry that was removed, or <code>null</code> for none (in which
     * case a warning will be logged).
     */
    public Entry removeEntry (Object key)
    {
        // remove from map
        final Entry oentry = remove(key);
        if (oentry == null) {
            log.warning("Missing entry to remove.", "key", key);
            return null;
        }
        // notify the observers and return the old entry
        _observers.apply(new ObserverList.ObserverOp<Observer>() {
            public boolean apply (Observer observer) {
                observer.entryRemoved(oentry);
                return true;
            }
        });
        return oentry;
    }

    /**
     * Determines whether the scene contains an entry with the supplied key.
     */
    public boolean containsEntry (Object key)
    {
        if (!(key instanceof Coord)) {
            return _entries.containsKey(key);
        }
        Coord coord = (Coord)key;
        return _tiles.containsKey(coord.x, coord.y);
    }

    /**
     * Looks up the entry with the supplied key.
     *
     * @return a reference to the identified entry, or <code>null</code> if not found.
     */
    public Entry getEntry (Object key)
    {
        if (!(key instanceof Coord)) {
            return _entries.get(key);
        }
        Coord coord = (Coord)key;
        int value = _tiles.get(coord.x, coord.y);
        return (value == -1) ? null : decodeTileEntry(coord, value);
    }

    /**
     * Returns a reference to the collection of entries.
     */
    public Collection<Entry> getEntries ()
    {
        return new AbstractCollection<Entry>() {
            public Iterator<Entry> iterator () {
                return new Iterator<Entry>() {
                    public boolean hasNext () {
                        return _tit.hasNext() || _eit.hasNext();
                    }
                    public Entry next () {
                        if (_tit.hasNext()) {
                            CoordIntEntry entry = _tit.next();
                            return decodeTileEntry(entry.getKey(), entry.getIntValue());
                        } else {
                            return _eit.next();
                        }
                    }
                    public void remove () {
                        throw new UnsupportedOperationException();
                    }
                    protected Iterator<CoordIntEntry> _tit = _tiles.coordIntEntrySet().iterator();
                    protected Iterator<Entry> _eit = _entries.values().iterator();
                };
            }
            public int size () {
                return _tiles.size() + _entries.size();
            }
        };
    }

    /**
     * Retrieves all entries intersecting the supplied shape.
     */
    public void getEntries (Shape shape, Collection<Entry> results)
    {
        // find intersecting tiles
        Rect bounds = shape.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        int minx = (int)FloatMath.floor(min.x);
        int maxx = (int)FloatMath.floor(max.x);
        int miny = (int)FloatMath.floor(min.y);
        int maxy = (int)FloatMath.floor(max.y);
        ArrayIntSet pairs = new ArrayIntSet();
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                int pair = _tileCoords.get(xx, yy);
                if (pair != EMPTY_COORD) {
                    _rect.getMinimumExtent().set(xx, yy);
                    _rect.getMaximumExtent().set(xx + 1f, yy + 1f);
                    if (shape.getIntersectionType(_rect) != Shape.IntersectionType.NONE) {
                        pairs.add(pair);
                    }
                }
            }
        }
        for (int ii = 0, nn = pairs.size(); ii < nn; ii++) {
            TileEntry entry = getTileEntry(pairs.get(ii));
            if (entry != null) {
                results.add(entry);
            }
        }

        // find intersecting elements
        ArrayList<SpaceElement> intersecting = new ArrayList<SpaceElement>();
        _space.getIntersecting(shape, intersecting);
        for (int ii = 0, nn = intersecting.size(); ii < nn; ii++) {
            results.add((Entry)intersecting.get(ii).getUserObject());
        }
    }

    /**
     * Retrieves all of the tile entries intersecting the supplied region.
     */
    public void getTileEntries (Rectangle region, Collection<TileEntry> results)
    {
        ArrayIntSet pairs = new ArrayIntSet();
        for (int yy = region.y, yymax = yy + region.height; yy < yymax; yy++) {
            for (int xx = region.x, xxmax = xx + region.width; xx < xxmax; xx++) {
                int pair = _tileCoords.get(xx, yy);
                if (pair != EMPTY_COORD) {
                    pairs.add(pair);
                }
            }
        }
        for (int ii = 0, nn = pairs.size(); ii < nn; ii++) {
            TileEntry entry = getTileEntry(pairs.get(ii));
            if (entry != null) {
                results.add(entry);
            }
        }
    }

    /**
     * Returns the tile entry intersecting the specified coordinates, if any.
     */
    public TileEntry getTileEntry (int x, int y)
    {
        int pair = _tileCoords.get(x, y);
        return (pair == EMPTY_COORD) ? null : getTileEntry(pair);
    }

    /**
     * Custom field write method.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        out.defaultWriteFields();
        out.write("name", name, "");
        out.write("version", version, 1);
        out.write("entries", _entries.values().toArray(new Entry[_entries.size()]),
            new Entry[0], Entry[].class);
    }

    /**
     * Custom field read method.
     */
    public void readFields (Importer in)
        throws IOException
    {
        in.defaultReadFields();
        name = in.read("name", "");
        version = in.read("version", 1);

        // initialize the tile config counts
        for (CoordIntEntry entry : _tiles.coordIntEntrySet()) {
            int idx = getTileConfigIndex(entry.getIntValue());
            _tileConfigs.get(idx).count++;
        }

        // initialize the reverse mapping for the tile configs
        for (int ii = 0, nn = _tileConfigs.size(); ii < nn; ii++) {
            TileConfigMapping mapping = _tileConfigs.get(ii);
            if (mapping != null) {
                _tileConfigIds.put(mapping.tile, ii);
            }
        }

        // read the entries, initialize the reference map, find the highest entry id
        for (Entry entry : in.read("entries", new Entry[0], Entry[].class)) {
            _entries.put(entry.getKey(), entry);
            _references.put(entry.getReference(), entry.getReference());
            if (entry instanceof IdEntry) {
                _lastEntryId = Math.max(_lastEntryId, ((IdEntry)entry).getId());
            }
        }
    }

    /**
     * Custom write method for streaming.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        // write the cached exported binary representation
        byte[] data = getData();
        out.writeInt(data.length);
        out.write(data);
    }

    /**
     * Custom read method for streaming.
     */
    public void readObject (ObjectInputStream in)
        throws IOException
    {
        // read the binary representation
        byte[] data = new byte[in.readInt()];
        in.read(data);

        // decode and copy its fields into this one
        TudeySceneModel nmodel = (TudeySceneModel)ExportUtil.fromBytes(data);
        DeepUtil.copy(nmodel, this);
        _data = new SoftReference<byte[]>(data);
    }

    /**
     * Returns the cached exported binary representation of the model.
     */
    public byte[] getData ()
    {
        byte[] data = (_data == null) ? null : _data.get();
        if (data == null) {
            _data = new SoftReference<byte[]>(data = ExportUtil.toBytes(this));
        }
        return data;
    }

    /**
     * Invalidates any cached data in the model, forcing it to be recreated.
     */
    public void invalidate ()
    {
        _data = null;
    }

    // documentation inherited from interface ActorAdvancer.Environment
    public boolean getPenetration (Actor actor, Shape shape, Vector2f result)
    {
        // start with zero penetration
        result.set(Vector2f.ZERO);

        // check against non-passable locations
        Rect bounds = shape.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        int minx = (int)FloatMath.floor(min.x);
        int maxx = (int)FloatMath.floor(max.x);
        int miny = (int)FloatMath.floor(min.y);
        int maxy = (int)FloatMath.floor(max.y);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                if (!actor.canCollide(_collisionFlags.get(xx, yy))) {
                    continue;
                }
                float lx = xx, ly = yy, ux = lx + 1f, uy = ly + 1f;
                _quad.getVertex(0).set(lx, ly);
                _quad.getVertex(1).set(ux, ly);
                _quad.getVertex(2).set(ux, uy);
                _quad.getVertex(3).set(lx, uy);
                _quad.getBounds().getMinimumExtent().set(lx, ly);
                _quad.getBounds().getMaximumExtent().set(ux, uy);
                if (_quad.intersects(shape)) {
                    _quad.getPenetration(shape, _penetration);
                    if (_penetration.lengthSquared() > result.lengthSquared()) {
                        result.set(_penetration);
                    }
                }
            }
        }

        // find intersecting elements
        _space.getIntersecting(shape, _intersecting);
        for (int ii = 0, nn = _intersecting.size(); ii < nn; ii++) {
            SpaceElement element = _intersecting.get(ii);
            Entry entry = (Entry)element.getUserObject();
            if (actor.canCollide(entry.getCollisionFlags(_cfgmgr))) {
                ((ShapeElement)element).getWorldShape().getPenetration(shape, _penetration);
                if (_penetration.lengthSquared() > result.lengthSquared()) {
                    result.set(_penetration);
                }
            }
        }
        _intersecting.clear();

        // if our vector is non-zero, we penetrated
        return !result.equals(Vector2f.ZERO);
    }

    @Override // documentation inherited
    public Object clone ()
    {
        return DeepUtil.copy(this, null);
    }

    /**
     * Performs the actual addition of the specified entry.
     *
     * @return the replaced entry.
     */
    protected Entry add (Entry entry)
    {
        if (!(entry instanceof TileEntry)) {
            Entry oentry = _entries.put(entry.getKey(), entry);
            if (oentry == null) {
                canonicalizeReference(entry);
                addElement(entry);
                invalidate();
            } else {
                // replace the old entry (a warning will be logged)
                _entries.put(entry.getKey(), oentry);
            }
            return oentry;
        }
        TileEntry tentry = (TileEntry)entry;
        Coord coord = tentry.getLocation();
        int idx = addTileConfig(tentry.tile);
        int ovalue = _tiles.put(coord.x, coord.y, tentry.encode(idx));
        if (ovalue != -1) {
            // replace the old value (a warning will be logged)
            _tiles.put(coord.x, coord.y, ovalue);
            removeTileConfig(idx);
            return decodeTileEntry(coord, ovalue);
        }
        createShadow(tentry);
        invalidate();
        return null;
    }

    /**
     * Performs the actual update of the specified entry.
     *
     * @return the replaced entry.
     */
    protected Entry update (Entry nentry)
    {
        if (!(nentry instanceof TileEntry)) {
            Entry oentry = _entries.put(nentry.getKey(), nentry);
            if (oentry == null) {
                // remove the entry (a warning will be logged)
                _entries.remove(nentry.getKey());
            } else {
                canonicalizeReference(nentry);
                removeElement(oentry);
                addElement(nentry);
                invalidate();
            }
            return oentry;
        }
        TileEntry tentry = (TileEntry)nentry;
        Coord coord = tentry.getLocation();
        int idx = addTileConfig(tentry.tile);
        int ovalue = _tiles.put(coord.x, coord.y, tentry.encode(idx));
        if (ovalue == -1) {
            // remove the value (a warning will be logged)
            _tiles.remove(coord.x, coord.y);
            removeTileConfig(idx);
            return null;
        }
        TileEntry oentry = decodeTileEntry(coord, ovalue);
        removeTileConfig(getTileConfigIndex(ovalue));
        deleteShadow(oentry);
        createShadow(tentry);
        invalidate();
        return oentry;
    }

    /**
     * Performs the actual removal of the identified entry.
     *
     * @return the removed entry.
     */
    protected Entry remove (Object key)
    {
        if (!(key instanceof Coord)) {
            Entry oentry = _entries.remove(key);
            if (oentry != null) {
                removeElement(oentry);
                invalidate();
            }
            return oentry;
        }
        Coord coord = (Coord)key;
        int ovalue = _tiles.remove(coord.x, coord.y);
        if (ovalue == -1) {
            return null;
        }
        TileEntry oentry = decodeTileEntry(coord, ovalue);
        removeTileConfig(getTileConfigIndex(ovalue));
        deleteShadow(oentry);
        invalidate();
        return oentry;
    }

    /**
     * Adds the entry's space element to the hash space.
     */
    protected void addElement (Entry entry)
    {
        SpaceElement element = entry.createElement(_cfgmgr);
        if (element != null) {
            _space.add(element);
            _elements.put(entry.getKey(), element);
        }
    }

    /**
     * Removes the entry's space element from the hash space.
     */
    protected void removeElement (Entry entry)
    {
        SpaceElement element = _elements.remove(entry.getKey());
        if (element != null) {
            _space.remove(element);
        }
    }

    /**
     * Creates the shadow data for the specified tile.
     */
    protected void createShadow (TileEntry entry)
    {
        int pair = entry.getLocation().encode();
        TileConfig.Original config = entry.getConfig(_cfgmgr);
        entry.getRegion(config, _region);
        for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
            for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                // add to tile coordinate mapping
                _tileCoords.put(xx, yy, pair);

                // add the collision flags, if any
                int flags = entry.getCollisionFlags(config, xx, yy);
                if (flags != 0) {
                    _collisionFlags.put(xx, yy, flags);
                }
            }
        }
    }

    /**
     * Deletes the shadow data for the supplied tile.
     */
    protected void deleteShadow (TileEntry entry)
    {
        entry.getRegion(entry.getConfig(_cfgmgr), _region);
        for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
            for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                // remove from tile coordinate mapping
                _tileCoords.remove(xx, yy);

                // remove collision flags
                _collisionFlags.remove(xx, yy);
            }
        }
    }

    /**
     * Retrieves the tile entry identified by the provided encoded coordinate pair.
     */
    protected TileEntry getTileEntry (int pair)
    {
        int x = Coord.decodeX(pair), y = Coord.decodeY(pair);
        int value = _tiles.get(x, y);
        if (value == -1) {
            log.warning("Tile shadow points to nonexistent tile.", "x", x, "y", y);
            return null;
        }
        return decodeTileEntry(x, y, value);
    }

    /**
     * Ensures that the entry's reference, if non-null, points to one of the references in the
     * {@link #_references} set (that is, that all entries whose references are equal point to
     * the same reference instance).
     */
    protected void canonicalizeReference (Entry entry)
    {
        ConfigReference eref = entry.getReference();
        if (eref == null) {
            return;
        }
        ConfigReference cref = _references.get(eref);
        if (cref == null) {
            _references.put(eref, eref);
        } else {
            entry.setReference(cref);
        }
    }

    /**
     * Adds a reference to the specified tile config and returns the index assigned to the config.
     */
    protected int addTileConfig (ConfigReference<TileConfig> tile)
    {
        TileConfigMapping mapping;
        Integer idx = _tileConfigIds.get(tile);
        if (idx == null) {
            mapping = new TileConfigMapping(tile);
            for (int ii = 0, nn = _tileConfigs.size(); ii < nn; ii++) {
                if (_tileConfigs.get(ii) == null) {
                    idx = ii;
                    _tileConfigs.set(ii, mapping);
                    break;
                }
            }
            if (idx == null) {
                idx = _tileConfigs.size();
                _tileConfigs.add(mapping);
            }
            _tileConfigIds.put(tile, idx);
        } else {
            mapping = _tileConfigs.get(idx);
        }
        mapping.count++;
        return idx;
    }

    /**
     * Removes a reference for the indexed tile config.
     */
    protected void removeTileConfig (int idx)
    {
        TileConfigMapping mapping = _tileConfigs.get(idx);
        if (--mapping.count == 0) {
            _tileConfigs.set(idx, null);
            _tileConfigIds.remove(mapping.tile);
        }
    }

    /**
     * Decodes the specified tile entry.
     */
    protected TileEntry decodeTileEntry (Coord coord, int value)
    {
        return decodeTileEntry(coord.x, coord.y, value);
    }

    /**
     * Decodes the specified tile entry.
     */
    protected TileEntry decodeTileEntry (int x, int y, int value)
    {
        TileEntry entry = new TileEntry();
        entry.getLocation().set(x, y);
        entry.tile = _tileConfigs.get(getTileConfigIndex(value)).tile;
        entry.elevation = (value << 16) >> 18;
        entry.rotation = value & 0x03;
        return entry;
    }

    /**
     * Extracts the tile configuration index from the supplied encoded tile.
     */
    protected static int getTileConfigIndex (int value)
    {
        return value >>> 16;
    }

    /**
     * Represents a type of tile identified by an integer id.
     */
    protected static class TileConfigMapping extends DeepObject
        implements Exportable
    {
        /** The tile configuration. */
        public ConfigReference<TileConfig> tile;

        /** The number of tiles of this type. */
        public transient int count;

        public TileConfigMapping (ConfigReference<TileConfig> tile)
        {
            this.tile = tile;
        }

        public TileConfigMapping ()
        {
        }
    }

    /** The place configuration. */
    protected TudeySceneConfig _placeConfig = new TudeySceneConfig();

    /** The scene configuration manager. */
    protected ConfigManager _cfgmgr = new ConfigManager();

    /** The encoded tiles. */
    protected CoordIntMap _tiles = new CoordIntMap();

    /** Tile config references by id. */
    protected ArrayList<TileConfigMapping> _tileConfigs = new ArrayList<TileConfigMapping>();

    /** Tile config ids mapped by reference. */
    protected transient HashMap<ConfigReference<TileConfig>, Integer> _tileConfigIds =
        Maps.newHashMap();

    /** Maps locations to the encoded coordinates of any tiles intersecting them. */
    protected transient CoordIntMap _tileCoords = new CoordIntMap(3, EMPTY_COORD);

    /** Collision flags for each location. */
    protected transient CoordIntMap _collisionFlags = new CoordIntMap(3, 0);

    /** Scene entries mapped by key. */
    protected transient HashMap<Object, Entry> _entries = Maps.newHashMap();

    /** The last entry id assigned. */
    protected transient int _lastEntryId;

    /** The set of entry references (used to ensure that entries with equal references use the same
     * instance. */
    protected transient WeakHashMap<ConfigReference, ConfigReference> _references =
        new WeakHashMap<ConfigReference, ConfigReference>();

    /** The space containing the (non-tile) entry shapes. */
    protected transient HashSpace _space = new HashSpace(64f, 6);

    /** Maps entry keys to space elements. */
    protected transient HashMap<Object, SpaceElement> _elements = Maps.newHashMap();

    /** The scene model observers. */
    protected transient ObserverList<Observer> _observers = ObserverList.newFastUnsafe();

    /** The cached exported representation of the scene model. */
    protected transient SoftReference<byte[]> _data;

    /** Region object to reuse. */
    protected transient Rectangle _region = new Rectangle();

    /** Bounds object to reuse. */
    protected transient Rect _rect = new Rect();

    /** Used to store tile shapes for intersecting testing. */
    protected transient Polygon _quad = new Polygon(4);

    /** (Re)used to store intersecting elements. */
    protected transient ArrayList<SpaceElement> _intersecting = new ArrayList<SpaceElement>();

    /** Stores penetration vector during queries. */
    protected transient Vector2f _penetration = new Vector2f();

    /** The value we use to signify an empty coordinate location. */
    protected static final int EMPTY_COORD = Coord.encode(Short.MIN_VALUE, Short.MIN_VALUE);
}
