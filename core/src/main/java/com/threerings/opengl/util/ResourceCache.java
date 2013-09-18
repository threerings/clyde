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

import java.io.File;

import java.util.HashMap;

import java.lang.ref.SoftReference;

import com.google.common.collect.Maps;

/**
 * Base class for the resource caches.
 */
public abstract class ResourceCache
{
    /**
     * Creates a new resource cache.
     *
     * @param checkTimestamps if true, check the last-modified timestamp of each resource file
     * when we retrieve it from the cache, reloading the resource if the file has been modified
     * externally.
     */
    public ResourceCache (GlContext ctx, boolean checkTimestamps)
    {
        _ctx = ctx;
        _checkTimestamps = checkTimestamps;
    }

    /**
     * A cache for a single type of resource.
     */
    protected abstract class Subcache<K, V>
    {
        /**
         * Retrieves the resource corresponding to the specified key.
         */
        public V getResource (K key)
        {
            CachedResource<V> cached = _resources.get(key);
            if (cached != null) {
                V resource = cached.get();
                if (!(resource == null || (_checkTimestamps && cached.wasModified()))) {
                    return resource;
                }
            }
            V resource = loadResource(key);
            _resources.put(key, new CachedResource<V>(resource, getResourceFile(key)));
            return resource;
        }

        /**
         * Clears the subcache, forcing all resources to be reloaded.
         */
        public void clear ()
        {
            _resources.clear();
        }

        /**
         * Loads the resource corresponding to the specified key.
         */
        protected abstract V loadResource (K key);

        /**
         * Returns the file corresponding to the specified key.
         */
        protected File getResourceFile (K key)
        {
            return _ctx.getResourceManager().getResourceFile(getResourcePath(key));
        }

        /**
         * Returns the resource path corresponding to the specified key.
         */
        protected abstract String getResourcePath (K key);

        /** The cached resources. */
        protected HashMap<K, CachedResource<V>> _resources = Maps.newHashMap();
    }

    /**
     * Contains a cached resource.
     */
    protected static class CachedResource<T> extends SoftReference<T>
    {
        public CachedResource (T resource, File file)
        {
            super(resource);
            _file = file;
            _lastModified = file.lastModified();
        }

        /**
         * Determines whether the resource file has been modified since this reference
         * was created.
         */
        public boolean wasModified ()
        {
            return _file.lastModified() > _lastModified;
        }

        /** The file corresponding to the resource. */
        protected File _file;

        /** The recorded last-modified timestamp. */
        protected long _lastModified;
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** Whether or not to check resource file timestamps. */
    protected boolean _checkTimestamps;
}
