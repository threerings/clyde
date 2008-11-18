//
// $Id$

package com.threerings.tudey.data;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

/**
 * Extends {@link BodyObject} with Tudey-specified data.
 */
public class TudeyBodyObject extends BodyObject
{
    @Override // documentation inherited
    public OccupantInfo createOccupantInfo (PlaceObject plobj)
    {
        return new TudeyOccupantInfo(this);
    }
}
