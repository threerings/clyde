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

package com.threerings.tudey.shape;

import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.math.Box;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.AlphaState;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.util.ShapeConfigElement;
import com.threerings.tudey.shape.config.ShapeModelConfig;

/**
 * Shape model implementation.
 */
public class ShapeModel extends Model.Implementation
{
    /**
     * Creates a new shape model implementation.
     */
    public ShapeModel (GlContext ctx, Scope parentScope, ShapeModelConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        _element = new ShapeConfigElement(ctx) { {
                _queue = _ctx.getCompositor().getQueue(RenderQueue.TRANSPARENT);
                _batch.getStates()[RenderState.ALPHA_STATE] = AlphaState.PREMULTIPLIED;
                _batch.getStates()[RenderState.DEPTH_STATE] = DepthState.TEST;
            }
            @Override public void enqueue () {
                TransformState tstate =
                    (TransformState)_batch.getStates()[RenderState.TRANSFORM_STATE];
                Transform3D modelview = tstate.getModelview();
                _parentViewTransform.compose(_localTransform, modelview);
                tstate.setDirty(true);

                _batch.depth = modelview.transformPointZ(getCenter());
                _queue.add(_batch, _priority);
            }
            @Override protected void boundsWillChange () {
                ((Model)_parentScope).boundsWillChange(ShapeModel.this);
            }
            @Override protected void boundsDidChange () {
                ((Model)_parentScope).boundsDidChange(ShapeModel.this);
            }
        };
        setConfig(ctx, config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (GlContext ctx, ShapeModelConfig config)
    {
        _ctx = ctx;
        _config = config;
        updateFromConfig();
    }

    @Override
    public Box getBounds ()
    {
        return _element.getBounds();
    }

    @Override
    public void updateBounds ()
    {
        // update the world transform
        if (_parentWorldTransform == null) {
            _element.getTransform().set(_localTransform);
        } else {
            _parentWorldTransform.compose(_localTransform, _element.getTransform());
        }

        // and the world bounds
        _element.updateBounds();
    }

    @Override
    public void drawBounds ()
    {
        DebugBounds.draw(_element.getBounds(), Color4f.WHITE);
    }

    @Override
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        return _element.getIntersection(ray, result);
    }

    @Override
    public void composite ()
    {
        _element.composite();
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        _element.setConfig(_config.shape, _config.outline);
        _element.getStates()[RenderState.COLOR_STATE] =
            (_colorState == null) ? ColorState.WHITE : _colorState;
        updateBounds();
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model config. */
    protected ShapeModelConfig _config;

    /** The element used to render the shape. */
    protected ShapeConfigElement _element;

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The parent view transform. */
    @Bound("viewTransform")
    protected Transform3D _parentViewTransform;

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The model's color state. */
    @Bound
    protected ColorState _colorState;
}
