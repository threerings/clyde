//
// $Id$

package com.threerings.tudey.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.math.FloatMath;
import com.threerings.util.DeepUtil;

import com.threerings.tudey.client.ActorSprite;

/**
 * An active element of the scene.
 */
public abstract class Actor extends SceneElement
    implements Cloneable, DSet.Entry
{
    /** The presence state, used for extrapolation. */
    public enum PresenceState { PRE_ADDED, ADDED, PRESENT, REMOVED };

    /** The location of the actor. */
    public float x, y;

    /** The orientation of the actor (radians CCW from X+ about Z+). */
    public float orient;

    /** The actor's presence state. */
    public transient PresenceState pstate = PresenceState.PRESENT;

    /**
     * Initializes the actor.  Should only be called on the server.
     */
    public void init (int actorId)
    {
        _actorId = actorId;
    }

    /**
     * Returns the actor's unique identifier.
     */
    public int getActorId ()
    {
        return _actorId;
    }

    /**
     * Determines whether this actor is present (i.e., whether it is not an extrapolated
     * pre- or post-existence state).
     */
    public boolean isPresent ()
    {
        return pstate == PresenceState.ADDED || pstate == PresenceState.PRESENT;
    }

    /**
     * Creates a sprite to represent this actor in the scene.
     */
    public abstract ActorSprite createSprite ();

    /**
     * Returns the name of the server-side logic class for the actor.
     */
    public abstract String getLogicClassName ();

    /**
     * Interpolates in-place between the state of this and another actor.
     *
     * @return a reference to this actor, for chaining.
     */
    public Actor interpolateLocal (Actor other, float t)
    {
        return interpolate(other, t, this);
    }

    /**
     * Interpolates between the state of this and the specified other actor.
     *
     * @return a new object containing the result.
     */
    public Actor interpolate (Actor other, float t)
    {
        return interpolate(other, t, (Actor)clone());
    }

    /**
     * Interpolates between the state of this and the specified other actor, placing the
     * state in the provided result object.
     *
     * @return a reference to the result object, for chaining.
     */
    public Actor interpolate (Actor other, float t, Actor result)
    {
        float nx = FloatMath.lerp(x, other.x, t);
        float ny = FloatMath.lerp(y, other.y, t);
        float norient = FloatMath.lerpa(orient, other.orient, t);
        result.set(this);
        result.setPosition(nx, ny, norient);
        result.pstate = PresenceState.PRESENT;
        return result;
    }

    /**
     * Extrapolates the state of this actor in-place.
     *
     * @param t the number of seconds (positive or negative) to extrapolate into the
     * past/future.
     * @return a reference to this actor, for chaining.
     */
    public Actor extrapolateLocal (float t)
    {
        return extrapolate(t, this);
    }

    /**
     * Extrapolates the state of this actor.
     *
     * @param t the number of seconds (positive or negative) to extrapolate into the
     * past/future.
     * @return a new object containing the result.
     */
    public Actor extrapolate (float t)
    {
        return extrapolate(t, (Actor)clone());
    }

    /**
     * Extrapolates the state of this actor, placing the result in the object provided.
     *
     * @param t the number of seconds (positive or negative) to extrapolate into the
     * past/future.
     * @return a reference to the result object, for chaining.
     */
    public Actor extrapolate (float t, Actor result)
    {
        result.set(this);
        if (pstate == PresenceState.ADDED && t < 0f) {
            result.pstate = PresenceState.PRE_ADDED;
        } else if (pstate == PresenceState.REMOVED && t >= 0f) {
            result.pstate = PresenceState.REMOVED;
        } else {
            result.pstate = PresenceState.PRESENT;
        }
        return result;
    }

    /**
     * Copies the state of another actor (assumed to be of the same class).
     *
     * @return a reference to this actor, for chaining.
     */
    public Actor set (Actor other)
    {
        return DeepUtil.copy(other, this);
    }

    /**
     * Sets the actor's position (location and orientation).
     */
    public void setPosition (float x, float y, float orient)
    {
        this.x = x;
        this.y = y;
        this.orient = orient;
    }

    /**
     * Returns the distance to another actor.
     */
    public float getDistance (Actor actor)
    {
        return getDistance(actor.x, actor.y);
    }

    /**
     * Returns the distance to the specified location.
     */
    public float getDistance (float x, float y)
    {
        float dx = this.x - x, dy = this.y - y;
        return FloatMath.sqrt(dx*dx + dy*dy);
    }

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return _actorId;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return _actorId;
    }

    /** Uniquely identifies the actor. */
    protected int _actorId;
}
