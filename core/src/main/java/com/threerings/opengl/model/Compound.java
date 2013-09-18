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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.expr.Bound;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.samskivert.util.StringUtil;

import com.threerings.opengl.compositor.Enqueueable;
import com.threerings.opengl.model.config.CompoundConfig;
import com.threerings.opengl.model.config.CompoundConfig.ComponentModel;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlContext;

/**
 * A compound model implementation.
 */
public class Compound extends Model.Implementation
    implements Enqueueable
{
    /**
     * Creates a new compound implementation.
     */
    public Compound (GlContext ctx, Scope parentScope, CompoundConfig config)
    {
        super(parentScope);
        setConfig(ctx, config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (GlContext ctx, CompoundConfig config)
    {
        _ctx = ctx;
        _config = config;
        updateFromConfig();
    }

    // documentation inherited from interface Enqueueable
    public void enqueue ()
    {
        // update the view transform
        _parentViewTransform.compose(_localTransform, _viewTransform);
    }

    @Override
    public List<Model> getChildren ()
    {
        return Collections.unmodifiableList(Arrays.asList(_models));
    }

    @Override
    public boolean hasCompleted ()
    {
        return _completed;
    }

    @Override
    public void setVisible (boolean visible)
    {
        for (Model model : _models) {
            model.setVisible(visible);
        }
    }

    @Override
    public void reset ()
    {
        for (Model model : _models) {
            model.reset();
        }
        _completed = false;
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
        tick(0f);
    }

    @Override
    public void drawBounds ()
    {
        DebugBounds.draw(_bounds, Color4f.WHITE);
        for (Model model : _models) {
            model.drawBounds();
        }
    }

    @Override
    public void dumpInfo (String prefix)
    {
        System.out.println(prefix + "Compound: " + _worldTransform + " " + _bounds);
        String pprefix = prefix + "  ";
        for (Model model : _models) {
            model.dumpInfo(pprefix);
        }
    }

    @Override
    public TickPolicy getTickPolicy ()
    {
        return _tickPolicy;
    }

    @Override
    public void wasAdded ()
    {
        // notify component models
        Scene scene = ((Model)_parentScope).getScene(this);
        for (Model model : _models) {
            model.wasAdded(scene);
        }
    }

    @Override
    public void willBeRemoved ()
    {
        // notify component models
        for (Model model : _models) {
            model.willBeRemoved();
        }
    }

    @Override
    public void tick (float elapsed)
    {
        // return immediately if completed
        if (_completed) {
            return;
        }

        // update the world transform
        if (_parentWorldTransform == null) {
            _worldTransform.set(_localTransform);
        } else {
            _parentWorldTransform.compose(_localTransform, _worldTransform);
        }

        // tick the component models
        _nbounds.setToEmpty();
        _completed = true;
        for (Model model : _models) {
            model.tick(elapsed);
            _nbounds.addLocal(model.getBounds());
            _completed &= model.hasCompleted();
        }

        // update the bounds if necessary
        if (!_bounds.equals(_nbounds)) {
            ((Model)_parentScope).boundsWillChange(this);
            _bounds.set(_nbounds);
            ((Model)_parentScope).boundsDidChange(this);
        }

        // notify containing model if completed
        if (_completed) {
            ((Model)_parentScope).completed(this);
        }
    }

    @Override
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        // exit early if there's no bounds intersection
        if (!_bounds.intersects(ray)) {
            return false;
        }
        // check the component models
        Vector3f closest = result;
        for (Model model : _models) {
            if (model.getIntersection(ray, result)) {
                result = FloatMath.updateClosest(ray.getOrigin(), result, closest);
            }
        }
        // if we ever changed the result reference, that means we hit something
        return (result != closest);
    }

    @Override
    public void composite ()
    {
        // add an enqueueable to initialize the shared state
        _ctx.getCompositor().addEnqueueable(this);

        // composite the component models
        for (Model model : _models) {
            model.composite();
        }
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        // create the component models
        Scene scene = ((Model)_parentScope).getScene(this);
        Model[] omodels = _models;
        _models = new Model[_config.models.length];
        Function getNodeFn = ScopeUtil.resolve(this, "getNode", Function.NULL);
        for (int ii = 0; ii < _models.length; ii++) {
            boolean create = (omodels == null || omodels.length <= ii);
            Model model = create ? new Model(_ctx) : omodels[ii];
            _models[ii] = model;
            ComponentModel component = _config.models[ii];
            Scope node = StringUtil.isBlank(component.node) ?
                null : (Scope)getNodeFn.call(component.node);
            model.setParentScope(node == null ? this : node);
            model.setConfig(component.model);
            model.getLocalTransform().set(component.transform);
            if (create && scene != null) {
                model.wasAdded(scene);
            }
        }
        if (omodels != null) {
            for (int ii = _models.length; ii < omodels.length; ii++) {
                Model model = omodels[ii];
                if (scene != null) {
                    model.willBeRemoved();
                }
                model.dispose();
            }
        }

        // update the influence flags
        _influenceFlags = _config.influences.getFlags();

        // update the tick policy if necessary
        TickPolicy npolicy = _config.tickPolicy;
        if (npolicy == TickPolicy.DEFAULT) {
            npolicy = TickPolicy.NEVER;
            for (Model model : _models) {
                TickPolicy mpolicy = model.getTickPolicy();
                if (mpolicy.ordinal() > npolicy.ordinal()) {
                    npolicy = mpolicy;
                }
            }
        }
        if (_tickPolicy != npolicy) {
            ((Model)_parentScope).tickPolicyWillChange(this);
            _tickPolicy = npolicy;
            ((Model)_parentScope).tickPolicyDidChange(this);
        }

        // update the bounds
        updateBounds();
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model config. */
    protected CompoundConfig _config;

    /** The component models. */
    protected Model[] _models;

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

    /** The view transform. */
    @Scoped
    protected Transform3D _viewTransform = new Transform3D();

    /** The model's tick policy. */
    protected TickPolicy _tickPolicy;

    /** Flags indicating which influences can affect the model. */
    protected int _influenceFlags;

    /** The bounds of the model. */
    @Scoped
    protected Box _bounds = new Box();

    /** Holds the bounds of the model when updating. */
    protected Box _nbounds = new Box();

    /** If true, the model has completed. */
    protected boolean _completed;
}
