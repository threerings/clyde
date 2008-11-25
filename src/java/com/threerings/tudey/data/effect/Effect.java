//
// $Id$

package com.threerings.tudey.data.effect;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.EffectConfig;

/**
 * Represents a stateless event occurring within the scene.
 */
public class Effect extends SimpleStreamableObject
{
    /**
     * Creates a new effect.
     */
    public Effect (
        ConfigReference<EffectConfig> config, int timestamp, Vector2f translation, float rotation)
    {
        _config = config;
        _timestamp = timestamp;
        _translation.set(translation);
        _rotation = rotation;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Effect ()
    {
    }

    /**
     * Gives the effect a chance to resolve and cache configuration data after being created or
     * deserialized.
     */
    public void init (ConfigManager cfgmgr)
    {
        EffectConfig config = cfgmgr.getConfig(EffectConfig.class, _config);
        _original = (config == null) ? null : config.getOriginal(cfgmgr);
        _original = (_original == null) ? NULL_ORIGINAL : _original;
    }

    /**
     * Returns the effect's config reference.
     */
    public ConfigReference<EffectConfig> getConfig ()
    {
        return _config;
    }

    /**
     * Returns the time at which the effect was fired.
     */
    public int getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * Returns a reference to the effect's translation vector.
     */
    public Vector2f getTranslation ()
    {
        return _translation;
    }

    /**
     * Returns the effect's rotation angle.
     */
    public float getRotation ()
    {
        return _rotation;
    }

    /**
     * Returns the time at which this effect expires.
     */
    public int getExpiry ()
    {
        return _timestamp + _original.lifespan;
    }

    /** The effect configuration. */
    protected ConfigReference<EffectConfig> _config;

    /** The time at which the effect was fired. */
    protected int _timestamp;

    /** The effect's translation. */
    protected Vector2f _translation = new Vector2f();

    /** The effect's rotation angle. */
    protected float _rotation;

    /** The cached config implementation. */
    protected transient EffectConfig.Original _original;

    /** Used when we can't resolve the effect config. */
    protected static final EffectConfig.Original NULL_ORIGINAL = new EffectConfig.Original();
}
