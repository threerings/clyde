//
// $Id$

package com.threerings.tudey.data;

import java.util.Set;
import java.util.Properties;

import com.threerings.io.Streamable;

import com.threerings.export.Exportable;

import com.threerings.util.PropertyConfig;

/**
 * Superclass for configurations of scene elements derived from property files.
 */
public abstract class SceneElementConfig extends PropertyConfig
    implements Streamable, Exportable
{
    /**
     * Adds the client-side resources required by this configuration to the supplied set.  These
     * resources will be preloaded and pinned in the cache during loading.
     */
    public void getResources (Set<SceneResource> results)
    {
        for (String model : _preloadModels) {
            results.add(new SceneResource.Model(model));
        }
        for (String anim : _preloadAnims) {
            results.add(new SceneResource.Animation(anim));
        }
        for (String sound : _preloadSounds) {
            results.add(new SceneResource.Sound(sound));
        }
    }

    /**
     * Initializes this configuration with its name, (optional) id, and properties.
     */
    protected void init (String name, int id, Properties props)
    {
        super.init(name, id, props);

        // find the resources to preload
        _preloadModels = getProperty("preload_models", new String[0]);
        _preloadAnims = getProperty("preload_animations", new String[0]);
        _preloadSounds = getProperty("preload_sounds", new String[0]);
    }

    /** A list of models to preload along with the described object. */
    protected String[] _preloadModels;

    /** A list of animations to preload along with the described object. */
    protected String[] _preloadAnims;

    /** A list of sounds to preload along with the described object. */
    protected String[] _preloadSounds;
}
