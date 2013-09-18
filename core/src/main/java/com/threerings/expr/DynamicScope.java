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

package com.threerings.expr;

import java.util.HashMap;

import com.samskivert.util.ObserverList;
import com.samskivert.util.WeakObserverList;

import com.threerings.expr.util.ScopeUtil;

/**
 * A {@link Scope} that allows dynamic reparenting and the addition and removal of symbols.  Can be
 * used either as a base class or as a contained object.
 */
public class DynamicScope
    implements Scope, ScopeUpdateListener
{
    /**
     * Creates a new scope that is its own owner.
     */
    public DynamicScope (String name)
    {
        this(name, (Scope)null);
    }

    /**
     * Creates a new scope that is its own owner with the supplied scope as its parent.
     */
    public DynamicScope (String name, Scope parent)
    {
        _owner = this;
        _scopeName = name;
        setParentScope(parent);
    }

    /**
     * Creates a new scope.
     */
    public DynamicScope (Object owner, String name)
    {
        this(owner, name, null);
    }

    /**
     * Creates a new scope with the supplied scope as its parent.
     */
    public DynamicScope (Object owner, String name, Scope parent)
    {
        _owner = owner;
        _scopeName = name;
        setParentScope(parent);
    }

    /**
     * Sets the parent of this scope.
     */
    public void setParentScope (Scope parent)
    {
        if (_parentScope == parent) {
            return;
        }
        if (_parentScope != null) {
            _parentScope.removeListener(this);
        }
        if ((_parentScope = parent) != null) {
            _parentScope.addListener(this);
        }
        ScopeUtil.updateBound(_owner, _parentScope);
        wasUpdated();
    }

    /**
     * Sets the mapping for the named symbol in this scope.
     */
    public void put (String name, Object value)
    {
        if (_symbols == null) {
            _symbols = new HashMap<String, Object>(1);
        }
        _symbols.put(name, value);
        wasUpdated();
    }

    /**
     * Removes the named symbol from this scope.
     */
    public void remove (String name)
    {
        if (_symbols != null) {
            _symbols.remove(name);
            wasUpdated();
        }
    }

    /**
     * Starts a compound update.  Update notifications will be deferred until
     * {@link #endCompoundUpdate} is called.
     */
    public void startCompoundUpdate ()
    {
        _compoundDepth++;
    }

    /**
     * Ends a compound update.
     */
    public void endCompoundUpdate ()
    {
        if (--_compoundDepth == 0) {
            wasUpdated();
        }
    }

    /**
     * Notes that this scope has been updated.
     */
    public void wasUpdated ()
    {
        if (_compoundDepth == 0 && _listeners != null) {
            final ScopeEvent event = new ScopeEvent(this);
            _listeners.apply(new ObserverList.ObserverOp<ScopeUpdateListener>() {
                public boolean apply (ScopeUpdateListener listener) {
                    listener.scopeUpdated(event);
                    return true;
                }
            });
        }
    }

    /**
     * Releases the resources associated with this scope.
     */
    public void dispose ()
    {
        if (_parentScope != null) {
            _parentScope.removeListener(this);
        }
        _listeners = null;
    }

    // documentation inherited from interface Scope
    public String getScopeName ()
    {
        return _scopeName;
    }

    // documentation inherited from interface Scope
    public Scope getParentScope ()
    {
        return _parentScope;
    }

    // documentation inherited from interface Scope
    public <T> T get (String name, Class<T> clazz)
    {
        // first try the dynamic symbols, then the reflective ones
        Object value = (_symbols == null) ? null : _symbols.get(name);
        return clazz.isInstance(value) ? clazz.cast(value) : ScopeUtil.get(_owner, name, clazz);
    }

    // documentation inherited from interface Scope
    public void addListener (ScopeUpdateListener listener)
    {
        checkCreateListeners();
        _listeners.add(listener);
    }

    // documentation inherited from interface Scope
    public void addListener (int index, ScopeUpdateListener listener)
    {
        checkCreateListeners();
        _listeners.add(index, listener);
    }

    // documentation inherited from interface Scope
    public void removeListener (ScopeUpdateListener listener)
    {
        if (_listeners != null) {
            _listeners.remove(listener);
            if (_listeners.isEmpty()) {
                _listeners = null;
            }
        }
    }

    // documentation inherited from interface ScopeUpdateListener
    public void scopeUpdated (ScopeEvent event)
    {
        ScopeUtil.updateBound(_owner, _parentScope);
        wasUpdated();
    }

    protected void checkCreateListeners ()
    {
        if (_listeners == null) {
            // we disable duplicate checking for performance; don't fuck up
            // NOTE: This FORCES us to evaluate in reverse order.
            _listeners = WeakObserverList.newFastUnsafe();
            _listeners.setCheckDuplicates(false);
        }
    }

    /** The owner of this scope. */
    protected Object _owner;

    /** The name of this scope. */
    protected String _scopeName;

    /** A reference to the parent scope. */
    protected Scope _parentScope = INVALID_SCOPE;

    /** The compound update depth. */
    protected int _compoundDepth;

    /** The mappings for the dynamic symbols in this scope. */
    protected HashMap<String, Object> _symbols;

    /** The listeners to this scope. */
    protected WeakObserverList<ScopeUpdateListener> _listeners;

    /** Used to force initialization. */
    protected static final Scope INVALID_SCOPE = new DynamicScope(null);
}
