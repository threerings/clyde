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

package com.threerings.opengl.model;

import com.threerings.expr.Bound;
import com.threerings.expr.Executor;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.Scoped;
import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform3D;

import com.threerings.opengl.model.config.ScriptedConfig;
import com.threerings.opengl.model.config.ScriptedConfig.TimeAction;
import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.GlContext;

/**
 * A scripted model implementation.
 */
public class Scripted extends Model.Implementation
{
    /**
     * Creates a new scripted implementation.
     */
    public Scripted (GlContext ctx, Scope parentScope, ScriptedConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        setConfig(config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (ScriptedConfig config)
    {
        _config = config;
        updateFromConfig();
    }

    @Override // documentation inherited
    public boolean hasCompleted ()
    {
        return _completed;
    }

    @Override // documentation inherited
    public void reset ()
    {
        _eidx = 0;
        _time = 0f;
        _completed = false;
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
        tick(0f);
    }

    @Override // documentation inherited
    public TickPolicy getTickPolicy ()
    {
        return _tickPolicy;
    }

    @Override // documentation inherited
    public void tick (float elapsed)
    {
        // update the world transform
        if (_parentWorldTransform == null) {
            _worldTransform.set(_localTransform);
        } else {
            _parentWorldTransform.compose(_localTransform, _worldTransform);
        }

        // update the bounds
        float expand = _config.boundsExpansion;
        Box.ZERO.expand(expand, expand, expand, _nbounds).transformLocal(_worldTransform);
        if (!_bounds.equals(_nbounds)) {
            ((Model)_parentScope).boundsWillChange(this);
            _bounds.set(_nbounds);
            ((Model)_parentScope).boundsDidChange(this);
        }
        if (_completed) {
            return;
        }
        _time += elapsed;
        executeActions();

        // check for loop or completion
        if (_config.loopDuration > 0f) {
            if (_time >= _config.loopDuration) {
                _time = FloatMath.IEEEremainder(_time, _config.loopDuration);
                _eidx = 0;
                executeActions();
            }
        } else if (_eidx >= _executors.length) {
            _completed = true;
            ((Model)_parentScope).completed(this);
        }
    }

    @Override // documentation inherited
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        updateFromConfig();
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        // create the executors
        _executors = new TimeExecutor[_config.actions.length];
        for (int ii = 0; ii < _executors.length; ii++) {
            ScriptedConfig.TimeAction action = _config.actions[ii];
            _executors[ii] = new TimeExecutor(
                action.time, action.action.createExecutor(_ctx, this));
        }

        // update the influence flags
        _influenceFlags = _config.influences.getFlags();

        // update the tick policy if necessary
        TickPolicy npolicy = _config.tickPolicy;
        if (npolicy == TickPolicy.DEFAULT) {
            npolicy = (_config.loopDuration > 0f) ? TickPolicy.WHEN_VISIBLE : TickPolicy.ALWAYS;
        }
        if (_tickPolicy != npolicy) {
            ((Model)_parentScope).tickPolicyWillChange(this);
            _tickPolicy = npolicy;
            ((Model)_parentScope).tickPolicyDidChange(this);
        }

        // update the bounds
        updateBounds();
    }

    /**
     * Executes all actions scheduled before or at the current time.
     */
    protected void executeActions ()
    {
        for (; _eidx < _executors.length && _executors[_eidx].time < _time; _eidx++) {
            _executors[_eidx].executor.execute();
        }
    }

    /**
     * Contains an executor to activate at a specific time.
     */
    protected static class TimeExecutor
    {
        /** The time at which to execute the action. */
        public float time;

        /** The action executor. */
        public Executor executor;

        public TimeExecutor (float time, Executor executor)
        {
            this.time = time;
            this.executor = executor;
        }
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model config. */
    protected ScriptedConfig _config;

    /** Executors for the timed actions. */
    protected TimeExecutor[] _executors;

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The world transform. */
    @Scoped
    protected Transform3D _worldTransform = new Transform3D();

    /** The model's tick policy. */
    protected TickPolicy _tickPolicy;

    /** Flags indicating which influences can affect the model. */
    protected int _influenceFlags;

    /** The bounds of the model. */
    protected Box _bounds = new Box();

    /** Holds the bounds of the model when updating. */
    protected Box _nbounds = new Box();

    /** The amount of time elapsed. */
    protected float _time;

    /** The index of the next executor. */
    protected int _eidx;

    /** If true, the script has completed. */
    protected boolean _completed;
}
