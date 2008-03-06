//
// $Id$

package com.threerings.tudey.data;

import java.util.Collection;
import java.util.HashMap;

import com.threerings.util.PropertyConfig;

/**
 * Contains information about a prop.
 */
public class PropConfig extends PropertyConfig
{
    /** The prop model. */
    public String model;

    /** The radius of the prop's bounding cylinder. */
    public float radius;

    /** The height of the prop's bounding cylinder. */
    public float height;

    /** Whether or not actors can pass through the prop. */
    public boolean passable;

    /** Whether or not bullets can pass through the prop. */
    public boolean penetrable;

    /**
     * Returns the configuration of the named prop.
     */
    public static PropConfig getConfig (String name)
    {
        return _configs.get(name);
    }

    /**
     * Returns the configurations of all props.
     */
    public static Collection<PropConfig> getConfigs ()
    {
        return _configs.values();
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        model = getProperty("model", "props/" + name + "/model");
        radius = getProperty("radius", 0.5f);
        height = getProperty("height", 1f);
        passable = getProperty("passable", false);
        penetrable = passable || getProperty("penetrable", false);
    }

    /** Maps prop names to their configurations. */
    protected static HashMap<String, PropConfig> _configs =
        loadConfigs(PropConfig.class, "props/props.txt");
}
