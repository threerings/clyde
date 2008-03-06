//
// $Id$

package com.threerings.tudey.data;

/**
 * Represents an update to the global environment.
 */
public class EnvironmentUpdate extends TudeySceneUpdate
{
    /**
     * Creates a new environment update.
     */
    public EnvironmentUpdate (Environment environment)
    {
        _environment = environment;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public EnvironmentUpdate ()
    {
    }

    /**
     * Returns a reference to the environment object.
     */
    public Environment getEnvironment ()
    {
        return _environment;
    }

    /**
     * Applies this update to the scene.
     */
    public void apply (TudeySceneModel model)
    {
        model.setEnvironment(_environment);
    }

    /** The environment object to apply. */
    protected Environment _environment;
}
