//
// $Id$

package com.threerings.config;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import java.util.Iterator;
import java.util.Map;

import com.samskivert.util.SoftCache;

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
    public ExpressionBinding[] bindings = new ExpressionBinding[0];

    // documentation inherited from interface ScopeUpdateListener
    public void scopeUpdated (ScopeEvent event)
    {
        wasUpdated();
    }

    @Override // documentation inherited
    protected BoundConfig getBound (Scope scope)
    {
        if (scope == null || bindings.length == 0) {
            return this;
        }
        if (_bound == null) {
            _bound = new SoftCache<ScopeKey, BoundConfig>();
        }
        ScopeKey key = new ScopeKey(scope);
        BoundConfig bound = _bound.get(key);
        if (bound == null) {
            _bound.put(key, bound = (BoundConfig)clone());
            bound.init(_cfgmgr);
            bound._base = this;
            bound.bind(scope);
        }
        return bound;
    }

    @Override // documentation inherited
    public void wasUpdated ()
    {
        // invalidate the binding paths
        for (ExpressionBinding binding : bindings) {
            binding.invalidatePaths();
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
        if (_bound == null) {
            return;
        }
        for (Iterator<Map.Entry<ScopeKey, SoftReference<BoundConfig>>> it =
                _bound.getMap().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ScopeKey, SoftReference<BoundConfig>> entry = it.next();
            BoundConfig bound = entry.getValue().get();
            if (bound == null) {
                it.remove();
                continue;
            }
            copy(bound);
            bound.wasUpdated();
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

    /**
     * Identifies a scope.
     */
    protected static class ScopeKey
    {
        public ScopeKey (Scope scope)
        {
            _scope = new WeakReference<Scope>(scope);
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return System.identityHashCode(_scope.get());
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return _scope.get() == ((ScopeKey)other)._scope.get();
        }

        /** The scope. */
        protected WeakReference<Scope> _scope;
    }

    /** Updaters for our bindings. */
    @DeepOmit
    protected transient Updater[] _updaters;

    /** Maps scopes to bound instances. */
    @DeepOmit
    protected transient SoftCache<ScopeKey, BoundConfig> _bound;
}
