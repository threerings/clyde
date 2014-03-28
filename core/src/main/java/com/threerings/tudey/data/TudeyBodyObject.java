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

import javax.annotation.Generated;
import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

/**
 * Extends {@link BodyObject} with Tudey-specified data.
 */
public class TudeyBodyObject extends BodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>pawnId</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PAWN_ID = "pawnId";
    // AUTO-GENERATED: FIELDS END

    /** The name of the message posted by the server to force the client to perform a client
     * action. The message provides two arguments: the client action config and the
     * {@link EntityKey} of the source. */
    public static final String FORCE_CLIENT_ACTION = "forceClientAction";

    /** The id of the player's pawn. */
    public int pawnId;

    @Override
    public OccupantInfo createOccupantInfo (PlaceObject plobj)
    {
        return new TudeyOccupantInfo(this);
    }

    @Override
    public String toString ()
    {
        return who();
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>pawnId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPawnId (int value)
    {
        int ovalue = this.pawnId;
        requestAttributeChange(
            PAWN_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.pawnId = value;
    }
    // AUTO-GENERATED: METHODS END
}
