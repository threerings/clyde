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

package com.threerings.config;

import java.util.Map;

import com.google.common.cache.CacheBuilder;

import com.threerings.editor.Editable;
import com.threerings.expr.ExpressionBinding;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.ScopeUpdateListener;
import com.threerings.expr.Updater;
import com.threerings.util.DeepOmit;

/**
 * A configuration that may include a number of expressions to be evaluated in the scope in which
 * the configuration is instantiated.
 */
public class BoundConfig extends ParameterizedConfig
    implements ScopeUpdateListener
{
    /** The config bindings. */
    @Editable(weight=1)
    public ExpressionBinding[] bindings = ExpressionBinding.EMPTY_ARRAY;

    // documentation inherited from interface ScopeUpdateListener
    public void scopeUpdated (ScopeEvent event)
    {
        wasUpdated();
    }

    @Override
    protected BoundConfig getBound (Scope scope)
    {
        if (scope == null || bindings.length == 0) {
            return this;
        }
        if (_bound == null) {
            _bound = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .softValues()
                .weakKeys()
                .initialCapacity(1)
                .<Scope, BoundConfig>build().asMap();
        }
        BoundConfig bound = _bound.get(scope);
        if (bound == null) {
            _bound.put(scope, bound = (BoundConfig)clone());
            bound.init(_cfgmgr);
            bound._base = this;
            bound.bind(scope);
        }
        return bound;
    }

    @Override
    public void wasUpdated ()
    {
        // invalidate the bindings
        for (ExpressionBinding binding : bindings) {
            binding.invalidate();
        }

        // update the bindings (if bound)
        if (_updaters != null) {
            for (Updater updater : _updaters) {
                updater.update();
            }
        }

        // fire the event
        super.wasUpdated();

        // update the bound instances
        if (_bound != null) {
            for (BoundConfig bound : _bound.values()) {
                copy(bound);
                bound.wasUpdated();
            }
            if (_bound.isEmpty()) {
                _bound = null;
            }
        }
    }

    /**
     * Binds this config to the specified scope.
     */
    protected void bind (Scope scope)
    {
        _updaters = new Updater[bindings.length];
        for (int ii = 0; ii < bindings.length; ii++) {
            _updaters[ii] = bindings[ii].createUpdater(_cfgmgr, scope, this);
            _updaters[ii].update();
        }
        scope.addListener(this);
    }

    /** Updaters for our bindings. */
    @DeepOmit
    protected transient Updater[] _updaters;

    /** Maps scopes to bound instances. */
    @DeepOmit
    protected transient Map<Scope, BoundConfig> _bound;
}
