//
// $Id$

package com.threerings.tudey.data;

import java.util.Collection;
import java.util.Set;

import com.samskivert.util.HashIntMap;

/**
 * Contains information about a block.
 */
public class BlockConfig extends SceneElementConfig
{
    /** The name of the server-side logic class for blocks of this type. */
    public String logic;

    /** The block model. */
    public String model;

    /** The block model variant. */
    public String variant;

    /** Whether or not actors can pass through blocks of this type. */
    public boolean passable;

    /** Whether or not projectiles can pass through blocks of this type. */
    public boolean penetrable;

    /**
     * Returns the configuration of the identified block.
     */
    public static BlockConfig getConfig (int id)
    {
        return _configs.get(id);
    }

    /**
     * Returns the configurations of all blocks.
     */
    public static Collection<BlockConfig> getConfigs ()
    {
        return _configs.values();
    }

    @Override // documentation inherited
    public void getResources (Set<SceneResource> results)
    {
        super.getResources(results);
        results.add(new SceneResource.Model(model, variant));
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        logic = getProperty("logic");
        model = getProperty("model", "world/dynamic/block/" + name + "/model");
        variant = getProperty("variant");
        passable = getProperty("passable", false);
        penetrable = passable || getProperty("penetrable", false);
    }

    /** Maps block ids to their configurations. */
    protected static HashIntMap<BlockConfig> _configs = loadConfigs(
        BlockConfig.class, "world/dynamic/block/list.txt", "world/dynamic/block/ids.properties");
}
