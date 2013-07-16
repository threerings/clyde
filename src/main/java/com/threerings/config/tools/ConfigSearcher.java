//
// $Id$

package com.threerings.config.tools;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
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
     * Find if anything satisfies the predicate in the specified object any any sub-objects.
     */
    public static boolean find (Object val, Predicate<? super ConfigReference<?>> detector)
    {
        return find(val, detector, Sets.newIdentityHashSet());
    }

    /**
     * Internal helper for find.
     */
    protected static boolean find (
        Object val, Predicate<? super ConfigReference<?>> detector,
        Set<Object> seen)
    {
        if (val == null) {
            return false;
        }

        Class<?> c = val.getClass();
        if ((c == String.class) || c.isPrimitive() || Primitives.isWrapperType(c) ||
                !seen.add(val)) {
            return false;
        }

        // make a list of sub-fields
        if (c.isArray()) {
            for (int ii = 0, nn = Array.getLength(val); ii < nn; ii++) {
                if (find(Array.get(val, ii), detector, seen)) {
                    return true;
                }
            }

        } else if (val instanceof ConfigReference) {
            ConfigReference<?> ref = (ConfigReference<?>)val;
            if (detector.apply(ref)) {
                return true;
            }
            for (Object value : ref.getArguments().values()) {
                if (find(value, detector, seen)) {
                    return true;
                }
            }

        } else {
            for (Field f : FIELDS.getUnchecked(c)) {
                Object o;
                try {
                    o = f.get(val);
                } catch (IllegalAccessException iae) {
                    continue;
                }
                if (find(o, detector, seen)) {
                    return true;
                }
            }
        }
        return false;
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
        if (val instanceof ConfigReference) {
            ConfigReference<?> ref = (ConfigReference<?>)val;
            list.add(detector.apply(ref));
            for (Object value : ref.getArguments().values()) {
                list.add(findAttributes(value, detector, seen));
            }

        } else if (c.isArray()) {
            for (int ii = 0, nn = Array.getLength(val); ii < nn; ii++) {
                list.add(findAttributes(Array.get(val, ii), detector, seen));
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

    /** All the fields (and superfields...) of a class, cached. */
    protected static final LoadingCache<Class<?>, ImmutableList<Field>> FIELDS =
        CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .build(
            new CacheLoader<Class<?>, ImmutableList<Field>>() {
                public ImmutableList<Field> load (Class<?> clazz) {
                    ImmutableList.Builder<Field> builder = ImmutableList.builder();
                    // add recurse on superclass
                    Class<?> superClazz = clazz.getSuperclass();
                    if (superClazz != null) {
                        builder.addAll(FIELDS.getUnchecked(superClazz));
                    }
                    // get all fields of the specified class, and filter out the static ones..
                    for (Field f : clazz.getDeclaredFields()) {
                        // add all non-static fields; make them accessible
                        if (0 == (f.getModifiers() & Modifier.STATIC)) {
                            f.setAccessible(true);
                            builder.add(f);
                        }
                    }
                    return builder.build();
                }
            });
}
