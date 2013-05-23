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
import com.threerings.expr.BooleanExpression;
import com.threerings.expr.MutableFloat;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.math.Box;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.compositor.Enqueueable;
import com.threerings.opengl.model.config.ConditionalConfig;
import com.threerings.opengl.model.config.ConditionalConfig.Case;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.GlContext;

/**
 * A conditional model implementation.
 */
public class Conditional extends Model.Implementation
    implements Enqueueable
{
    /**
     * Creates a new conditional implementation.
     */
    public Conditional (GlContext ctx, Scope parentScope, ConditionalConfig config)
    {
        super(parentScope);
        setConfig(ctx, config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (GlContext ctx, ConditionalConfig config)
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

    // TODO: setVisible?

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
        // update the world transform
        if (_parentWorldTransform == null) {
            _worldTransform.set(_localTransform);
        } else {
            _parentWorldTransform.compose(_localTransform, _worldTransform);
        }

        // update the active model and its bounds
        updateActive();
        _active.updateBounds();

        // update the bounds if necessary
        Box nbounds = _active.getBounds();
        if (!_bounds.equals(nbounds)) {
            ((Model)_parentScope).boundsWillChange(this);
            _bounds.set(nbounds);
            ((Model)_parentScope).boundsDidChange(this);
        }
    }

    @Override
    public void drawBounds ()
    {
        _active.drawBounds();
    }

    @Override
    public void dumpInfo (String prefix)
    {
        System.out.println(prefix + "Conditional: " + _worldTransform + " " + _bounds);
        _active.dumpInfo(prefix + "  ");
    }

    @Override
    public TickPolicy getTickPolicy ()
    {
        return _tickPolicy;
    }

    @Override
    public void wasAdded ()
    {
        Scene scene = ((Model)_parentScope).getScene(this);
        _active.wasAdded(scene);
    }

    @Override
    public void willBeRemoved ()
    {
        _active.willBeRemoved();
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

        // update and tick the active model
        updateActive();
        _active.tick(elapsed);

        // update the bounds if necessary
        Box nbounds = _active.getBounds();
        if (!_bounds.equals(nbounds)) {
            ((Model)_parentScope).boundsWillChange(this);
            _bounds.set(nbounds);
            ((Model)_parentScope).boundsDidChange(this);
        }

        // notify containing model if completed
        if (_completed = _active.hasCompleted()) {
            ((Model)_parentScope).completed(this);
        }
    }

    @Override
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        return _active.getIntersection(ray, result);
    }

    @Override
    public void composite ()
    {
        // add an enqueueable to initialize the shared state
        _ctx.getCompositor().addEnqueueable(this);

        // composite the active model
        _active.composite();
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        // create the evaluators
        _evaluators = new BooleanExpression.Evaluator[_config.cases.length];
        for (int ii = 0; ii < _evaluators.length; ii++) {
            _evaluators[ii] = _config.cases[ii].condition.createEvaluator(this);
        }

        // create the models
        Model[] omodels = _models;
        _models = new Model[_config.cases.length + 1];
        for (int ii = 0; ii < _models.length; ii++) {
            Model model = (omodels == null || omodels.length <= ii) ?
                new Model(_ctx) : omodels[ii];
            _models[ii] = model;
            model.setParentScope(this);
            if (ii == _config.cases.length) {
                model.setConfig(_config.defaultModel);
                model.getLocalTransform().set(_config.defaultTransform);
            } else {
                Case caze = _config.cases[ii];
                model.setConfig(caze.model);
                model.getLocalTransform().set(caze.transform);
            }
        }
        if (omodels != null) {
            for (int ii = _models.length; ii < omodels.length; ii++) {
                omodels[ii].dispose();
            }
        }

        // update the influence flags
        _influenceFlags = _config.influences.getFlags();

        // update the bounds
        updateBounds();
    }

    /**
     * Updates the active model.
     */
    protected void updateActive ()
    {
        // get the distance from the camera for lod conditions
        Transform3D cameraTransform = _ctx.getCompositor().getCamera().getWorldTransform();
        _worldTransform.update(Transform3D.UNIFORM);
        _distance.value = cameraTransform.getTranslation().distance(
            _worldTransform.getTranslation());

        // evaluate the cases to find the active index
        int idx = 0;
        for (; idx < _evaluators.length && !_evaluators[idx].evaluate(); idx++);
        Model nactive = _models[idx];
        if (_active == nactive) {
            return;
        }
        Scene scene = ((Model)_parentScope).getScene(this);
        if (scene != null && _active != null) {
            _active.willBeRemoved();
        }
        _active = nactive;
        if (scene != null) {
            _active.wasAdded(scene);
        }

        // update the tick policy
        TickPolicy npolicy = _config.tickPolicy;
        if (npolicy == TickPolicy.DEFAULT) {
            npolicy = _active.getTickPolicy();
        }
        if (_tickPolicy != npolicy) {
            ((Model)_parentScope).tickPolicyWillChange(this);
            _tickPolicy = npolicy;
            ((Model)_parentScope).tickPolicyDidChange(this);
        }
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model config. */
    protected ConditionalConfig _config;

    /** The evaluators for the cases. */
    protected BooleanExpression.Evaluator[] _evaluators;

    /** The case models. */
    protected Model[] _models;

    /** The active model. */
    protected Model _active;

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

    /** The distance to the camera. */
    @Scoped
    protected MutableFloat _distance = new MutableFloat();

    /** The model's tick policy. */
    protected TickPolicy _tickPolicy;

    /** Flags indicating which influences can affect the model. */
    protected int _influenceFlags;

    /** The bounds of the model. */
    @Scoped
    protected Box _bounds = new Box();

    /** If true, the model has completed. */
    protected boolean _completed;
}
