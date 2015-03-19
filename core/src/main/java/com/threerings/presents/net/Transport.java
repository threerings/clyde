//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.presents.net;

import com.samskivert.util.HashIntMap;

/**
 * Message transport parameters.  These include the type of transport and the channel (used to
 * define independent streams for ordered transport), and may eventually include message priority,
 * etc.
 */
public class Transport
{
    /**
     * The available types of transport.
     */
    public enum Type
    {
        /**
         * Messages are neither guaranteed to arrive nor, if they do arrive, to arrive in order
         * and without duplicates.  Functionally identical to UDP.
         */
        UNRELIABLE_UNORDERED(false, false) {
            @Override
            public Type combine (Type other) {
                return other; // we defer to all
            }
        },

        /**
         * Messages are not guaranteed to arrive, but if they do arrive, then they will arrive in
         * order and without duplicates.  In other words, out-of-order packets will be dropped.
         */
        UNRELIABLE_ORDERED(false, true) {
            @Override
            public Type combine (Type other) {
                return other.isReliable() ? RELIABLE_ORDERED : this;
            }
        },

        /**
         * Messages are guaranteed to arrive eventually, but they are not guaranteed to arrive in
         * order.
         */
        RELIABLE_UNORDERED(true, false) {
            @Override
            public Type combine (Type other) {
                return other.isOrdered() ? RELIABLE_ORDERED : this;
            }
        },

        /**
         * Messages are guaranteed to arrive, and will arrive in the order in which they are sent.
         * Functionally identical to TCP.
         */
        RELIABLE_ORDERED(true, true) {
            @Override
            public Type combine (Type other) {
                return this; // we override all
            }
        };

        /**
         * Checks whether this transport type guarantees that messages will be delivered.
         */
        public boolean isReliable ()
        {
            return _reliable;
        }

        /**
         * Checks whether this transport type guarantees that messages will be received in the
         * order in which they were sent, if they are received at all.
         */
        public boolean isOrdered ()
        {
            return _ordered;
        }

        /**
         * Returns a transport type that combines the requirements of this type with those of the
         * specified other type.
         */
        public abstract Type combine (Type other);

        Type (boolean reliable, boolean ordered)
        {
            _reliable = reliable;
            _ordered = ordered;
        }

        protected boolean _reliable, _ordered;
    }

    /** The unreliable/unordered mode of transport. */
    public static final Transport UNRELIABLE_UNORDERED = getInstance(Type.UNRELIABLE_UNORDERED);

    /** The unreliable/ordered mode on the default channel. */
    public static final Transport UNRELIABLE_ORDERED = getInstance(Type.UNRELIABLE_ORDERED, 0);

    /** The reliable/unordered mode. */
    public static final Transport RELIABLE_UNORDERED = getInstance(Type.RELIABLE_UNORDERED);

    /** The reliable/ordered mode on the default channel. */
    public static final Transport RELIABLE_ORDERED = getInstance(Type.RELIABLE_ORDERED, 0);

    /** The default mode of transport. */
    public static final Transport DEFAULT = RELIABLE_ORDERED;

    /**
     * Returns the shared instance with the specified parameters.
     */
    public static Transport getInstance (Type type)
    {
        return getInstance(type, 0);
    }

    /**
     * Returns the shared instance with the specified parameters.
     */
    public static Transport getInstance (Type type, int channel)
    {
        // were there more parameters in transport objects, it would be better to have a single map
        // of instances and use Transport objects as keys (as in examples of the flyweight
        // pattern).  however, doing it this way avoids the need to create a new object on lookup
        if (_unordered == null) {
            int length = Type.values().length;
            _unordered = new Transport[length];
            @SuppressWarnings({ "unchecked" }) HashIntMap<Transport>[] ordered =
                (HashIntMap<Transport>[])new HashIntMap<?>[length];
            _ordered = ordered;
        }

        // for unordered transport, we map on the type alone
        int idx = type.ordinal();
        if (!type.isOrdered()) {
            Transport instance = _unordered[idx];
            if (instance == null) {
                _unordered[idx] = instance = new Transport(type);
            }
            return instance;
        }

        // for ordered transport, we map on the type and channel
        HashIntMap<Transport> instances = _ordered[idx];
        if (instances == null) {
            _ordered[idx] = instances = new HashIntMap<Transport>();
        }
        Transport instance = instances.get(channel);
        if (instance == null) {
            instances.put(channel, instance = new Transport(type, channel));
        }
        return instance;
    }

    /**
     * Returns the type of transport.
     */
    public Type getType ()
    {
        return _type;
    }

    /**
     * Returns the transport channel.
     */
    public int getChannel ()
    {
        return _channel;
    }

    /**
     * Checks whether this transport guarantees that messages will be delivered.
     */
    public boolean isReliable ()
    {
        return _type.isReliable();
    }

    /**
     * Checks whether this transport guarantees that messages will be received in the order in
     * which they were sent, if they are received at all.
     */
    public boolean isOrdered ()
    {
        return _type.isOrdered();
    }

    /**
     * Returns a transport that satisfies the requirements of this and the specified other
     * transport.
     */
    public Transport combine (Transport other)
    {
        // if the channels are different, we fall back to the default channel
        return getInstance(
            _type.combine(other._type),
            (_channel == other._channel) ? _channel : 0);
    }

    @Override
    public int hashCode ()
    {
        return 31*_type.hashCode() + _channel;
    }

    @Override
    public boolean equals (Object other)
    {
        Transport otrans;
        return other instanceof Transport && (otrans = (Transport)other)._type == _type &&
            otrans._channel == _channel;
    }

    @Override
    public String toString ()
    {
        return "[type=" + _type + ", channel=" + _channel + "]";
    }

    protected Transport (Type type)
    {
        this(type, 0);
    }

    protected Transport (Type type, int channel)
    {
        _type = type;
        _channel = channel;
    }

    /** The type of transport. */
    protected Type _type;

    /** The transport channel. */
    protected int _channel;

    /** Unordered instances mapped by type (would use {@link java.util.EnumMap}, but it doesn't
     * work with Retroweaver). */
    protected static Transport[] _unordered;

    /** Ordered instances mapped by type and channel. */
    protected static HashIntMap<Transport>[] _ordered;
}
