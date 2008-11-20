//
// $Id$

package com.threerings.tudey.data.actor;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

/**
 * An actor with a translation and a rotation.
 */
public abstract class MovableActor extends Actor
{
    /** A flag indicating that the actor has changed its position in a discontinuous fashion. */
    public static final int WARP = (1 << 0);

    /**
     * Creates a new movable actor.
     */
    public MovableActor (int id, int created)
    {
        super(id, created);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public MovableActor ()
    {
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

    @Override // documentation inherited
    public Actor interpolate (Actor other, float t, Actor result)
    {
        super.interpolate(other, t, result);

        // interpolate translation and rotation unless warped
        MovableActor mother = (MovableActor)other, mresult = (MovableActor)result;
        if (!mother.isSet(WARP)) {
            _translation.lerp(mother.getTranslation(), t, mresult.getTranslation());
            mresult.setRotation(FloatMath.lerpa(_rotation, mother.getRotation(), t));
        }
        return result;
    }

    /** The actor's translation. */
    protected Vector2f _translation = new Vector2f();

    /** The actor's rotation angle. */
    protected float _rotation;

    /** Various flags. */
    protected int _flags;
}
