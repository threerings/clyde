//
// $Id$

package com.threerings.opengl.mat;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.ScopeUpdateListener;

import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.geom.config.GeometryConfig;
import com.threerings.opengl.material.config.MaterialConfig;
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
        _ctx = ctx;
        _scope = scope;
        _geometryConfig = geometryConfig;
        setMaterialConfig(materialConfig);
    }

    /**
     * Sets the material configuration of this surface.
     */
    public void setMaterialConfig (MaterialConfig config)
    {
        if (_materialConfig != null) {
            _materialConfig.removeListener(this);
        }
        if ((_materialConfig = config) != null) {
            _materialConfig.addListener(this);
        }
        updateFromConfig();
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
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<MaterialConfig> event)
    {
        updateFromConfig();
    }

    // documentation inherited from interface ScopeUpdateListener
    public void scopeUpdated (ScopeEvent event)
    {
    }

    /**
     * Updates the surface to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The expression scope. */
    protected Scope _scope;

    /** The configuration of the surface geometry. */
    protected GeometryConfig _geometryConfig;

    /** The configuration of the surface material. */
    protected MaterialConfig _materialConfig;

    /** The surface geometry. */
    protected Geometry _geometry;
}
