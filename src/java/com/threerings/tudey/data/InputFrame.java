//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Represents a single frame of user input.
 */
public class InputFrame extends SimpleStreamableObject
{
    /** Indicates that the user wants to move. */
    public static final byte MOVE = (byte)(1 << 0);

    /** Indicates that the user wants to strafe. */
    public static final byte STRAFE = (byte)(1 << 1);

    /**
     * Creates a new input frame.
     */
    public InputFrame (long timestamp, float direction, byte flags)
    {
        _timestamp = timestamp;
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
    public long getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Returns the direction requested by the user.
     */
    public float getDirection ()
    {
        return _direction;
    }

    /**
     * Returns the user's input flags.
     */
    public byte getFlags ()
    {
        return _flags;
    }

    /** The timestamp of the input frame. */
    protected long _timestamp;

    /** The direction requested by the user. */
    protected float _direction;

    /** The user's input flags. */
    protected byte _flags;
}
