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

import com.samskivert.swing.RuntimeAdjust;
import com.samskivert.util.PrefsConfig;

import com.threerings.expr.Bound;
import com.threerings.expr.Executor;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Box;
import com.threerings.math.Transform3D;

import com.threerings.opengl.model.config.ScriptedConfig;
import com.threerings.opengl.scene.SceneElement.TickPolicy;
import com.threerings.opengl.util.GlContext;

import static com.threerings.ClydeLog.log;

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
        setConfig(ctx, config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (GlContext ctx, ScriptedConfig config)
    {
        _ctx = ctx;
        _config = config;
        updateFromConfig();
    }

    @Override
    public boolean hasCompleted ()
    {
        return _completed;
    }

    @Override
    public void reset ()
    {
        _eidx = 0;
        _time = 0f;
        _completed = false;
    }

    @Override
    public void wasAdded ()
    {
        _lastAddedTimestamp = ScopeUtil.resolveTimestamp(this, Scope.NOW).value;
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
    public TickPolicy getTickPolicy ()
    {
        return _tickPolicy;
    }

    @Override
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
        if (_completed || _lastAddedTimestamp == 0) {
            // Don't bother ticking until we're added to the scene.
            return;
        }
        if (elapsed != 0) {
            // Don't bother updating the time if this is a zero-tick.
            // (Non-ticking elements do actually get ticked with a 0 elapsed.)
            _time = (ScopeUtil.resolveTimestamp(this, Scope.NOW).value -
                _lastAddedTimestamp) / 1000f;
        }
        executeActions();

        // check for loop or completion
        if (_config.loopDuration > 0f) {
            if (_time >= _config.loopDuration) {
                _time %= _config.loopDuration;
                _eidx = 0;
                executeActions();
            }
        } else if (_eidx >= _executors.length) {
            _completed = true;
            ((Model)_parentScope).completed(this);
        }
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
                action.time, action.duration,
                action.action.createExecutor(_ctx, this));
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
        boolean showDebug = _scriptedDebug.getValue();
        for (; _eidx < _executors.length && _executors[_eidx].time < _time; _eidx++) {
            if (showDebug && _executors[_eidx].duration == 0f &&
                    (_time - _executors[_eidx].time) > LONG_ELAPSED_TIME) {
                log.warning("Long Elapsed Time from Scripted",
                    "config", _config);
            }
            if (_executors[_eidx].duration == 0f ||
                    _time < (_executors[_eidx].time + _executors[_eidx].duration)) {
                _executors[_eidx].executor.execute();
            }
        }
    }

    /**
     * Contains an executor to activate at a specific time.
     */
    protected static class TimeExecutor
    {
        /** The time at which to execute the action. */
        public float time;

        /** The length of the executed action. */
        public float duration;

        /** The action executor. */
        public Executor executor;

        public TimeExecutor (float time, float duration, Executor executor)
        {
            this.time = time;
            this.duration = duration;
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
    @Scoped
    protected Box _bounds = new Box();

    /** The last timestamp for our tick. */
    protected long _lastAddedTimestamp;

    /** Holds the bounds of the model when updating. */
    protected Box _nbounds = new Box();

    /** The amount of time elapsed. */
    protected float _time;

    /** The index of the next executor. */
    protected int _eidx;

    /** If true, the script has completed. */
    protected boolean _completed;

    /** Tells us whether or not to enable debug output for long scripted events. */
    protected static RuntimeAdjust.BooleanAdjust _scriptedDebug =
            new RuntimeAdjust.BooleanAdjust("Enables debug messages for long scripted effects.",
                    "clyde.client.scriptedDebug", new PrefsConfig("clyde"), false);

    /** The time over which we spit out a debug message. */
    protected static float LONG_ELAPSED_TIME = 1f;
}
