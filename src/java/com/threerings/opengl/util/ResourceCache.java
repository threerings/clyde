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
public abstract class ResourceCache<T>
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
     * Retrieves the resource at the specified path.
     */
    protected T getResource (String path)
    {
        CachedResource<T> cached = _resources.get(path);
        if (cached != null) {
            T resource = cached.ref.get();
            if (!(resource == null || (_checkTimestamps &&
                    getResourceFile(path).lastModified() > cached.lastModified))) {
                return resource;
            }
        }
        File file = getResourceFile(path);
        T resource = loadResource(file);
        _resources.put(path, new CachedResource<T>(resource, file.lastModified()));
        return resource;
    }

    /**
     * Returns the file corresponding to the specified path (either an absolute pathname or the
     * name of a resource).
     */
    protected File getResourceFile (String path)
    {
        // TODO: remove when we no longer need to support absolute paths
        File file = new File(path);
        return file.isAbsolute() ? file : _ctx.getResourceManager().getResourceFile(path);
    }

    /**
     * Loads the resource from the specified file.
     */
    protected abstract T loadResource (File file);

    /**
     * Contains a cached resource.
     */
    protected static class CachedResource<T>
    {
        /** The underlying reference to the resource. */
        public SoftReference<T> ref;

        /** The last-modified timestamp of the resource file. */
        public long lastModified;

        public CachedResource (T resource, long lastModified)
        {
            ref = new SoftReference<T>(resource);
            this.lastModified = lastModified;
        }
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** Whether or not to check resource file timestamps. */
    protected boolean _checkTimestamps;

    /** The cached resources. */
    protected HashMap<String, CachedResource<T>> _resources = Maps.newHashMap();
}
