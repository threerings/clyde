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

package com.threerings.tudey.data.actor;

import com.threerings.io.Streamable;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.delta.Deltable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;
import com.threerings.util.DeepObject;
import com.threerings.util.Shallow;

import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents an active, stateful element of the scene.
 */
public class Actor extends DeepObject
    implements Streamable, Deltable
{
    /** A flag indicating that the actor has changed its position in a discontinuous fashion. */
    public static final int WARP = (1 << 0);

    /** The value of the last flag defined in this class. */
    public static final int LAST_FLAG = WARP;

    /**
     * Creates a new actor.
     */
    public Actor (
        ConfigReference<ActorConfig> config, int id, int created,
        Vector2f translation, float rotation)
    {
        _config = config;
        _id = id;
        _created = created;
        _translation.set(translation);
        _rotation = rotation;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Actor ()
    {
        // these will be set later
        _id = 0;
        _created = 0;
    }

    /**
     * Gives the actor a chance to resolve and cache configuration data after being created or
     * deserialized.
     */
    public void init (ConfigManager cfgmgr)
    {
        ActorConfig config = cfgmgr.getConfig(ActorConfig.class, _config);
        _original = (config == null) ? null : config.getOriginal(cfgmgr);
        _original = (_original == null) ? NULL_ORIGINAL : _original;
    }

    /**
     * Adds the resources to preload for this actor into the provided set.
     */
    public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
    {
        preloads.add(new Preloadable.Config(ActorConfig.class, _config));
        _original.getPreloads(cfgmgr, preloads);
    }

    /**
     * Sets the actor's config reference.
     */
    public void setConfig (ConfigReference<ActorConfig> config)
    {
        _config = config;
    }

    /**
     * Returns the actor's config reference.
     */
    public ConfigReference<ActorConfig> getConfig ()
    {
        return _config;
    }

    /**
     * Returns a reference to the cached config implementation.
     */
    public ActorConfig.Original getOriginal ()
    {
        return _original;
    }

    /**
     * Returns the actor's unique identifier.
     */
    public int getId ()
    {
        return _id;
    }

    /**
     * Returns the timestamp at which the actor was created.
     */
    public int getCreated ()
    {
        return _created;
    }

    /**
     * Sets the timestamp at which the actor was destroyed.
     */
    public void setDestroyed (int destroyed)
    {
        _destroyed = destroyed;
    }

    /**
     * Returns the timestamp at which the actor was destroyed, or {@link Integer#MAX_VALUE} if
     * not yet destroyed.
     */
    public int getDestroyed ()
    {
        return _destroyed;
    }

    /**
     * Returns a reference to the actor's translation vector.
     */
    public Vector2f getTranslation ()
    {
        return _translation;
    }

    /**
     * Sets the actor's rotation angle.
     */
    public void setRotation (float rotation)
    {
        _rotation = rotation;
    }

    /**
     * Returns the actor's rotation angle.
     */
    public float getRotation ()
    {
        return _rotation;
    }

    /**
     * Sets the actor's flags.
     */
    public void setFlags (int flags)
    {
        _flags = flags;
    }

    /**
     * Returns the actor's flags.
     */
    public int getFlags ()
    {
        return _flags;
    }

    /**
     * Sets a flag.
     */
    public void set (int flag)
    {
        _flags |= flag;
    }

    /**
     * Clears a flag.
     */
    public void clear (int flag)
    {
        _flags &= ~flag;
    }

    /**
     * Determines whether a flag is set.
     */
    public boolean isSet (int flag)
    {
        return (_flags & flag) != 0;
    }

    /**
     * Interpolates between the state of this actor and that of the specified other, placing the
     * result in the provided object.
     *
     * @param start the start time (the timestamp of this actor).
     * @param end the end time (the timestamp of the other actor).
     * @param timestamp the desired time (the timestamp of the result).
     */
    public Actor interpolate (Actor other, int start, int end, int timestamp, Actor result)
    {
        // start by deep-copying this actor
        copy(result);

        // interpolate translation and rotation unless warped
        if (!other.isSet(WARP)) {
            float t = (float)(timestamp - start) / (end - start);
            _translation.lerp(other.getTranslation(), t, result.getTranslation());
            result.setRotation(FloatMath.lerpa(_rotation, other.getRotation(), t));
        }
        return result;
    }

    /**
     * Extrapolates the state of this actor after the specified time interval, in seconds (which
     * may be negative).
     */
    public Actor extrapolate (float elapsed, Actor result)
    {
        return (Actor)copy(result);
    }

    /**
     * Determines whether this actor is controlled by the client and, if so, creates and returns an
     * {@link ActorAdvancer} instance to perform client-side prediction.  Returns <code>null</code>
     * if not controlled by the client.
     */
    public ActorAdvancer maybeCreateAdvancer (TudeyContext ctx, TudeySceneView view, int timestamp)
    {
        return null;
    }

    /**
     * Determines whether this actor should be checked for collisions with the specified other actor.
     */
    public boolean canCollide (Actor oactor)
    {
        return _id != oactor.getId() && canCollide(oactor.getCollisionFlags());
    }

    /**
     * Determines whether this actor should be checked for collisions with scene elements with the
     * specified flags.
     */
    public boolean canCollide (int flags)
    {
        return (getCollisionMask() & flags) != 0;
    }

    /**
     * Returns the actor's collision flags.
     */
    public int getCollisionFlags ()
    {
        return _original.collisionFlags;
    }

    /**
     * Returns the actor's collision mask.
     */
    public int getCollisionMask ()
    {
        return _original.collisionMask;
    }

    @Override // documentation inherited
    public String toString ()
    {
        return "[config=" + _config + ", id=" + _id + "]";
    }

    /** The actor's configuration reference. */
    protected ConfigReference<ActorConfig> _config;

    /** The actor's unique identifier. */
    protected final int _id;

    /** The timestamp at which the actor was created. */
    protected final int _created;

    /** The timestamp at which the actor was destroyed. */
    protected int _destroyed = Integer.MAX_VALUE;

    /** The actor's translation. */
    protected Vector2f _translation = new Vector2f();

    /** The actor's rotation angle. */
    protected float _rotation;

    /** Various flags. */
    protected int _flags;

    /** The cached config implementation. */
    @Shallow
    protected transient ActorConfig.Original _original;

    /** Used when we can't resolve the actor config. */
    protected static final ActorConfig.Original NULL_ORIGINAL = new ActorConfig.Original();
}
