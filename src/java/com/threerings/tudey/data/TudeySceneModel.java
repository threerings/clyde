//
// $Id$

package com.threerings.tudey.data;

import java.io.IOException;

import java.util.Collection;

import com.samskivert.util.HashIntMap;

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

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.GlobalSprite;
import com.threerings.tudey.client.sprite.PlaceableSprite;
import com.threerings.tudey.config.PlaceableConfig;
import com.threerings.tudey.config.SceneGlobalConfig;

/**
 * Contains a representation of a Tudey scene.
 */
public class TudeySceneModel extends SceneModel
    implements Exportable
{
    /**
     * An entry in the scene.
     */
    public static abstract class Entry extends DeepObject
        implements Exportable, Comparable<Entry>
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

        /**
         * Creates a sprite for this entry.
         */
        public abstract EntrySprite createSprite (GlContext ctx, TudeySceneView view);

        // documentation inherited from interface Comparable
        public int compareTo (Entry other)
        {
            return _id - other._id;
        }

        /** The entry's unique identifier. */
        protected int _id;
    }

    /**
     * A global entry.
     */
    public static class GlobalEntry extends Entry
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
    public static class PlaceableEntry extends Entry
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
     * Adds an entry to the scene, assigning it a unique id in the process.
     */
    public void addEntry (Entry entry)
    {
        entry.setId(++_lastEntryId);
        _entries.put(_lastEntryId, entry);
    }

    /**
     * Updates an entry within the scene.
     *
     * @return a reference to the entry that was replaced, or <code>null</code> for none.
     */
    public Entry updateEntry (Entry entry)
    {
        return _entries.put(entry.getId(), entry);
    }

    /**
     * Removes an entry from the scene.
     *
     * @return a reference to the entry that was remove, or <code>null</code> for none.
     */
    public Entry removeEntry (int id)
    {
        return _entries.remove(id);
    }

    /**
     * Looks up the entry with the supplied id.
     *
     * @return a reference to the identified entry, or <code>null</code> if not found.
     */
    public Entry getEntry (int id)
    {
        return _entries.get(id);
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
            _entries.put(entry.getId(), entry);
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

    /** Scene entries mapped by id. */
    protected transient HashIntMap<Entry> _entries = new HashIntMap<Entry>();
}
