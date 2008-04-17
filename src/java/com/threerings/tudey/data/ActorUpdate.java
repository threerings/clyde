//
// $Id$

package com.threerings.tudey.data;

import com.threerings.util.StreamableArrayList;

/**
 * Represents an update to the actor set.
 */
public class ActorUpdate extends StreamableArrayList<Actor>
{
    /**
     * Applies the update.
     */
    public void apply (ActorSet actors)
    {
        for (Actor actor : this) {
            actors.update(actor);
        }
    }
}
