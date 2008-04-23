//
// $Id$

package com.threerings.tudey.data;

/**
 * A static element of the scene.
 */
public class Placeable extends SceneElement
{
    /**
     * Initializes the placeable.  Should only be called on the server.
     */
    public void init (int placeableId)
    {
        _placeableId = placeableId;
    }

    /**
     * Returns the placeable's unique identifier.
     */
    public int getPlaceableId ()
    {
        return _placeableId;
    }

    /**
     * Checks whether the configuration of this object is valid.
     */
    public boolean isValid ()
    {
        return true;
    }

    /** Uniquely identifies the placeable in the scene. */
    protected int _placeableId;
}
