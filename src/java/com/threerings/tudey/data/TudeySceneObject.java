//
// $Id$

package com.threerings.tudey.data;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.whirled.data.SceneObject;

/**
 * Extends the {@link SceneObject} with information specific to Tudey scenes.
 */
public class TudeySceneObject extends SceneObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>tudeySceneService</code> field. */
    public static final String TUDEY_SCENE_SERVICE = "tudeySceneService";
    // AUTO-GENERATED: FIELDS END

    /** Provides Tudey scene services. */
    public TudeySceneMarshaller tudeySceneService;

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

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>tudeySceneService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTudeySceneService (TudeySceneMarshaller value)
    {
        TudeySceneMarshaller ovalue = this.tudeySceneService;
        requestAttributeChange(
            TUDEY_SCENE_SERVICE, value, ovalue);
        this.tudeySceneService = value;
    }
    // AUTO-GENERATED: METHODS END
}
