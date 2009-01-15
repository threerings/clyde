//
// $Id$

package com.threerings.tudey.config;

import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.cursor.PathCursor;
import com.threerings.tudey.client.sprite.PathSprite;
import com.threerings.tudey.util.TudeyContext;

/**
 * The configuration of a path.
 */
public class PathConfig extends ParameterizedConfig
{
    /** Used when we can't resolve the path's underlying original implementation. */
    public static final Original NULL_ORIGINAL = new Original();

    /**
     * Contains the actual implementation of the path.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Adds the implementation's update references to the provided set.
         */
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Returns a reference to the config's underlying original implementation.
         */
        public abstract Original getOriginal (ConfigManager cfgmgr);

        /**
         * Creates or updates a cursor implementation for this configuration.
         *
         * @param scope the path's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract PathCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, PathCursor.Implementation impl);

        /**
         * Creates or updates a sprite implementation for this configuration.
         *
         * @param scope the path's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract PathSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, PathSprite.Implementation impl);
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The color to use when showing this path in the scene editor. */
        @Editable(mode="alpha", hgroup="c")
        public Color4f color = new Color4f();

        /** Whether or not the path should be used as a default entrance. */
        @Editable(hgroup="c")
        public boolean defaultEntrance;

        /** Tags used to identify the path within the scene. */
        @Editable
        public String[] tags = new String[0];

        /** The path's event handlers. */
        @Editable
        public HandlerConfig[] handlers = new HandlerConfig[0];

        /**
         * Returns the name of the server-side logic class to use for the path, or
         * <code>null</code> for none.
         */
        public String getLogicClassName ()
        {
            return (tags.length == 0 && handlers.length == 0 && !defaultEntrance) ? null :
                "com.threerings.tudey.server.logic.EntryLogic";
        }

        /**
         * Adds the resources to preload for this path into the provided set.
         */
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            for (HandlerConfig handler : handlers) {
                handler.action.getPreloads(cfgmgr, preloads);
            }
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        @Override // documentation inherited
        public PathCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, PathCursor.Implementation impl)
        {
            if (impl instanceof PathCursor.Original) {
                ((PathCursor.Original)impl).setConfig(this);
            } else {
                impl = new PathCursor.Original(ctx, scope, this);
            }
            return impl;
        }

        @Override // documentation inherited
        public PathSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, PathSprite.Implementation impl)
        {
            if (!ScopeUtil.resolve(scope, "markersVisible", false)) {
                return null;
            }
            if (impl instanceof PathSprite.Original) {
                ((PathSprite.Original)impl).setConfig(this);
            } else {
                impl = new PathSprite.Original(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The path reference. */
        @Editable(nullable=true)
        public ConfigReference<PathConfig> path;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(PathConfig.class, path);
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            PathConfig config = cfgmgr.getConfig(PathConfig.class, path);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }

        @Override // documentation inherited
        public PathCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, PathCursor.Implementation impl)
        {
            PathConfig config = ctx.getConfigManager().getConfig(PathConfig.class, path);
            return (config == null) ? null : config.getCursorImplementation(ctx, scope, impl);
        }

        @Override // documentation inherited
        public PathSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, PathSprite.Implementation impl)
        {
            PathConfig config = ctx.getConfigManager().getConfig(PathConfig.class, path);
            return (config == null) ? null : config.getSpriteImplementation(ctx, scope, impl);
        }
    }

    /** The actual path implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Returns a reference to the config's underlying original implementation.
     */
    public Original getOriginal (ConfigManager cfgmgr)
    {
        return implementation.getOriginal(cfgmgr);
    }

    /**
     * Creates or updates a cursor implementation for this configuration.
     *
     * @param scope the path's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public PathCursor.Implementation getCursorImplementation (
        TudeyContext ctx, Scope scope, PathCursor.Implementation impl)
    {
        return implementation.getCursorImplementation(ctx, scope, impl);
    }

    /**
     * Creates or updates a sprite implementation for this configuration.
     *
     * @param scope the path's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public PathSprite.Implementation getSpriteImplementation (
        TudeyContext ctx, Scope scope, PathSprite.Implementation impl)
    {
        return implementation.getSpriteImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
