//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.actor.MovableActor;

/**
 * Represents a movable actor.
 */
public abstract class MovableActorSprite extends ActorSprite
{
    /**
     * Creates a new movable actor sprite.
     */
    public MovableActorSprite (
        GlContext ctx, TudeySceneView view, int timestamp, MovableActor actor)
    {
        super(ctx, view, timestamp, actor);
        _mactor = (MovableActor)_actor;
    }

    @Override // documentation inherited
    protected void update ()
    {
        super.update();

        // set the model's transform based on the actor position
        Vector2f translation = _mactor.getTranslation();
        Transform3D transform = _model.getLocalTransform();
        Vector3f trans = transform.getTranslation();
        trans.set(translation.x, translation.y,
            _view.getFloorZ(translation.x, translation.y, trans.z));
        transform.getRotation().fromAngleAxis(_mactor.getRotation(), Vector3f.UNIT_Z);
        _model.updateBounds();
    }

    /** A casted reference to the play head actor. */
    protected MovableActor _mactor;
}
