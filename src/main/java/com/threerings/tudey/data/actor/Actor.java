//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
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
import com.threerings.delta.DeltaFinal;
import com.threerings.delta.Deltable;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

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
        if (preloads.add(new Preloadable.Config(ActorConfig.class, _config))) {
            _original.getPreloads(cfgmgr, preloads);
        }
    }

    /**
     * Sets the actor's config reference.
     */
    public void setConfig (ConfigReference<ActorConfig> config)
    {
        _config = config;
        setDirty(true);
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
        setDirty(true);
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
     * Sets the actor's translation and its dirty flag.
     */
    public void setTranslation (float x, float y)
    {
        _translation.set(x, y);
        setDirty(true);
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
        setDirty(true);
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
        setDirty(true);
    }

    /**
     * Returns the actor's flags.
     */
    public int getFlags ()
    {
        return _flags;
    }

    /**
     * Sets or clears a flag.
     */
    public void set (int flag, boolean value)
    {
        _flags = value ? (_flags | flag) : (_flags & ~flag);
        setDirty(true);
    }

    /**
     * Sets a flag.
     */
    public void set (int flag)
    {
        _flags |= flag;
        setDirty(true);
    }

    /**
     * Clears a flag.
     */
    public void clear (int flag)
    {
        _flags &= ~flag;
        setDirty(true);
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
    public Actor extrapolate (float elapsed, int timestamp, Actor result)
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
        return isClientControlled(ctx, view) ? createAdvancer(view, timestamp) : null;
    }

    /**
     * Checks whether this actor is client controlled.
     */
    public boolean isClientControlled (TudeyContext ctx, TudeySceneView view)
    {
        return _id < 0;
    }

    /**
     * Creates an advancer for the actor.
     */
    public ActorAdvancer createAdvancer (ActorAdvancer.Environment environment, int timestamp)
    {
        return new ActorAdvancer(environment, this, timestamp);
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
        return isSet(WARP) ? _original.spawnAdjustMask : _original.collisionMask;
    }

    /**
     * Returns if the actor is affected by direction flags.
     */
    public boolean directionAffected ()
    {
        return false;
    }

    /**
     * Sets the state of the actor's dirty flag.
     */
    public void setDirty (boolean dirty)
    {
        _dirty = dirty;
    }

    /**
     * Returns the state of the actor's dirty flag.
     */
    public boolean isDirty ()
    {
        return _dirty;
    }

    @Override
    public Object copy (Object dest)
    {
        // we handle base class fields "manually" for performance reasons, since they get called
        // often and the reflective implementation is comparatively slow
        Actor result = (Actor)super.copy(dest);
        result._config = _config.clone();
        result._id = _id;
        result._created = _created;
        result._destroyed = _destroyed;
        result._translation.set(_translation);
        result._rotation = _rotation;
        result._flags = _flags;
        result._original = _original;
        result.setDirty(true);
        return result;
    }

    @Override
    public boolean equals (Object other)
    {
        if (!super.equals(other)) {
            return false;
        }
        Actor oactor = (Actor)other;
        return _config.equals(oactor._config) &&
            _id == oactor._id &&
            _created == oactor._created &&
            _destroyed == oactor._destroyed &&
            _translation.equals(oactor._translation) &&
            _rotation == oactor._rotation &&
            _flags == oactor._flags &&
            _original == oactor._original;
    }

    @Override
    public int hashCode ()
    {
        int hash = super.hashCode();
        hash = 31*hash + _config.hashCode();
        hash = 31*hash + _id;
        hash = 31*hash + _created;
        hash = 31*hash + _destroyed;
        hash = 31*hash + _translation.hashCode();
        hash = 31*hash + Float.floatToIntBits(_rotation);
        hash = 31*hash + _flags;
        hash = 31*hash + System.identityHashCode(_original);
        return hash;
    }

    @Override
    public String toString ()
    {
        return "[config=" + _config + ", id=" + _id + "]";
    }

    /** The actor's configuration reference. */
    @DeepOmit
    protected ConfigReference<ActorConfig> _config;

    /** The actor's unique identifier. */
    @DeepOmit
    @DeltaFinal
    protected int _id;

    /** The timestamp at which the actor was created. */
    @DeepOmit
    @DeltaFinal
    protected int _created;

    /** The timestamp at which the actor was destroyed. */
    @DeepOmit
    protected int _destroyed = Integer.MAX_VALUE;

    /** The actor's translation. */
    @DeepOmit
    protected Vector2f _translation = new Vector2f();

    /** The actor's rotation angle. */
    @DeepOmit
    protected float _rotation;

    /** Various flags. */
    @DeepOmit
    protected int _flags;

    /** The cached config implementation. */
    @DeepOmit
    protected transient ActorConfig.Original _original;

    /** A dirty flag set whenever we change the actor's state. */
    @DeepOmit
    protected transient boolean _dirty = true;

    /** Used when we can't resolve the actor config. */
    protected static final ActorConfig.Original NULL_ORIGINAL = new ActorConfig.Original();
}
