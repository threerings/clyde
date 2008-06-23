//
// $Id$

package com.threerings.opengl.material;

import com.threerings.math.Vector3f;

import com.threerings.opengl.geometry.Geometry;
import com.threerings.opengl.renderer.Batch;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.CompoundBatch;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.AlphaState;
import com.threerings.opengl.renderer.state.ColorMaskState;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.CullState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.FogState;
import com.threerings.opengl.renderer.state.LightState;
import com.threerings.opengl.renderer.state.MaterialState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.util.GlContext;

/**
 * A surface created by a {@link DefaultMaterial}.
 */
public class DefaultSurface extends Surface
{
    public DefaultSurface (GlContext ctx, DefaultMaterial material, Geometry geom)
    {
        _ctx = ctx;
        _material = material;

        // start with the geometry batch
        _bbatch = createBaseBatch(geom);

        // add all the shared states
        RenderState[] states = _bbatch.getStates();
        RenderState.copy(material.getSharedStates(), states);

        // enable or disable culling for polygonal geometry
        if (geom.isPolygonal()) {
            states[RenderState.CULL_STATE] =
                geom.isSolid() ? CullState.BACK_FACE : CullState.DISABLED;
        }

        // set the batch's state key
        _bbatch.updateKey();

        // find the batch's center in mesh space
        _center = geom.getBounds().getCenter();
    }

    /**
     * Returns a reference to the base batch.
     */
    public SimpleBatch getBaseBatch ()
    {
        return _bbatch;
    }

    @Override // documentation inherited
    public void setHost (SurfaceHost host)
    {
        // copy the shared transform state
        _host = host;
        RenderState[] states = _bbatch.getStates();
        states[RenderState.TRANSFORM_STATE] = host.getTransformState();
        update();
    }

    @Override // documentation inherited
    public void update ()
    {
        RenderState[] states = _bbatch.getStates();
        LightState lstate = _material.isEmissive() ? LightState.DISABLED : _host.getLightState();
        states[RenderState.LIGHT_STATE] = lstate;
        float alpha;
        if (lstate.getLights() == null) {
            ColorState cstate = _host.getColorState();
            alpha = cstate.getColor().a;
            states[RenderState.COLOR_STATE] = ColorState.getInstance(cstate);
            states[RenderState.MATERIAL_STATE] = null;
        } else {
            MaterialState mstate = _host.getMaterialState();
            alpha = mstate.getFrontDiffuse().a;
            states[RenderState.COLOR_STATE] = null;
            states[RenderState.MATERIAL_STATE] = MaterialState.getInstance(mstate);
        }
        states[RenderState.FOG_STATE] = _host.getFogState();
        if (alpha == 1f) {
            _batch = _bbatch;
            _transparent = false;
        } else {
            if (_tbatch == null) {
                _tbatch = createTranslucentBatch();
            }
            RenderState[] sstates = ((SimpleBatch)_tbatch.getBatches().get(1)).getStates();
            sstates[RenderState.COLOR_STATE] = states[RenderState.COLOR_STATE];
            sstates[RenderState.MATERIAL_STATE] = states[RenderState.MATERIAL_STATE];
            sstates[RenderState.LIGHT_STATE] = states[RenderState.LIGHT_STATE];
            sstates[RenderState.FOG_STATE] = states[RenderState.FOG_STATE];
            _batch = _tbatch;
            _transparent = true;
        }
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        _batch.depth = _host.getModelview().transformPointZ(_center);
        if (_transparent) {
            _ctx.getRenderer().enqueueTransparent(_batch);
        } else {
            _ctx.getRenderer().enqueueOpaque(_batch);
        }
    }

    @Override // documentation inherited
    public Object clone ()
    {
        // make a clone of the batch
        DefaultSurface osurface = (DefaultSurface)super.clone();
        osurface._bbatch = (SimpleBatch)_bbatch.clone();
        return osurface;
    }

    /**
     * Creates the batch to render the geometry.
     */
    protected SimpleBatch createBaseBatch (Geometry geom)
    {
        return geom.createBatch(_ctx.getRenderer(), false);
    }

    /**
     * Creates a batch to render the geometry in two passes: first to the z buffer, then to
     * the color buffer.
     */
    protected CompoundBatch createTranslucentBatch ()
    {
        SimpleBatch first = (SimpleBatch)_bbatch.clone();
        RenderState[] states = first.getStates();
        states[RenderState.LIGHT_STATE] = LightState.DISABLED;
        states[RenderState.FOG_STATE] = FogState.DISABLED;
        states[RenderState.COLOR_STATE] = null;
        states[RenderState.MATERIAL_STATE] = null;
        states[RenderState.COLOR_MASK_STATE] = ColorMaskState.NONE;

        SimpleBatch second = (SimpleBatch)_bbatch.clone();
        states = second.getStates();
        states[RenderState.ALPHA_STATE] = AlphaState.PREMULTIPLIED;
        states[RenderState.DEPTH_STATE] = DepthState.TEST;

        return new CompoundBatch(first, second);
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** The parent material. */
    protected DefaultMaterial _material;

    /** Whether or not the mesh is to be rendered as transparent. */
    protected boolean _transparent;

    /** The base batch, which contains the geometry. */
    protected SimpleBatch _bbatch;

    /** The compound batch used for two-pass transparency. */
    protected CompoundBatch _tbatch;

    /** The batch to enqueue. */
    protected Batch _batch;

    /** The surface host. */
    protected SurfaceHost _host;

    /** The center of the batch in mesh space. */
    protected Vector3f _center;
}
