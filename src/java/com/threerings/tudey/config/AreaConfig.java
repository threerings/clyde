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

import com.threerings.tudey.client.sprite.AreaSprite;

/**
 * The configuration of an area.
 */
public class AreaConfig extends ParameterizedConfig
{
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
         * Creates or updates a sprite implementation for this configuration.
         *
         * @param scope the area's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract AreaSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, AreaSprite.Implementation impl);
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
        public AreaSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, AreaSprite.Implementation impl)
        {
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
        public AreaSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, AreaSprite.Implementation impl)
        {
            AreaConfig config = ctx.getConfigManager().getConfig(AreaConfig.class, area);
            return (config == null) ? null : config.getSpriteImplementation(ctx, scope, impl);
        }
    }

    /** The actual area implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Creates or updates a sprite implementation for this configuration.
     *
     * @param scope the area's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public AreaSprite.Implementation getSpriteImplementation (
        GlContext ctx, Scope scope, AreaSprite.Implementation impl)
    {
        return implementation.getSpriteImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
