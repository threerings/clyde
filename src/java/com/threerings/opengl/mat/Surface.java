//
// $Id$

package com.threerings.opengl.mat;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Matrix4f;

import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.material.config.TechniqueConfig;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;

/**
 * A renderable surface.
 */
public class Surface extends SimpleScope
    implements Renderable, ConfigUpdateListener<MaterialConfig>
{
    /**
     * Creates a new surface.
     */
    public Surface (
        GlContext ctx, Scope parentScope, GeometryConfig geometryConfig,
        MaterialConfig materialConfig)
    {
        this(ctx, parentScope, geometryConfig, materialConfig, null);
    }

    /**
     * Creates a new surface.
     */
    public Surface (
        GlContext ctx, Scope parentScope, Geometry geometry, MaterialConfig materialConfig)
    {
        this(ctx, parentScope, null, materialConfig, geometry);
    }

    /**
     * Sets the material configuration of this surface.
     */
    public void setMaterialConfig (MaterialConfig config)
    {
        _materialConfig.removeListener(this);
        (_materialConfig = config).addListener(this);
        updateFromConfigs();
    }

    /**
     * Returns a reference to this surface's material configuration.
     */
    public MaterialConfig getMaterialConfig ()
    {
        return _materialConfig;
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        _renderable.enqueue();
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<MaterialConfig> event)
    {
        updateFromConfigs();
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "surface";
    }

    @Override // documentation inherited
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        updateFromConfigs();
    }

    @Override // documentation inherited
    public void dispose ()
    {
        super.dispose();
        _materialConfig.removeListener(this);
    }

    /**
     * Creates a new surface.
     */
    protected Surface (
        GlContext ctx, Scope parentScope, GeometryConfig geometryConfig,
        MaterialConfig materialConfig, Geometry geometry)
    {
        super(parentScope);
        _ctx = ctx;
        _geometryConfig = geometryConfig;
        _materialConfig = materialConfig;
        _geometry = geometry;

        _materialConfig.addListener(this);
        updateFromConfigs();
    }

    /**
     * Updates the surface to match its new or modified configurations.
     */
    protected void updateFromConfigs ()
    {
        String scheme = ScopeUtil.resolve(_parentScope, "renderScheme", (String)null);
        TechniqueConfig technique = _materialConfig.getTechnique(_ctx, scheme);
        if (_geometryConfig != null) {
            PassDescriptor[] passes = technique.getDescriptors(_ctx);
            _geometry = _geometryConfig.createGeometry(_ctx, this, technique.deformer, passes);
        }
        _boneMatrices = _geometry.getBoneMatrices();
        _renderable = technique.createRenderable(_ctx, this, _geometry);
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The configuration of the surface geometry (or null, if the geometry didn't come from a
     * config). */
    protected GeometryConfig _geometryConfig;

    /** The configuration of the surface material. */
    protected MaterialConfig _materialConfig;

    /** The bone matrices, if any. */
    @Scoped
    protected Matrix4f[] _boneMatrices;

    /** The surface geometry. */
    protected Geometry _geometry;

    /** The renderable created from the configs. */
    protected Renderable _renderable;
}
