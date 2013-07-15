//
// $Id$

package com.threerings.config.tools;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;

import com.threerings.config.ConfigReference;

/**
 * Utilitiies for searching for and in ConfigReferences.
 */
public class ConfigSearcher
{
    /**
     * Find all the attributes in the specified object and any sub-objects.
     */
    public static <T> Iterable<T> findAttributes (
        Object val, Function<? super ConfigReference<?>, ? extends Iterable<T>> detector)
    {
        return findAttributes(val, detector, Sets.newIdentityHashSet());
    }

    /**
     * Internal helper for findAttributes.
     */
    protected static <T> Iterable<T> findAttributes (
        Object val, Function<? super ConfigReference<?>, ? extends Iterable<T>> detector,
        Set<Object> seen)
    {
        if (val == null) {
            return ImmutableList.<T>of();
        }

        Class<?> c = val.getClass();
        if ((c == String.class) || c.isPrimitive() || Primitives.isWrapperType(c) ||
                !seen.add(val)) {
            return ImmutableList.<T>of();
        }

        // make a list of sub-fields
        List<Iterable<T>> list = Lists.newArrayList();
        if (c.isArray()) {
            for (int ii = 0, nn = Array.getLength(val); ii < nn; ii++) {
                list.add(findAttributes(Array.get(val, ii), detector, seen));
            }

        } else if (val instanceof ConfigReference) {
            ConfigReference<?> ref = (ConfigReference<?>)val;
            list.add(detector.apply(ref));
            for (Object value : ref.getArguments().values()) {
                list.add(findAttributes(value, detector, seen));
            }

        } else {
            for (Field f : FIELDS.getUnchecked(c)) {
                Object o;
                try {
                    o = f.get(val);
                } catch (IllegalAccessException iae) {
                    continue;
                }
                list.add(findAttributes(o, detector, seen));
            }
        }
        return Iterables.concat(list);
    }

    /** A cache of Class -> Field[], since calling getFields() copies each time to protect the
     * original array. */
    protected static final LoadingCache<Class<?>, Field[]> FIELDS = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .build(
            new CacheLoader<Class<?>, Field[]>() {
                public Field[] load (Class<?> c) {
                    Field[] fa = c.getFields();
                    for (Field f : fa) {
                        f.setAccessible(true);
                    }
                    return fa;
                }
            });
}
