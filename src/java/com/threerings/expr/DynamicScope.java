//
// $Id$

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
        _symbols.put(name, value);
        wasUpdated();
    }

    /**
     * Removes the named symbol from this scope.
     */
    public void remove (String name)
    {
        _symbols.remove(name);
        wasUpdated();
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
        if (_compoundDepth == 0) {
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
        Object value = _symbols.get(name);
        return clazz.isInstance(value) ? clazz.cast(value) : ScopeUtil.get(_owner, name, clazz);
    }

    // documentation inherited from interface Scope
    public void addListener (ScopeUpdateListener listener)
    {
        _listeners.add(listener);
    }

    // documentation inherited from interface Scope
    public void removeListener (ScopeUpdateListener listener)
    {
        _listeners.remove(listener);
    }

    // documentation inherited from interface ScopeUpdateListener
    public void scopeUpdated (ScopeEvent event)
    {
        ScopeUtil.updateBound(_owner, _parentScope);
        wasUpdated();
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
    protected HashMap<String, Object> _symbols = new HashMap<String, Object>();

    /** The listeners to this scope. */
    protected WeakObserverList<ScopeUpdateListener> _listeners = WeakObserverList.newFastUnsafe();

    /** Used to force initialization. */
    protected static final Scope INVALID_SCOPE = new DynamicScope(null);
}
