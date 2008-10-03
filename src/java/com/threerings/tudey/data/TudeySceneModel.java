//
// $Id$

package com.threerings.tudey.data;

import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;

import com.samskivert.util.ObserverList;

import com.threerings.whirled.data.SceneModel;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepUtil;

import com.threerings.opengl.gui.util.Point;
import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.sprite.AreaSprite;
import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.GlobalSprite;
import com.threerings.tudey.client.sprite.PathSprite;
import com.threerings.tudey.client.sprite.PlaceableSprite;
import com.threerings.tudey.client.sprite.TileSprite;
import com.threerings.tudey.config.AreaConfig;
import com.threerings.tudey.config.PathConfig;
import com.threerings.tudey.config.PlaceableConfig;
import com.threerings.tudey.config.SceneGlobalConfig;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.util.TudeySceneMetrics;

import static com.threerings.tudey.Log.*;

/**
 * Contains a representation of a Tudey scene.
 */
public class TudeySceneModel extends SceneModel
    implements Exportable
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
         * Creates a sprite for this entry.
         */
        public abstract EntrySprite createSprite (GlContext ctx, TudeySceneView view);
    }

    /**
     * A tile entry.  Tiles are identified by their locations.
     */
    public static class TileEntry extends Entry
    {
        /** The configuration of the tile. */
        @Editable(nullable=false)
        public ConfigReference<TileConfig> tile;

        /** The tile's elevation. */
        @Editable(min=Byte.MIN_VALUE, max=Byte.MAX_VALUE)
        public int elevation;

        /** The tile's rotation. */
        @Editable(min=0, max=3)
        public int rotation;

        /**
         * Returns a reference to the tile's location.
         */
        public Point getLocation ()
        {
            return _location;
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

        @Override // documentation inherited
        public Object getKey ()
        {
            return _location;
        }

        @Override // documentation inherited
        public EntrySprite createSprite (GlContext ctx, TudeySceneView view)
        {
            return new TileSprite(ctx, view, this);
        }

        /** The location of the tile. */
        protected Point _location = new Point();
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
        @Editable(nullable=false)
        public ConfigReference<SceneGlobalConfig> sceneGlobal;

        @Override // documentation inherited
        public EntrySprite createSprite (GlContext ctx, TudeySceneView view)
        {
            return new GlobalSprite(ctx, view, this);
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
        public EntrySprite createSprite (GlContext ctx, TudeySceneView view)
        {
            return new PlaceableSprite(ctx, view, this);
        }
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
        @Editable(editor="table")
        public Vertex[] vertices = new Vertex[0];

        @Override // documentation inherited
        public EntrySprite createSprite (GlContext ctx, TudeySceneView view)
        {
            return new PathSprite(ctx, view, this);
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
        @Editable(editor="table")
        public Vertex[] vertices = new Vertex[0];

        @Override // documentation inherited
        public EntrySprite createSprite (GlContext ctx, TudeySceneView view)
        {
            return new AreaSprite(ctx, view, this);
        }
    }

    /**
     * Represents a single vertex in a path or area.
     */
    public static class Vertex extends DeepObject
        implements Exportable
    {
        /** The vertex coordinates. */
        @Editable(column=true)
        public float x, y;
    }

    /**
     * Initializes the model.
     */
    public void init (ConfigManager cfgmgr)
    {
        _cfgmgr.init("scene", cfgmgr);
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
     * Adds an entry to the scene, possibly assigning it a unique id in the process.
     *
     * @return true if the entry was successfully added, false if there was already
     * an entry with the same id (in which case a warning will be logged).
     */
    public boolean addEntry (final Entry entry)
    {
        // assign id if appropriate
        if (entry instanceof IdEntry) {
            ((IdEntry)entry).setId(++_lastEntryId);
        }
        // add to map
        Entry oentry = _entries.put(entry.getKey(), entry);
        if (oentry != null) {
            log.warning("Attempted to replace existing entry.", "oentry", oentry, "nentry", entry);
            _entries.put(entry.getKey(), oentry);
            return false;
        }
        // notify the observers
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
        final Entry oentry = _entries.put(nentry.getKey(), nentry);
        if (oentry == null) {
            log.warning("Attempted to update nonexistent entry.", "entry", nentry);
            _entries.remove(nentry.getKey());
            return null;
        }
        // notify the observers
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
        final Entry oentry = _entries.remove(key);
        if (oentry == null) {
            log.warning("Missing entry to remove.", "key", key);
            return null;
        }
        // notify the observers
        _observers.apply(new ObserverList.ObserverOp<Observer>() {
            public boolean apply (Observer observer) {
                observer.entryRemoved(oentry);
                return true;
            }
        });
        return oentry;
    }

    /**
     * Looks up the entry with the supplied key.
     *
     * @return a reference to the identified entry, or <code>null</code> if not found.
     */
    public Entry getEntry (Object key)
    {
        return _entries.get(key);
    }

    /**
     * Returns a reference to the collection of entries.
     */
    public Collection<Entry> getEntries ()
    {
        return _entries.values();
    }

    /**
     * Custom field write method.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        out.defaultWriteFields();
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
        for (Entry entry : in.read("entries", new Entry[0], Entry[].class)) {
            _entries.put(entry.getKey(), entry);
        }
    }

    @Override // documentation inherited
    public Object clone ()
    {
        return DeepUtil.copy(this, null);
    }

    /** The scene configuration manager. */
    protected ConfigManager _cfgmgr = new ConfigManager();

    /** The last entry id assigned. */
    protected int _lastEntryId;

    /** Scene entries mapped by key. */
    protected transient HashMap<Object, Entry> _entries = new HashMap<Object, Entry>();

    /** The scene model observers. */
    protected transient ObserverList<Observer> _observers = ObserverList.newFastUnsafe();
}
