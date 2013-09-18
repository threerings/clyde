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

package com.threerings.delta;

import java.io.IOException;

import java.lang.reflect.Method;

import java.util.Map;

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
        Class<?> oclazz = (original == null) ? null : original.getClass();
        Class<?> rclazz = (revised == null) ? null : revised.getClass();
        return oclazz == rclazz && (original instanceof Deltable || oclazz.isArray());
    }

    /**
     * Creates and returns a new {@link Delta} that will convert the original object to the
     * revised object.
     */
    public static Delta createDelta (Object original, Object revised)
    {
        Class<?> clazz = original.getClass();
        DeltaCreator dc = _creators.get(clazz);
        if (dc == null) {
            dc = DeltaCreator.create(clazz);
            _creators.put(clazz, dc);
        }
        return dc.createDelta(original, revised);
    }

    /**
     * Applies this delta to the specified object.
     *
     * @return a new object incorporating the changes represented by this delta.
     */
    public abstract Object apply (Object original);

    /**
     * Merges this delta with another.
     *
     * @return a new delta containing the changes included in both.
     */
    public abstract Delta merge (Delta other);

    /**
     * Abstracts the different ways that a Delta can be created.
     */
    protected static abstract class DeltaCreator
    {
        /**
         * Create a DeltaCreator for the specified Class.
         */
        public static DeltaCreator create (Class<?> clazz)
        {
            if (clazz.isArray()) {
                return ARRAY;

            } else if (Deltable.class.isAssignableFrom(clazz)) {
                try {
                    final Method creator = clazz.getMethod("createDelta", Object.class);
                    return new DeltaCreator() {
                        public Delta createDelta (Object original, Object revised) {
                            try {
                                return (Delta)creator.invoke(original, revised);
                            } catch (Exception e) {
                                throw new RuntimeException(
                                        "Error invoking custom delta method " + creator, e);
                            }
                        }
                    };

                } catch (NoSuchMethodException e) {
                    return REFLECTIVE;
                }
            }
            throw new RuntimeException("Cannot create delta for " + clazz);
        }

        /** A sharable DeltaCreator that creates ReflectiveDelta instances. */
        protected static final DeltaCreator REFLECTIVE = new DeltaCreator() {
            public Delta createDelta (Object original, Object revised) {
                return new ReflectiveDelta(original, revised);
            }
        };

        /** A sharable DeltaCreator that creates ArrayDelta instances. */
        protected static final DeltaCreator ARRAY = new DeltaCreator() {
            public Delta createDelta (Object original, Object revised) {
                return new ArrayDelta(original, revised);
            }
        };

        /**
         * Create a delta for the specified objects.
         */
        public abstract Delta createDelta (Object original, Object revised);
    }

    /** Custom creator methods mapped by class. */
    protected static Map<Class<?>, DeltaCreator> _creators = Maps.newIdentityHashMap();

    /** Streamer for raw class references. */
    protected static Streamer _classStreamer;
    static {
        try {
            _classStreamer = Streamer.getStreamer(Class.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize ReflectiveDelta class", e);
        }
    }
}
