//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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
     * object to the revised object.
     */
    public static boolean checkDeltable (Object original, Object revised)
    {
        Class oclazz = (original == null) ? null : original.getClass();
        Class rclazz = (revised == null) ? null : revised.getClass();
        return oclazz == rclazz && (original instanceof Deltable || oclazz.isArray());
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
}
