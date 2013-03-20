//
// $Id$

package com.threerings.util;

import java.util.Map;

import com.google.common.cache.CacheBuilder;

/**
 * Utilities to build Maps backed by a guava Cache.
 */
public class CacheUtil
{
    /**
     * Create a soft-value map.
     */
    public static <K, V> Map<K, V> softValues ()
    {
        return softValues(-1);
    }

    /**
     * Create a soft-value map with the specified initial capacity.
     */
    public static <K, V> Map<K, V> softValues (int initialCapacity)
    {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .softValues();
        if (initialCapacity != -1) {
            builder.initialCapacity(initialCapacity);
        }
        return builder.<K, V>build().asMap();
    }
}
