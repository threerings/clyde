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

package com.threerings.util;

import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;

import com.google.common.collect.Sets;

/**
 * Wraps up an {@link IdentityHashMap} to look like a set for the purpose of subclassing.
 * IdentityHashMap has the desirable property of not generating extra objects when elements
 * are added/removed, because it stores the elements directly in its bucket array (resolving
 * collisions using linear probing).  Note that if you don't need to subclass the set,
 * then you can just pass an IdentityHashMap to {@link Sets#newSetFromMap}.
 */
public abstract class AbstractIdentityHashSet<T> extends AbstractSet<T>
{
    /**
     * Creates a new set with the default expected maximum size.
     */
    public AbstractIdentityHashSet ()
    {
        _map = new IdentityHashMap<T, Boolean>();
    }

    /**
     * Creates a new set with the specified expected maximum size.
     */
    public AbstractIdentityHashSet (int expectedMaxSize)
    {
        _map = new IdentityHashMap<T, Boolean>(expectedMaxSize);
    }

    @Override
    public int size ()
    {
        return _map.size();
    }

    @Override
    public boolean add (T element)
    {
        return _map.put(element, Boolean.TRUE) == null;
    }

    @Override
    public boolean remove (Object object)
    {
        return _map.remove(object) != null;
    }

    @Override
    public boolean contains (Object object)
    {
        return _map.containsKey(object);
    }

    @Override
    public void clear ()
    {
        _map.clear();
    }

    @Override
    public Iterator<T> iterator ()
    {
        return _map.keySet().iterator();
    }

    /** The underlying map. */
    protected IdentityHashMap<T, Boolean> _map;
}
