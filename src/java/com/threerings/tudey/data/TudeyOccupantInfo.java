//
// $Id$

package com.threerings.tudey.data;

import com.threerings.crowd.data.OccupantInfo;

/**
 * Extends {@link OccupantInfo} to include the occupant's pawn id, if any.
 */
public class TudeyOccupantInfo extends OccupantInfo
{
    /** The id of the actor controlled by the occupant, or 0 for none. */
    public int pawnId;

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
