//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * The superclass of the tudey scene updates.
 */
public abstract class TudeySceneUpdate extends SimpleStreamableObject
{
    /**
     * Applies this update to the scene.
     */
    public abstract void apply (TudeySceneModel model);
}
