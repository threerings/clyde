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

package com.threerings.opengl.gui;

import com.samskivert.util.StringUtil;

import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.Updater;
import com.threerings.math.Box;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.MessageManager;

import com.threerings.opengl.compositor.Enqueueable;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.gui.config.ComponentBillboardConfig;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.ArticulatedConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlContext;

/**
 * A component billboard model implementation.
 */
public class ComponentBillboard extends Model.Implementation
    implements Enqueueable
{
    /**
     * Creates a new billboard implementation.
     */
    public ComponentBillboard (GlContext ctx, Scope parentScope, ComponentBillboardConfig config)
    {
        super(parentScope);
        _ctx = ctx;

        // create the batch that we will enqueue
        RenderState[] states = RenderState.createDefaultSet();
        states[RenderState.ARRAY_STATE] = null;
        states[RenderState.COLOR_STATE] = null;
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

        setConfig(ctx, config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (GlContext ctx, ComponentBillboardConfig config)
    {
        _ctx = ctx;
        _config = config;
        updateFromConfig();
    }

    // documentation inherited from interface Enqueueable
    public void enqueue ()
    {
        // update the rotation
        _parentViewTransform.compose(_localTransform, _billboardViewTransform);
        if (_updater != null) {
            _updater.update();
        } else {
            _billboardLocalTransform.getRotation().set(Quaternion.IDENTITY);
        }

        // update the view transform
        TransformState tstate = (TransformState)_batch.getStates()[RenderState.TRANSFORM_STATE];
        Transform3D modelview = tstate.getModelview();
        _billboardViewTransform.compose(_billboardLocalTransform, modelview);
        tstate.setDirty(true);

        // update the depth
        _batch.depth = modelview.transformPointZ(Vector3f.ZERO);

        // enqueue our batch
        _queue.add(_batch, _config.priority);
    }

    @Override
    public int getInfluenceFlags ()
    {
        return _influenceFlags;
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
            return;
        }
        _parentWorldTransform.compose(_localTransform, _worldTransform);

        // and the world bounds
        float extent = Math.max(_root.getWidth()/2, _root.getHeight()/2) * _config.scale;
        _nbounds.getMinimumExtent().set(-extent, -extent, -extent);
        _nbounds.getMaximumExtent().set(+extent, +extent, +extent);
        _nbounds.transformLocal(_worldTransform);
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
    public void composite ()
    {
        _ctx.getCompositor().addEnqueueable(this);
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

        // update the queue reference and states
        _queue = _ctx.getCompositor().getQueue(_config.queue);
        RenderState[] states = _batch.getStates();
        states[RenderState.ALPHA_STATE] = _config.alphaState.getState();
        states[RenderState.DEPTH_STATE] = _config.depthState.getState();

        // initialize the local transform
        _billboardLocalTransform.set(Vector3f.ZERO, Quaternion.IDENTITY, _config.scale);

        // (re)create the updater
        if (_config.rotationEnabled) {
            _updater = ArticulatedConfig.createBillboardUpdater(
                this, _billboardViewTransform, _billboardLocalTransform,
                _config.rotationX, _config.rotationY);
        } else {
            _updater = null;
        }

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

    /** The transform updater. */
    protected Updater _updater;

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The parent view transform. */
    @Bound("viewTransform")
    protected Transform3D _parentViewTransform;

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The billboard view transform. */
    protected Transform3D _billboardViewTransform = new Transform3D();

    /** The billboard local transform. */
    protected Transform3D _billboardLocalTransform = new Transform3D();

    /** The world transform. */
    protected Transform3D _worldTransform = new Transform3D();

    /** Flags indicating which influences can affect the model. */
    protected int _influenceFlags;

    /** The bounds of the model. */
    protected Box _bounds = new Box();

    /** Holds the bounds of the model when updating. */
    protected Box _nbounds = new Box();
}
