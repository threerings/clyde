//
// $Id$

package com.threerings.config.util;

import java.util.List;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * Utility class for reflection.
 */
public class FieldCache
{
    /**
     * Get the default predicate, which returns non-transient instance fields.
     */
    public static Predicate<Field> getDefaultPredicate ()
    {
        return new Predicate<Field>() {
                    public boolean apply (Field field) {
                        return 0 == (field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT));
                    }
                };
    }

    /**
     * Create a FieldCache that finds non-static, non-transient fields.
     */
    public FieldCache ()
    {
        this(getDefaultPredicate());
    }

    /**
     * Create a FieldCache that selects fields according to the specified predicate.
     */
    public FieldCache (Predicate<? super Field> pred)
    {
        _pred = pred;
    }

    /**
     * Get the fields in the specified class.
     */
    public ImmutableList<Field> getFields (Class<?> clazz)
    {
        return _fields.getUnchecked(clazz);
    }

    /** Our predicate for selecting fields. */
    protected final Predicate<? super Field> _pred;

    /** All the fields (and superfields...) of a class, cached. */
    protected final LoadingCache<Class<?>, ImmutableList<Field>> _fields =
            CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .build(new CacheLoader<Class<?>, ImmutableList<Field>>() {
                    public ImmutableList<Field> load (Class<?> clazz) {
                        ImmutableList.Builder<Field> builder = ImmutableList.builder();
                        // add recurse on superclass
                        Class<?> superClazz = clazz.getSuperclass();
                        if (superClazz != null) {
                            builder.addAll(_fields.getUnchecked(superClazz));
                        }
                        List<Field> ourFields = Lists.newArrayList();
                        // get all the filtered fields of the specified class
                        for (Field f : clazz.getDeclaredFields()) {
                            if (_pred.apply(f)) {
                                f.setAccessible(true);
                                ourFields.add(f);
                            }
                        }
                        // add our fields in name-sorted order
                        builder.addAll(Ordering.natural().onResultOf(new Function<Field, String>() {
                                public String apply (Field field) {
                                    return field.getName();
                                }
                            }).immutableSortedCopy(ourFields));
                        return builder.build();
                    }
                });
}
