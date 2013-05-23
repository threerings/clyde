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

package com.threerings.tudey.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Identifies an entity (scene entry or actor) within the scene.
 */
public abstract class EntityKey extends SimpleStreamableObject
{
    /**
     * Identifies an entry.
     */
    public static class Entry extends EntityKey
    {
        /**
         * Creates a new entry key.
         */
        public Entry (Object key)
        {
            _key = key;
        }

        /**
         * No-arg constructor for deserialization.
         */
        public Entry ()
        {
        }

        /**
         * Returns the entry key.
         */
        public Object getKey ()
        {
            return _key;
        }

        @Override
        public int hashCode ()
        {
            return _key.hashCode();
        }

        @Override
        public boolean equals (Object other)
        {
            return other instanceof Entry && ((Entry)other)._key.equals(_key);
        }

        @Override
        public String toString ()
        {
            return "[entryKey=" + _key + "]";
        }

        /** The entry key. */
        protected Object _key;
    }

    /**
     * Identifies an actor.
     */
    public static class Actor extends EntityKey
    {
        /**
         * Creates a new actor key.
         */
        public Actor (int id)
        {
            _id = id;
        }

        /**
         * No-arg constructor for deserialization.
         */
        public Actor ()
        {
        }

        /**
         * Returns the actor's unique identifier.
         */
        public int getId ()
        {
            return _id;
        }

        @Override
        public int hashCode ()
        {
            return _id;
        }

        @Override
        public boolean equals (Object other)
        {
            return other instanceof Actor && ((Actor)other)._id == _id;
        }

        @Override
        public String toString ()
        {
            return "[actorId=" + _id + "]";
        }

        /** The actor's unique identifier. */
        protected int _id;
    }
}
