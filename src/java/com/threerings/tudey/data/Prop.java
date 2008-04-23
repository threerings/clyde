//
// $Id$

package com.threerings.tudey.data;

/**
 * A static piece of scenery.
 */
public class Prop extends Placeable
{
    /**
     * Creates a new prop of the specified type.
     */
    public Prop (String type)
    {
        _type = type;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Prop ()
    {
    }

    /**
     * Returns the type of this prop.
     */
    public String getType ()
    {
        return _type;
    }

    /**
     * Returns the configuration of this prop.
     */
    public PropConfig getConfig ()
    {
        return PropConfig.getConfig(_type);
    }

    /**
     * Checks whether the configuration of this prop is valid.
     */
    public boolean isValid ()
    {
        return getConfig() != null;
    }

    @Override // documentation inherited
    public void getResources (java.util.Set<SceneResource> results)
    {
        getConfig().getResources(results);
    }

    /** The prop type. */
    protected String _type;
}
