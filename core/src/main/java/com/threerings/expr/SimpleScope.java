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
        if ((_parentScope = parentScope) != null) {
            _parentScope.addListener(this);
        }
        ScopeUtil.updateBound(this, _parentScope);
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
        if (_parentScope != null) {
            _parentScope.addListener(listener);
        }
    }

    // documentation inherited from interface Scope
    public void removeListener (ScopeUpdateListener listener)
    {
        if (_parentScope != null) {
            _parentScope.removeListener(listener);
        }
    }

    // documentation inherited from interface ScopeUpdateListener
    public void scopeUpdated (ScopeEvent event)
    {
        ScopeUtil.updateBound(this, _parentScope);
    }

    /** A reference to the parent scope. */
    protected Scope _parentScope;
}
