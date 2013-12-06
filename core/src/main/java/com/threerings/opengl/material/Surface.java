//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl.material;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Matrix4f;

import com.threerings.opengl.compositor.Compositable;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.geometry.Geometry;
import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.material.config.PassConfig;
import com.threerings.opengl.material.config.TechniqueConfig;
import com.threerings.opengl.material.config.TechniqueConfig.NormalEnqueuer;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.log;

/**
 * A renderable surface.
 */
public class Surface extends SimpleScope
    implements Compositable, ConfigUpdateListener<MaterialConfig>
{
    /**
     * Creates a new surface.
     */
    public Surface (
        GlContext ctx, Scope parentScope, GeometryConfig geometryConfig,
        MaterialConfig materialConfig)
    {
        this(ctx, parentScope, geometryConfig, materialConfig, ctx.getCompositor().getGroup());
    }

    /**
     * Creates a new surface.
     */
    public Surface (
        GlContext ctx, Scope parentScope, GeometryConfig geometryConfig,
        MaterialConfig materialConfig, RenderQueue.Group group)
    {
        this(ctx, parentScope, geometryConfig, materialConfig, null, group);
    }

    /**
     * Creates a new surface.
     */
    public Surface (
        GlContext ctx, Scope parentScope, Geometry geometry, MaterialConfig materialConfig)
    {
        this(ctx, parentScope, geometry, materialConfig, ctx.getCompositor().getGroup());
    }

    /**
     * Creates a new surface.
     */
    public Surface (
        GlContext ctx, Scope parentScope, Geometry geometry,
        MaterialConfig materialConfig, RenderQueue.Group group)
    {
        this(ctx, parentScope, null, materialConfig, geometry, group);
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
        updateFromConfigs();
    }

    /**
     * Returns a reference to this surface's material configuration.
     */
    public MaterialConfig getMaterialConfig ()
    {
        return _materialConfig;
    }

    // documentation inherited from interface Compositable
    public void composite ()
    {
        _compositable.composite();
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<MaterialConfig> event)
    {
        updateFromConfigs();
    }

    @Override
    public String getScopeName ()
    {
        return "surface";
    }

    @Override
    public void dispose ()
    {
        super.dispose();
        if (_materialConfig != null) {
            _materialConfig.removeListener(this);
        }
    }

    /**
     * Creates a new surface.
     */
    protected Surface (
        GlContext ctx, Scope parentScope, GeometryConfig geometryConfig,
        MaterialConfig materialConfig, Geometry geometry, RenderQueue.Group group)
    {
        super(parentScope);
        _ctx = ctx;
        _geometryConfig = geometryConfig;
        _geometry = geometry;
        _group = group;

        setMaterialConfig(materialConfig);
    }

    /**
     * Updates the surface to match its new or modified configurations.
     */
    protected void updateFromConfigs ()
    {
        String scheme = ScopeUtil.resolve(_parentScope, "renderScheme", (String)null);
        TechniqueConfig technique = (_materialConfig == null) ?
            BLANK_TECHNIQUE : _materialConfig.getTechnique(_ctx, scheme);
        if (technique == null) {
            log.warning("No technique available to render material.",
                "material", _materialConfig.getReference(), "scheme", scheme);
            technique = BLANK_TECHNIQUE;
        }
        if (technique.receivesProjections) {
            Projection[] projections = ScopeUtil.resolve(
                _parentScope, "projections", null, Projection[].class);
            if (projections != null && projections.length > 0) {
                technique = Projection.rewrite(technique, projections);
            }
        }
        if (_geometryConfig != null) {
            PassDescriptor[] passes = technique.getDescriptors(_ctx);
            _geometry = _geometryConfig.createGeometry(_ctx, this, technique.deformer, passes);
        }
        _boneMatrices = _geometry.getBoneMatrices();
        _compositable = technique.createCompositable(_ctx, this, _geometry, _group);
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

    /** The group into which we enqueue our batches. */
    protected RenderQueue.Group _group;

    /** The compositable created from the configs. */
    protected Compositable _compositable;

    /** A technique that renders the material as blank. */
    protected static final TechniqueConfig BLANK_TECHNIQUE = new TechniqueConfig();
    static {
        ((NormalEnqueuer)BLANK_TECHNIQUE.enqueuer).passes = new PassConfig[] { new PassConfig() };
    }
}
