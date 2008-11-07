//
// $Id$

package com.threerings.tudey.dobj;

/**
 * Notifies objects when a scene delta has been received.
 */
public interface SceneDeltaListener
{
    /**
     * Called when a scene delta has been received.
     */
    public void sceneDeltaReceived (SceneDeltaEvent event);
}
