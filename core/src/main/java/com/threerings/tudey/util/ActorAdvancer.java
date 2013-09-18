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

package com.threerings.tudey.util;

import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.shape.Shape;

/**
 * Used on the client and the server to advance the state of an actor based on its state and
 * surroundings.
 */
public class ActorAdvancer
{
    /**
     * Provides a callback mechanism to allow the advancer to query the actor's environment while
     * advancing it.
     */
    public interface Environment
    {
        /**
         * Returns a reference to the scene model.
         */
        public TudeySceneModel getSceneModel ();

        /**
         * Checks whether the actor is colliding with anything and, if it is, populates the
         * provided object with the penetration vector (the minimum translation required to
         * cancel the penetration).
         *
         * @return true if a collision was detected (in which case the result vector will be
         * populated), false otherwise.
         */
        public boolean getPenetration (Actor actor, Shape shape, Vector2f result);

        /**
         * Checks whether the actor is colliding with anything.
         */
        public boolean collides (Actor actor, Shape shape);

        /**
         * Checks the specified mask for a collision.
         */
        public boolean collides (int mask, Shape shape);

        /**
         * Returns the direction flags affecting the actor.
         */
        public int getDirections (Actor actor, Shape shape);
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
     * Returns a reference to the advancer actor.
     */
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * Returns the advancer timestamp.
     */
    public int getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * (Re)initializes the advancer.
     */
    public void init (Actor actor, int timestamp)
    {
        _actor = actor;
        _timestamp = timestamp;
        updateShape();
    }

    /**
     * Transfers state from the specified source advancer.
     */
    public void transfer (ActorAdvancer source)
    {
        _timestamp = source._timestamp;
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
     * Jumps to the specified timestamp without actually taking a step.
     */
    public void jump (int timestamp)
    {
        _timestamp = Math.max(_timestamp, timestamp);
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
        _shape = _actor.getOriginal().getShape(_environment.getSceneModel().getConfigManager())
            .getShape().transform(_transform, _shape);
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
