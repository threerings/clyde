//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.PlaceableConfig;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.PlaceableEntry;

/**
 * Represents a placeable entry.
 */
public class PlaceableSprite extends EntrySprite
    implements ConfigUpdateListener<PlaceableConfig>
{
    /**
     * The actual sprite implementation.
     */
    public static abstract class Implementation extends SimpleScope
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (GlContext ctx, Scope parentScope)
        {
            super(parentScope);
            _ctx = ctx;
        }

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "impl";
        }

        /** The renderer context. */
        protected GlContext _ctx;
    }

    /**
     * A prop implementation.
     */
    public static class Prop extends Implementation
    {
        /**
         * Creates a new prop implementation.
         */
        public Prop (GlContext ctx, Scope parentScope, PlaceableConfig.Prop config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (PlaceableConfig.Prop config)
        {
        }
    }

    /**
     * A marker implementation.
     */
    public static class Marker extends Implementation
    {
        /**
         * Creates a new marker implementation.
         */
        public Marker (GlContext ctx, Scope parentScope, PlaceableConfig.Marker config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (PlaceableConfig.Marker config)
        {
        }
    }

    /**
     * Creates a new placeable sprite.
     */
    public PlaceableSprite (GlContext ctx, TudeySceneView view, PlaceableEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<PlaceableConfig> event)
    {
        updateFromEntry();
    }

    @Override // documentation inherited
    public void update (Entry entry)
    {
        _entry = (PlaceableEntry)entry;
        updateFromEntry();
    }

    @Override // documentation inherited
    public void dispose ()
    {
        super.dispose();
    }

    /**
     * Updates the state of the sprite from its entry.
     */
    protected void updateFromEntry ()
    {
    }

    /** The scene entry. */
    protected PlaceableEntry _entry;
}
