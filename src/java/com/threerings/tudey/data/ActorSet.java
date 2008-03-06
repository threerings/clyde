//
// $Id$

package com.threerings.tudey.data;

import com.threerings.presents.dobj.DSet;

/**
 * A set of actors.  Allows direct manipulation of the set's contents.
 */
public class ActorSet extends DSet<Actor>
{
    @Override // documentation inherited
    public boolean add (Actor actor)
    {
        return super.add(actor);
    }

    @Override // documentation inherited
    public boolean remove (Actor actor)
    {
        return super.remove(actor);
    }

    @Override // documentation inherited
    public Actor removeKey (Comparable key)
    {
        return super.removeKey(key);
    }

    @Override // documentation inherited
    public Actor update (Actor actor)
    {
        return super.update(actor);
    }
}
