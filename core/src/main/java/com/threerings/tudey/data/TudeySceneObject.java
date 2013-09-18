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
import com.threerings.crowd.data.OccupantInfo;

import com.threerings.whirled.data.SceneObject;

/**
 * Extends the {@link SceneObject} with information specific to Tudey scenes.
 */
public class TudeySceneObject extends SceneObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>tudeySceneService</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String TUDEY_SCENE_SERVICE = "tudeySceneService";
    // AUTO-GENERATED: FIELDS END

    /** Provides Tudey scene services. */
    public TudeySceneMarshaller tudeySceneService;

    /**
     * Returns the id of the pawn controlled by the client with the provided oid, or 0 if none.
     */
    public int getPawnId (int cloid)
    {
        TudeyOccupantInfo info = (TudeyOccupantInfo)occupantInfo.get(cloid);
        return (info == null) ? 0 : info.pawnId;
    }

    /**
     * Returns the id of the first pawn in the occupant list.
     */
    public int getFirstPawnId ()
    {
        for (OccupantInfo info : occupantInfo) {
            int pawnId = ((TudeyOccupantInfo)info).pawnId;
            if (pawnId > 0) {
                return pawnId;
            }
        }
        return 0;
    }

    /**
     * Returns the occupant info corresponding to the specified pawn, or <code>null</code> if
     * no such occupant info is registered.
     */
    public TudeyOccupantInfo getOccupantInfo (int pawnId)
    {
        for (OccupantInfo info : occupantInfo) {
            TudeyOccupantInfo tinfo = (TudeyOccupantInfo)info;
            if (tinfo.pawnId == pawnId) {
                return tinfo;
            }
        }
        return null;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>tudeySceneService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setTudeySceneService (TudeySceneMarshaller value)
    {
        TudeySceneMarshaller ovalue = this.tudeySceneService;
        requestAttributeChange(
            TUDEY_SCENE_SERVICE, value, ovalue);
        this.tudeySceneService = value;
    }
    // AUTO-GENERATED: METHODS END
}
