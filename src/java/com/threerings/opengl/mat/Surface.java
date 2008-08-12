//
// $Id$

package com.threerings.opengl.mat;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.ScopeUpdateListener;
import com.threerings.expr.util.ScopeUtil;

import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.geom.config.GeometryConfig;
import com.threerings.opengl.geom.config.PassDescriptor;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.material.config.TechniqueConfig;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;

/**
 * A renderable surface.
 */
public class Surface
    implements Renderable, ConfigUpdateListener<MaterialConfig>, ScopeUpdateListener
{
    /**
     * Creates a new surface.
     */
    public Surface (
        GlContext ctx, Scope scope, GeometryConfig geometryConfig, MaterialConfig materialConfig)
    {
        this(ctx, scope, geometryConfig, materialConfig, null);
    }

    /**
     * Creates a new surface.
     */
    public Surface (GlContext ctx, Scope scope, Geometry geometry, MaterialConfig materialConfig)
    {
        this(ctx, scope, null, materialConfig, geometry);
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

    // documentation inherited from interface ScopeUpdateListener
    public void scopeUpdated (ScopeEvent event)
    {
        updateFromConfigs();
    }

    /**
     * Creates a new surface.
     */
    protected Surface (
        GlContext ctx, Scope scope, GeometryConfig geometryConfig,
        MaterialConfig materialConfig, Geometry geometry)
    {
        _ctx = ctx;
        _scope = scope;
        _geometryConfig = geometryConfig;
        _materialConfig = materialConfig;
        _geometry = geometry;

        _scope.addListener(this);
        _materialConfig.addListener(this);
        updateFromConfigs();
    }

    /**
     * Updates the surface to match its new or modified configurations.
     */
    protected void updateFromConfigs ()
    {
        String scheme = ScopeUtil.resolve(_scope, "renderScheme", (String)null);
        TechniqueConfig technique = _materialConfig.getTechnique(_ctx, scheme);
        if (_geometryConfig != null) {
            PassDescriptor[] passes = technique.getDescriptors(_ctx);
            _geometry = _geometryConfig.createGeometry(_ctx, _scope, technique.deformer, passes);
        }
        _renderable = technique.createRenderable(_ctx, _scope, _geometry);
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The expression scope. */
    protected Scope _scope;

    /** The configuration of the surface geometry (or null, if the geometry didn't come from a
     * config). */
    protected GeometryConfig _geometryConfig;

    /** The configuration of the surface material. */
    protected MaterialConfig _materialConfig;

    /** The surface geometry. */
    protected Geometry _geometry;

    /** The renderable created from the configs. */
    protected Renderable _renderable;
}
