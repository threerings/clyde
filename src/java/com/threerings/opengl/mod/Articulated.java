//
// $Id$

package com.threerings.opengl.mod;

import java.util.Map;

import com.google.common.collect.Maps;

import com.threerings.expr.Scope;
import com.threerings.math.Ray;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mat.Surface;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.config.ArticulatedConfig;
import com.threerings.opengl.util.GlContext;

/**
 * An articulated model implementation.
 */
public class Articulated extends Model.Implementation
{
    /**
     * Creates a new articulated implementation.
     */
    public Articulated (GlContext ctx, Scope parentScope, ArticulatedConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        setConfig(config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (ArticulatedConfig config)
    {
        _config = config;
        Map<String, MaterialConfig> materialConfigs = Maps.newHashMap();
        _surfaces = createSurfaces(
            _ctx, this, config.skin.visible, config.materialMappings, materialConfigs);
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray ray, Vector3f result)
    {
        return false;
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model configuration. */
    protected ArticulatedConfig _config;

    /** The skinned surfaces. */
    protected Surface[] _surfaces;
}
