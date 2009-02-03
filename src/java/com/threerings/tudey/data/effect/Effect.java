//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
