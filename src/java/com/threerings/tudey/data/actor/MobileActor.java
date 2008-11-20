//
// $Id$

package com.threerings.tudey.data.actor;

import com.threerings.math.FloatMath;

/**
 * An actor capable of moving by itself.
 */
public abstract class MobileActor extends MovableActor
{
    /** A flag indicating that the actor is in motion. */
    public static final int MOVING = (1 << 1);

    /**
     * Creates a new mobile actor.
     */
    public MobileActor (int id, int created)
    {
        super(id, created);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public MobileActor ()
    {
    }

    /**
     * Sets the direction of motion.
     */
    public void setDirection (float direction)
    {
        _direction = direction;
    }

    /**
     * Returns the direction of motion.
     */
    public float getDirection ()
    {
        return _direction;
    }

    /**
     * Returns the actor's speed in units per second.
     */
    public abstract float getSpeed ();

    @Override // documentation inherited
    public Actor extrapolate (float elapsed, Actor result)
    {
        super.extrapolate(elapsed, result);

        // if moving, extrapolate based on direction and speed
        if (isSet(MOVING)) {
            MobileActor mresult = (MobileActor)result;
            float speed = getSpeed();
            mresult.getTranslation().addLocal(
                speed * FloatMath.cos(_direction),
                speed * FloatMath.sin(_direction));
        }
        return result;
    }

    /** The direction of motion. */
    protected float _direction;
}
