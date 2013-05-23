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

package com.threerings.opengl.util;

import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Contains a set of preloadables and provides a means for incrementalling preloading them.
 */
public class PreloadableSet extends HashSet<Preloadable>
{
    /**
     * Creates a new preloadable set.
     */
    public PreloadableSet (GlContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Preloads a batch of the default duration.
     *
     * @return the percentage of the total resources loaded, from zero to one.
     */
    public float preloadBatch ()
    {
        return preloadBatch(100L);
    }

    /**
     * Preloads a batch of resources in the set.  Any preloadables added to the set after this
     * method is called for the first time will be preloaded immediately.
     *
     * @param duration the maximum amount of time to spend on the batch.
     * @return the percentage of the total resources loaded, from zero to one.
     */
    public float preloadBatch (long duration)
    {
        if (_remaining != null && _remaining.isEmpty()) {
            return 1f;
        }
        if (_remaining == null) {
            _remaining = Lists.newArrayList(this);
        }
        long end = System.currentTimeMillis() + duration;
        for (int ii = _remaining.size() - 1; ii >= 0 && System.currentTimeMillis() < end; ii--) {
            _remaining.remove(ii).preload(_ctx);
            _preloaded++;
        }
        if (_remaining.isEmpty()) {
            return 1f;
        }
        return (float)_preloaded / size();
    }

    @Override
    public boolean add (Preloadable preloadable)
    {
        if (!super.add(preloadable)) {
            return false;
        }
        // if already processing batches, just preload this immediately
        if (_remaining != null) {
            preloadable.preload(_ctx);
            _preloaded++;
        }
        return true;
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The list of resources remaining to be preloaded. */
    protected List<Preloadable> _remaining;

    /** The number of resources preloaded so far. */
    protected int _preloaded;
}
