//
// $Id$

package com.threerings.tudey.data;

/**
 * An action to take on triggering a sensor.
 */
public abstract class Action extends SceneElement
{
    /**
     * Returns the name of the class to use on the server to execute this action.
     */
    public abstract String getLogicClassName ();
}
