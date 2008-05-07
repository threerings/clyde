//
// $Id$

package com.threerings.packit.net;

import com.threerings.io.SimpleStreamableObject;

/**
 * A datagram transmitted through the {@link PackitSocket}.
 */
public class Datagram extends SimpleStreamableObject
{
    /** The sequence number of the datagram. */
    public int sequence;

    /** The acknowledgement number. */
    public int acknowledgment;

    /** The timestamp. */
    public int timestamp;

    /** The echo timestamp. */
    public int echoTimestamp;
}
