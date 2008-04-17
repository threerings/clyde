//
// $Id$

package com.threerings.tudey.data;

/**
 * An interface for places with two-dimensional content.
 */
public interface TudeyPlaceObject
{
    /**
     * Sets the place timestamp.
     */
    public void setTimestamp (long timestamp);

    /**
     * Retrieves the place timestamp.
     */
    public long getTimestamp ();

    /**
     * Pauses or unpauses the place clock.
     */
    public void setPaused (boolean paused);

    /**
     * Determines whether the clock is paused.
     */
    public boolean isPaused ();

    /**
     * Sets the scene descriptor.
     */
    public void setSceneDescriptor (SceneDescriptor scene);

    /**
     * Gets the scene descriptor used to construct the scene model.
     */
    public SceneDescriptor getSceneDescriptor ();

    /**
     * Sets the set of actors.
     */
    public void setActors (ActorSet actors);

    /**
     * Returns a reference to the set of actors.
     */
    public ActorSet getActors ();
}
