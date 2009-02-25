//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

package com.threerings.opengl.gui;

import com.samskivert.util.StringUtil;

import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.math.Box;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.MessageManager;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.gui.config.ComponentBillboardConfig;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.AlphaState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.SimpleRenderable;

/**
 * A component billboard model implementation.
 */
public class ComponentBillboard extends Model.Implementation
{
    /**
     * Creates a new billboard implementation.
     */
    public ComponentBillboard (GlContext ctx, Scope parentScope, ComponentBillboardConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        _queue = ctx.getCompositor().getQueue(RenderQueue.TRANSPARENT);

        // create the batch that we will enqueue
        RenderState[] states = RenderState.createDefaultSet();
        states[RenderState.ALPHA_STATE] = AlphaState.PREMULTIPLIED;
        states[RenderState.ARRAY_STATE] = null;
        states[RenderState.COLOR_STATE] = null;
        states[RenderState.DEPTH_STATE] = DepthState.TEST;
        states[RenderState.MATERIAL_STATE] = null;
        states[RenderState.TEXTURE_STATE] = null;
        states[RenderState.TRANSFORM_STATE] = new TransformState();
        _batch = new SimpleBatch(states, new SimpleBatch.DrawCommand() {
            public boolean call () {
                _root.render(_ctx.getRenderer());
                return true;
            }
            public int getPrimitiveCount () {
                return 0;
            }
        });

        setConfig(config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (ComponentBillboardConfig config)
    {
        _config = config;
        updateFromConfig();
    }

    @Override // documentation inherited
    public int getInfluenceFlags ()
    {
        return _influenceFlags;
    }

    @Override // documentation inherited
    public Box getBounds ()
    {
        return _bounds;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        // update the world transform
        if (_parentWorldTransform == null) {
            return;
        }
        _parentWorldTransform.compose(_localTransform, _worldTransform);

        // and the world bounds
        float extent = Math.max(_root.getWidth()/2, _root.getHeight()/2) * _config.scale;
        _nbounds.getMinimumExtent().set(-extent, -extent, -extent);
        _nbounds.getMaximumExtent().set(+extent, +extent, +extent);
        _nbounds.transformLocal(_worldTransform);
        if (!_bounds.equals(_nbounds)) {
            ((Model)_parentScope).boundsWillChange();
            _bounds.set(_nbounds);
            ((Model)_parentScope).boundsDidChange();
        }
    }

    @Override // documentation inherited
    public void drawBounds ()
    {
        DebugBounds.draw(_bounds, Color4f.WHITE);
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        // update the view transform
        TransformState tstate = (TransformState)_batch.getStates()[RenderState.TRANSFORM_STATE];
        Transform3D modelview = tstate.getModelview();
        _parentViewTransform.compose(_localTransform, modelview);
        modelview.getRotation().set(Quaternion.IDENTITY);
        modelview.setScale(modelview.getScale() * _config.scale);
        tstate.setDirty(true);

        // update the depth
        _batch.depth = modelview.transformPointZ(Vector3f.ZERO);

        // enqueue our batch
        _queue.add(_batch);
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        // update the component
        String bundle = StringUtil.isBlank(_config.bundle) ?
            MessageManager.GLOBAL_BUNDLE : _config.bundle;
        _root = _config.root.getComponent(
            _ctx, this, _ctx.getMessageManager().getBundle(bundle), _root);
        Dimension size = _root.getPreferredSize(-1, -1);
        _root.setBounds(-size.width/2, -size.height/2, size.width, size.height);
        _root.validate();
        _root.wasAdded();

        // update the influence flags
        _influenceFlags = _config.influences.getFlags();

        // update the bounds
        updateBounds();
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The queue into which we place our batch. */
    protected RenderQueue _queue;

    /** The batch that we submit to the renderer. */
    protected SimpleBatch _batch;

    /** The model configuration. */
    protected ComponentBillboardConfig _config;

    /** The root component. */
    protected Component _root;

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
    protected Transform3D _worldTransform = new Transform3D();

    /** Flags indicating which influences can affect the model. */
    protected int _influenceFlags;

    /** The bounds of the model. */
    protected Box _bounds = new Box();

    /** Holds the bounds of the model when updating. */
    protected Box _nbounds = new Box();
}
