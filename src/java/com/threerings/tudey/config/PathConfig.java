//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.sprite.PathSprite;

/**
 * The configuration of a path.
 */
public class PathConfig extends ParameterizedConfig
{
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
         * Creates or updates a sprite implementation for this configuration.
         *
         * @param scope the path's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract PathSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, PathSprite.Implementation impl);
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /** The color to use when showing this path in the scene editor. */
        @Editable(mode="alpha")
        public Color4f color = new Color4f();

        @Override // documentation inherited
        public PathSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, PathSprite.Implementation impl)
        {
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
        public PathSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, PathSprite.Implementation impl)
        {
            PathConfig config = ctx.getConfigManager().getConfig(PathConfig.class, path);
            return (config == null) ? null : config.getSpriteImplementation(ctx, scope, impl);
        }
    }

    /** The actual path implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Creates or updates a sprite implementation for this configuration.
     *
     * @param scope the path's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public PathSprite.Implementation getSpriteImplementation (
        GlContext ctx, Scope scope, PathSprite.Implementation impl)
    {
        return implementation.getSpriteImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
