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

import com.threerings.io.Streamable;

import com.threerings.util.DeepObject;

/**
 * Represents a single frame of user input.
 */
public class InputFrame extends DeepObject
    implements Streamable
{
    /** Indicates that the user wants to move. */
    public static final int MOVE = (1 << 0);

    /** The value of the last flag defined in this class. */
    public static final int LAST_FLAG = MOVE;

    /**
     * Creates a new input frame.
     */
    public InputFrame (int timestamp, float rotation, float direction, int flags)
    {
        _timestamp = timestamp;
        _rotation = rotation;
        _direction = direction;
        _flags = flags;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public InputFrame ()
    {
    }

    /**
     * Returns the timestamp of this frame.
     */
    public int getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Returns the rotation requested by the user.
     */
    public float getRotation ()
    {
        return _rotation;
    }

    /**
     * Returns the direction of movement requested by the user.
     */
    public float getDirection ()
    {
        return _direction;
    }

    /**
     * Returns the user's input flags.
     */
    public int getFlags ()
    {
        return _flags;
    }

    /**
     * Determines whether a flag is set.
     */
    public boolean isSet (int flag)
    {
        return (_flags & flag) != 0;
    }

    /**
     * Returns the approximate size of the frame in bytes (including its two-byte class code).
     */
    public int getApproximateSize ()
    {
        return 18;
    }

    @Override
    public String toString ()
    {
        return "[timestamp=" + _timestamp + ", rotation=" + _rotation +
            ", direction=" + _direction + ", flags=" + _flags + "]";
    }

    /** The timestamp of the input frame. */
    protected int _timestamp;

    /** The rotation requested by the user. */
    protected float _rotation;

    /** The direction of movement requested by the user. */
    protected float _direction;

    /** The user's input flags. */
    protected int _flags;
}
