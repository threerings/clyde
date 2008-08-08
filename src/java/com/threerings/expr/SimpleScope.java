//
// $Id$

package com.threerings.expr;

import com.threerings.expr.util.ScopeUtil;

/**
 * A base class for objects providing simple scopes.
 */
public abstract class SimpleScope
    implements Scope, ScopeUpdateListener
{
    /**
     * Creates a new simple scope.
     */
    public SimpleScope (Scope parentScope)
    {
        _parentScope = parentScope;
        _parentScope.addListener(this);
        ScopeUtil.updateBound(this, _parentScope);
    }

    // documentation inherited from interface Scope
    public String getScopeName ()
    {
        return null;
    }

    // documentation inherited from interface Scope
    public Scope getParentScope ()
    {
        return _parentScope;
    }

    // documentation inherited from interface Scope
    public <T> T get (String name, Class<T> clazz)
    {
        return ScopeUtil.get(this, name, clazz);
    }

    // documentation inherited from interface Scope
    public void addListener (ScopeUpdateListener listener)
    {
        _parentScope.addListener(listener);
    }

    // documentation inherited from interface Scope
    public void removeListener (ScopeUpdateListener listener)
    {
        _parentScope.removeListener(listener);
    }

    // documentation inherited from interface ScopeUpdateListener
    public void scopeUpdated (ScopeEvent event)
    {
        ScopeUtil.updateBound(this, _parentScope);
    }

    /** A reference to the parent scope. */
    protected Scope _parentScope;
}
