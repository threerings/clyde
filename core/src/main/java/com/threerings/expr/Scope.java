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

/**
 * Provides a means to resolve symbols in a dynamic, hierarchical fashion.  Symbols can be mapped
 * to {@link Function}s, {@link Variable}s, or arbitrary objects (often mutable ones, so that
 * values can change after resolution).
 */
public interface Scope
{
    /** The name of a special symbol that we expect to map to a {@link MutableLong} containing the
     * current time as sampled at the beginning of each frame. */
    public static final String NOW = "now";

    /** The name of a special symbol that we expect to map to a {@link MutableLong} containing the
     * base time of the scope's owner (such at the time at which an animation started). */
    public static final String EPOCH = "epoch";

    /**
     * Returns the name of this scope for purposes of qualification.  Can return <code>null</code>
     * if qualified symbols cannot specifically address this scope.
     */
    public String getScopeName ();

    /**
     * Returns a reference to the parent scope, or <code>null</code> if this is the top level.
     */
    public Scope getParentScope ();

    /**
     * Looks up a symbol in this scope.
     *
     * @return the mapping for the requested symbol, or <code>null</code> if not found.
     */
    public <T> T get (String name, Class<T> clazz);

    /**
     * Adds a listener for changes in scope.  The listener will be notified when symbols are
     * added or removed and whenever the scope hierarchy changes.
     */
    public void addListener (ScopeUpdateListener listener);

    /**
     * Removes a listener for changes in scope.
     */
    public void removeListener (ScopeUpdateListener listener);
}
