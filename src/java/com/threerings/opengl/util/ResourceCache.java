//
// $Id$

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
         * Loads the resource corresponding to the specified key.
         */
        protected abstract V loadResource (K key);

        /**
         * Returns the file corresponding to the specified key.
         */
        protected File getResourceFile (K key)
        {
            // TODO: simplify when we no longer need to support absolute paths
            String path = getResourcePath(key);
            File file = new File(path);
            return file.isAbsolute() ? file : _ctx.getResourceManager().getResourceFile(path);
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
