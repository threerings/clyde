//
// $Id$

package com.threerings.opengl.scene.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.math.Box;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.scene.Influencer;
import com.threerings.opengl.util.GlContext;

/**
 * An influencer implementation.
 */
public class InfluencerConfig extends ModelConfig.Implementation
{
    /**
     * Represents the extent of the influence.
     */
    @EditorTypes({ Limited.class, Unlimited.class })
    public static abstract class Extent extends DeepObject
        implements Exportable
    {
        /**
         * Retrieves the bounds of the extent under the specified transform.
         */
        public abstract void transformBounds (Transform3D transform, Box result);
    }

    /**
     * Limited extent.
     */
    public static class Limited extends Extent
    {
        /** The size in the x direction. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float sizeX = 1f;

        /** The size in the y direction. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float sizeY = 1f;

        /** The size in the z direction. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float sizeZ = 1f;

        @Override // documentation inherited
        public void transformBounds (Transform3D transform, Box result)
        {
            result.getMinimumExtent().set(-sizeX, -sizeY, -sizeZ);
            result.getMaximumExtent().set(+sizeX, +sizeY, +sizeZ);
            result.transformLocal(transform);
        }
    }

    /**
     * Unlimited extent.
     */
    public static class Unlimited extends Extent
    {
        @Override // documentation inherited
        public void transformBounds (Transform3D transform, Box result)
        {
            result.set(Box.MAX_VALUE);
        }
    }

    /** The influence that this influencer exerts. */
    @Editable
    public SceneInfluenceConfig influence = new SceneInfluenceConfig.AmbientLight();

    /** The extent of the influence. */
    @Editable
    public Extent extent = new Limited();

    @Override // documentation inherited
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        if (impl instanceof Influencer) {
            ((Influencer)impl).setConfig(this);
        } else {
            impl = new Influencer(ctx, scope, this);
        }
        return impl;
    }
}
