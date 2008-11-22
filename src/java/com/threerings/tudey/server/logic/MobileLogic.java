//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;

/**
 * Controls the state of a mobile actor.
 */
public class MobileLogic extends ActorLogic
{
    @Override // documentation inherited
    protected Actor createActor (
        ConfigReference<ActorConfig> ref, int id, int timestamp,
        Vector2f translation, float rotation)
    {
        return new Mobile(ref, id, timestamp, translation, rotation);
    }
}
