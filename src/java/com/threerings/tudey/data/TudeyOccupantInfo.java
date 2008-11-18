//
// $Id$

package com.threerings.tudey.data;

import com.threerings.crowd.data.OccupantInfo;

/**
 * Extends {@link OccupantInfo} to include the occupant's actor id, if any.
 */
public class TudeyOccupantInfo extends OccupantInfo
{
    /** The id of the occupant's actor within the scene, or 0 for none. */
    public int actorId;

    /**
     * Creates a new occupant info object.
     */
    public TudeyOccupantInfo (TudeyBodyObject body)
    {
        super(body);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public TudeyOccupantInfo ()
    {
    }
}
