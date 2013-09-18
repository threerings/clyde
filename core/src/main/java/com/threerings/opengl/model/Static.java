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

package com.threerings.opengl.model;

import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.math.Box;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.compositor.Enqueueable;
import com.threerings.opengl.material.Surface;
import com.threerings.opengl.model.config.StaticConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlContext;

/**
 * A static model implementation.
 */
public class Static extends Model.Implementation
    implements Enqueueable
{
    /**
     * Creates a new static implementation.
     */
    public Static (GlContext ctx, Scope parentScope, StaticConfig.Resolved config)
    {
        super(parentScope);
        setConfig(ctx, config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (GlContext ctx, StaticConfig.Resolved config)
    {
        _ctx = ctx;
        _config = config;
        updateFromConfig();
    }

    // documentation inherited from interface Enqueueable
    public void enqueue ()
    {
        // update the shared transform state
        Transform3D modelview = _transformState.getModelview();
        _parentViewTransform.compose(_localTransform, modelview);
        _transformState.setDirty(true);
    }

    @Override
    public int getInfluenceFlags ()
    {
        return _config.influenceFlags;
    }

    @Override
    public Box getBounds ()
    {
        return _bounds;
    }

    @Override
    public void updateBounds ()
    {
        // update the world transform
        if (_parentWorldTransform == null) {
            _worldTransform.set(_localTransform);
        } else {
            _parentWorldTransform.compose(_localTransform, _worldTransform);
        }

        // and the world bounds
        _config.bounds.transform(_worldTransform, _nbounds);
        if (!_bounds.equals(_nbounds)) {
            ((Model)_parentScope).boundsWillChange(this);
            _bounds.set(_nbounds);
            ((Model)_parentScope).boundsDidChange(this);
        }
    }

    @Override
    public void drawBounds ()
    {
        DebugBounds.draw(_bounds, Color4f.WHITE);
    }

    @Override
    public void dumpInfo (String prefix)
    {
        System.out.println(prefix + "Static: " + _worldTransform + " " + _bounds);
    }

    @Override
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        // we must transform the ray into model space before checking against the collision mesh
        if (_config.collision == null || !_bounds.intersects(ray) ||
                !_config.collision.getIntersection(ray.transform(
                    _worldTransform.invert()), result)) {
            return false;
        }
        // then transform it back if we get a hit
        _worldTransform.transformPointLocal(result);
        return true;
    }

    @Override
    public void composite ()
    {
        // add an enqueueable to initialize the shared state
        _ctx.getCompositor().addEnqueueable(this);

        // composite the surfaces
        for (Surface surface : _surfaces) {
            surface.composite();
        }
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        if (_surfaces != null) {
            for (Surface surface : _surfaces) {
                surface.dispose();
            }
        }
        _surfaces = createSurfaces(_ctx, this, _config.gmats);
        updateBounds();
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model configuration. */
    protected StaticConfig.Resolved _config;

    /** The surfaces corresponding to each visible mesh. */
    protected Surface[] _surfaces;

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The parent view transform. */
    @Bound("viewTransform")
    protected Transform3D _parentViewTransform;

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The world transform. */
    @Scoped
    protected Transform3D _worldTransform = new Transform3D();

    /** The shared transform state. */
    @Scoped
    protected TransformState _transformState = new TransformState();

    /** The bounds of the model. */
    @Scoped
    protected Box _bounds = new Box();

    /** Holds the new bounds of the model when updating. */
    protected Box _nbounds = new Box();
}
