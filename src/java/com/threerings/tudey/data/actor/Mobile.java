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

    /** A flag indicating that the actor is interacting. */
    public static final int INTERACTING = (1 << 2);

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
     * Returns the (base) speed of the actor.
     */
    public float getSpeed ()
    {
        return ((ActorConfig.Mobile)_original).speed;
    }

    /**
     * Sets the action timestamp.
     */
    public void setActed (int acted)
    {
        _acted = acted;
    }

    /**
     * Returns the action timestamp.
     */
    public int getActed ()
    {
        return _acted;
    }

    /**
     * Takes an Euler step of the specified duration.
     */
    public void step (float elapsed)
    {
        if (isSet(MOVING)) {
            float length = getSpeed() * elapsed;
            _translation.addLocal(
                length * FloatMath.cos(_direction),
                length * FloatMath.sin(_direction));
        }
    }

    @Override // documentation inherited
    public Actor extrapolate (float elapsed, Actor result)
    {
        super.extrapolate(elapsed, result);

        // take a step of the indicated duration
        ((Mobile)result).step(elapsed);

        return result;
    }

    /** The direction of motion. */
    protected float _direction;

    /** The action timestamp. */
    protected int _acted;
}
