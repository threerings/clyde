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

import com.threerings.tudey.client.cursor.AreaCursor;
import com.threerings.tudey.client.sprite.AreaSprite;
import com.threerings.tudey.util.TudeyContext;

/**
 * The configuration of an area.
 */
public class AreaConfig extends ParameterizedConfig
{
    /** Used when we can't resolve the area's underlying original implementation. */
    public static final Original NULL_ORIGINAL = new Original();

    /**
     * Contains the actual implementation of the area.
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
         * @param scope the area's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract AreaCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, AreaCursor.Implementation impl);

        /**
         * Creates or updates a sprite implementation for this configuration.
         *
         * @param scope the area's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract AreaSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, AreaSprite.Implementation impl);
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The color to use when showing this path in the scene editor. */
        @Editable(mode="alpha", hgroup="c")
        public Color4f color = new Color4f();

        /** A tag to use to identify the area within the scene. */
        @Editable(hgroup="c")
        public String tag = "";

        /** The area's event handlers. */
        @Editable
        public HandlerConfig[] handlers = new HandlerConfig[0];

        /**
         * Returns the name of the server-side logic class to use for the area, or
         * <code>null</code> for none.
         */
        public String getLogicClassName ()
        {
            return (StringUtil.isBlank(tag) && handlers.length == 0) ? null :
                "com.threerings.tudey.server.logic.EntryLogic";
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        @Override // documentation inherited
        public AreaCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, AreaCursor.Implementation impl)
        {
            if (impl instanceof AreaCursor.Original) {
                ((AreaCursor.Original)impl).setConfig(this);
            } else {
                impl = new AreaCursor.Original(ctx, scope, this);
            }
            return impl;
        }

        @Override // documentation inherited
        public AreaSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, AreaSprite.Implementation impl)
        {
            if (!ScopeUtil.resolve(scope, "markersVisible", false)) {
                return null;
            }
            if (impl instanceof AreaSprite.Original) {
                ((AreaSprite.Original)impl).setConfig(this);
            } else {
                impl = new AreaSprite.Original(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The area reference. */
        @Editable(nullable=true)
        public ConfigReference<AreaConfig> area;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(AreaConfig.class, area);
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            AreaConfig config = cfgmgr.getConfig(AreaConfig.class, area);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }

        @Override // documentation inherited
        public AreaCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, AreaCursor.Implementation impl)
        {
            AreaConfig config = ctx.getConfigManager().getConfig(AreaConfig.class, area);
            return (config == null) ? null : config.getCursorImplementation(ctx, scope, impl);
        }

        @Override // documentation inherited
        public AreaSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, AreaSprite.Implementation impl)
        {
            AreaConfig config = ctx.getConfigManager().getConfig(AreaConfig.class, area);
            return (config == null) ? null : config.getSpriteImplementation(ctx, scope, impl);
        }
    }

    /** The actual area implementation. */
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
     * @param scope the area's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public AreaCursor.Implementation getCursorImplementation (
        TudeyContext ctx, Scope scope, AreaCursor.Implementation impl)
    {
        return implementation.getCursorImplementation(ctx, scope, impl);
    }

    /**
     * Creates or updates a sprite implementation for this configuration.
     *
     * @param scope the area's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public AreaSprite.Implementation getSpriteImplementation (
        TudeyContext ctx, Scope scope, AreaSprite.Implementation impl)
    {
        return implementation.getSpriteImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
