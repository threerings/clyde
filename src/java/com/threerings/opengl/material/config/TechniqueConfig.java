//
// $Id$

package com.threerings.opengl.material.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.compositor.config.RenderSchemeConfig;
import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.geom.config.PassDescriptor;
import com.threerings.opengl.renderer.CompoundBatch;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;

/**
 * Represents a single technique for rendering a material.
 */
public class TechniqueConfig extends DeepObject
    implements Exportable
{
    /** The scheme with which this technique is associated. */
    @Editable(editor="config", mode="render_scheme", nullable=true)
    public String scheme;

    /** The queue into which we place the batch. */
    @Editable(editor="config", mode="render_queue", nullable=true)
    public String queue = RenderQueue.OPAQUE;

    /** The passes used to render the material. */
    @Editable
    public PassConfig[] passes = new PassConfig[] { new PassConfig() };

    /**
     * Processes this technique to accommodate the features of the hardware.
     *
     * @return the processed technique, or <code>null</code> if the technique is not supported.
     */
    public TechniqueConfig process (GlContext ctx)
    {
        // for now, we don't do any actual processing; we just check for support
        return isSupported(ctx) ? this : null;
    }

    /**
     * Determines whether this technique is supported.
     */
    public boolean isSupported (GlContext ctx)
    {
        for (PassConfig pass : passes) {
            if (!pass.isSupported(ctx)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the cached configuration for the technique's render scheme.
     */
    public RenderSchemeConfig getSchemeConfig (GlContext ctx)
    {
        if (_schemeConfig == INVALID_SCHEME_CONFIG) {
            return (scheme == null) ? null :
                ctx.getConfigManager().getConfig(RenderSchemeConfig.class, scheme);
        }
        return _schemeConfig;
    }

    /**
     * Creates the descriptors for this technique's passes.
     */
    public PassDescriptor[] createDescriptors (GlContext ctx)
    {
        PassDescriptor[] descriptors = new PassDescriptor[passes.length];
        for (int ii = 0; ii < passes.length; ii++) {
            descriptors[ii] = passes[ii].createDescriptor(ctx);
        }
        return descriptors;
    }

    /**
     * Creates a renderable to render the supplied geometry using this technique.
     */
    public Renderable createRenderable (GlContext ctx, Scope scope, Geometry geometry)
    {
        return null;
    }

    /**
     * Invalidates any cached state for this config.
     */
    public void invalidate ()
    {
        _schemeConfig = INVALID_SCHEME_CONFIG;
    }

    /**
     * Creates a compound batch to render all passes in order.
     */
    protected CompoundBatch createBatch (GlContext ctx, Geometry geometry)
    {
        CompoundBatch batch = new CompoundBatch();
        for (int ii = 0; ii < passes.length; ii++) {
            batch.getBatches().add(createBatch(ctx, geometry, ii));
        }
        return batch;
    }

    /**
     * Creates a simple batch.
     */
    protected SimpleBatch createBatch (GlContext ctx, Geometry geometry, int pass)
    {
        RenderState[] states = passes[pass].createStates(ctx);
        states[RenderState.ARRAY_STATE] = geometry.getArrayState(pass);
        return new SimpleBatch(states, geometry.getDrawCommand(pass));
    }

    /** The cached scheme config. */
    @DeepOmit
    protected transient RenderSchemeConfig _schemeConfig = INVALID_SCHEME_CONFIG;

    /** An invalid render scheme config. */
    protected static final RenderSchemeConfig INVALID_SCHEME_CONFIG = new RenderSchemeConfig();
}
