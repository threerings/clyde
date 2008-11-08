//
// $Id$

package com.threerings.delta;

import java.io.IOException;

import java.lang.reflect.Method;

import java.util.HashMap;

import com.google.common.collect.Maps;

import com.threerings.io.Streamable;
import com.threerings.io.Streamer;

/**
 * Represents a set of changes that may be applied to an existing object to create a new object
 * (with a streamed form that is more compact than that which would be required for streaming
 * the entire new object).
 */
public abstract class Delta
    implements Streamable
{
    /**
     * Determines whether it is possible to create a {@link Delta} converting the original
     * object to the revised object (both non-<code>null</code>).
     */
    public static boolean checkDeltable (Object original, Object revised)
    {
        Class clazz = original.getClass();
        return revised.getClass() == clazz && (original instanceof Deltable || clazz.isArray());
    }

    /**
     * Creates and returns a new {@link Delta} that will convert the original object to the
     * revised object.
     */
    public static Delta createDelta (Object original, Object revised)
    {
        if (original instanceof Deltable) {
            // check for a custom creator method
            Class<?> clazz = original.getClass();
            Method creator = _creators.get(clazz);
            if (creator == null) {
                try {
                    creator = clazz.getMethod("createDelta", Object.class);
                } catch (NoSuchMethodException e) {
                    creator = _none;
                }
                _creators.put(clazz, creator);
            }
            if (creator == _none) {
                return new ReflectiveDelta(original, revised);
            }
            try {
                return (Delta)creator.invoke(original, revised);
            } catch (Exception e) {
                throw new RuntimeException("Error invoking custom delta method " + creator, e);
            }
        }
        Class clazz = original.getClass();
        if (clazz.isArray()) {
            return new ArrayDelta(original, revised);
        } else {
            throw new RuntimeException("Cannot create delta for " + clazz);
        }
    }

    /**
     * Applies this delta to the specified object.
     *
     * @return a new object incorporating the changes represented by this delta.
     */
    public abstract Object apply (Object original);

    /** Custom creator methods mapped by class. */
    protected static HashMap<Class, Method> _creators = new HashMap<Class, Method>();

    /** Irrelevant method used to indicate that the class has no custom creator. */
    protected static Method _none;
    static {
        try {
            _none = Object.class.getMethod("toString");
        } catch (NoSuchMethodException e) {
            // won't happen
        }
    }

    /** Streamer for raw class references. */
    protected static Streamer _classStreamer;

    /** Maps primitive types to {@link Streamer} instances for corresponding wrappers. */
    protected static HashMap<Class, Streamer> _wrapperStreamers = Maps.newHashMap();
    static {
        try {
            _classStreamer = Streamer.getStreamer(Class.class);
            _wrapperStreamers.put(Boolean.TYPE, Streamer.getStreamer(Boolean.class));
            _wrapperStreamers.put(Byte.TYPE, Streamer.getStreamer(Byte.class));
            _wrapperStreamers.put(Character.TYPE, Streamer.getStreamer(Character.class));
            _wrapperStreamers.put(Double.TYPE, Streamer.getStreamer(Double.class));
            _wrapperStreamers.put(Float.TYPE, Streamer.getStreamer(Float.class));
            _wrapperStreamers.put(Integer.TYPE, Streamer.getStreamer(Integer.class));
            _wrapperStreamers.put(Long.TYPE, Streamer.getStreamer(Long.class));
            _wrapperStreamers.put(Short.TYPE, Streamer.getStreamer(Short.class));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize ReflectiveDelta class", e);
        }
    }

    /** A special value indicating that the field has been changed to <code>null</code>. */
    protected static final Object NULL = new Object() {
        public String toString () {
            return "null";
        }
    };
}
