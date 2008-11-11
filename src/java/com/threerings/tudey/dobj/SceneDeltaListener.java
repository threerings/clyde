//
// $Id$

package com.threerings.tudey.dobj;

import com.threerings.presents.dobj.ChangeListener;

/**
 * Notifies objects when a scene delta has been received.
 */
public interface SceneDeltaListener extends ChangeListener
{
    /**
     * Called when a scene delta has been received.
     */
    public void sceneDeltaReceived (SceneDeltaEvent event);
}
