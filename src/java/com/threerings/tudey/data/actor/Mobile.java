//
// $Id$

package com.threerings.tudey.data.actor;

import com.threerings.config.ConfigReference;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.ActorConfig;

/**
 * An actor capable of moving by itself.
 */
public class Mobile extends Actor
{
    /** A flag indicating that the actor is in motion. */
    public static final int MOVING = (1 << 1);

    /**
     * Creates a new mobile actor.
     */
    public Mobile (
        ConfigReference<ActorConfig> config, int id, int created,
        Vector2f translation, float rotation)
    {
        super(config, id, created, translation, rotation);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Mobile ()
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
    public float getSpeed ()
    {
        return 6f;
    }

    @Override // documentation inherited
    public Actor extrapolate (float elapsed, Actor result)
    {
        super.extrapolate(elapsed, result);

        // if moving, extrapolate based on direction and speed
        if (isSet(MOVING)) {
            Mobile mresult = (Mobile)result;
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
