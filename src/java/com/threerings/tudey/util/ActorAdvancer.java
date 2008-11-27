//
// $Id$

package com.threerings.tudey.util;

import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.shape.Shape;

/**
 * Used on the client and the server to advance the state of an actor based on its state and
 * surroundings.
 */
public class ActorAdvancer
{
    /**
     * Provides a callback mechanism to allow the advancer to query the pawn's environment while
     * advancing it.
     */
    public interface Environment
    {
        /**
         * Checks whether the actor is colliding with anything and, if it is, populates the
         * provided object with the penetration vector (the minimum translation required to
         * cancel the penetration).
         *
         * @return true if a collision was detected (in which case the result vector will be
         * populated), false otherwise.
         */
        public boolean getPenetration (Actor actor, Shape shape, Vector2f result);
    }

    /**
     * Creates a new advancer for the supplied actor.
     */
    public ActorAdvancer (Environment environment, Actor actor, int timestamp)
    {
        _environment = environment;
        init(actor, timestamp);
    }

    /**
     * (Re)initializes the advancer.
     */
    public void init (Actor actor, int timestamp)
    {
        _actor = actor;
        _timestamp = timestamp;
    }

    /**
     * Advances the actor to the specified timestamp.
     */
    public void advance (int timestamp)
    {
        if (timestamp <= _timestamp) {
            return;
        }
        float elapsed = (timestamp - _timestamp) / 1000f;
        _timestamp = timestamp;

        // take a step
        step(elapsed);
    }

    /**
     * Takes an Euler step of the specified length.
     */
    protected void step (float elapsed)
    {
        // nothing by default
    }

    /**
     * Updates the stored shape.
     */
    protected void updateShape ()
    {
        _transform.set(_actor.getTranslation(), _actor.getRotation(), 1f);
        _shape = _actor.getOriginal().shape.getShape().transform(_transform, _shape);
    }

    /** The actor's environment. */
    protected Environment _environment;

    /** The current actor state. */
    protected Actor _actor;

    /** The timestamp at which the state is valid. */
    protected int _timestamp;

    /** The shape of the actor. */
    protected Shape _shape;

    /** Used to store the actor's transform. */
    protected Transform2D _transform = new Transform2D(Transform2D.UNIFORM);
}
