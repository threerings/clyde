//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;
import com.threerings.math.Vector2f;

import com.threerings.opengl.mod.Model;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.ActorSpriteConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.util.ActorHistory;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents an active element of the scene.
 */
public class ActorSprite extends Sprite
    implements TudeySceneView.TickParticipant, ConfigUpdateListener<ActorConfig>
{
    /**
     * The actual sprite implementation.
     */
    public static abstract class Implementation extends SimpleScope
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Updates the implementation with new interpolated state.
         */
        public void update (Actor actor)
        {
            // nothing by default
        }

        /**
         * Notes that the actor was just created (as opposed to just being added).
         */
        public void wasCreated ()
        {
            // nothing by default
        }

        /**
         * Notes that the actor is about to be destroyed (as opposed to just being removed).
         */
        public void willBeDestroyed ()
        {
            // nothing by default
        }

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "impl";
        }
    }

    /**
     * Superclass of the original implementations.
     */
    public static class Original extends Implementation
    {
        /**
         * Creates a new implementation.
         */
        public Original (TudeyContext ctx, Scope parentScope, ActorSpriteConfig config)
        {
            super(parentScope);
            _ctx = ctx;

            // set the config
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (ActorSpriteConfig config)
        {
            _model.setConfig((_config = config).model);
        }

        @Override // documentation inherited
        public void update (Actor actor)
        {
            // update the model transform
            Vector2f translation = actor.getTranslation();
            _view.getFloorTransform(
                translation.x, translation.y, actor.getRotation(), _model.getLocalTransform());
            _model.updateBounds();
        }

        /** The renderer context. */
        protected TudeyContext _ctx;

        /** The sprite configuration. */
        protected ActorSpriteConfig _config;

        /** The model. */
        @Bound
        protected Model _model;

        /** The owning view. */
        @Bound
        protected TudeySceneView _view;
    }

    /**
     * Creates a new actor sprite.
     */
    public ActorSprite (TudeyContext ctx, TudeySceneView view, int timestamp, Actor actor)
    {
        super(ctx, view);

        // create the history and play head actor
        _history = new ActorHistory(timestamp, actor, view.getBufferDelay() * 2);
        int delayedTime = view.getDelayedTime();
        _actor = _history.get(delayedTime, (Actor)actor.clone());

        // create the model
        _model = new Model(ctx);

        // if the actor is created, add it immediately
        if (_history.isCreated(delayedTime)) {
            _view.getScene().add(_model);
            update();
        }

        // register as tick participant
        _view.addTickParticipant(this);
    }

    /**
     * Returns a reference to the "play head" actor containing interpolated state.
     */
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * Returns a reference to the sprite's model.
     */
    public Model getModel ()
    {
        return _model;
    }

    /**
     * Updates this sprite with new state.
     */
    public void update (int timestamp, Actor actor)
    {
        _history.record(timestamp, actor);
    }

    /**
     * Notes that the actor has been removed.
     */
    public void remove (int timestamp)
    {
        _removed = timestamp;
    }

    // documentation inherited from interface TudeySceneView.TickParticipant
    public boolean tick (int delayedTime)
    {
        // update the interpolated state
        _history.get(delayedTime, _actor);

        // handle pre-creation state
        if (_impl == null) {
            if (_history.isCreated(delayedTime)) {
                _view.getScene().add(_model);
                update();
                _impl.wasCreated();
            } else {
                return true; // chill until actually created
            }
        } else {
            update();
        }

        // see if we're destroyed/removed
        if (_history.isDestroyed(delayedTime)) {
            _impl.willBeDestroyed();
            dispose();
            return false;

        } else if (delayedTime >= _removed) {
            dispose();
            return false;

        } else {
            return true;
        }
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<ActorConfig> event)
    {
        updateFromConfig();
        _impl.update(_actor);
    }

    @Override // documentation inherited
    public void dispose ()
    {
        super.dispose();
        if (_impl != null) {
            _impl.dispose();
        }
        if (_config != null) {
            _config.removeListener(this);
        }
        _view.getScene().remove(_model);
    }

    /**
     * Updates the configuration and implementation of the sprite.
     */
    protected void update ()
    {
        setConfig(_actor.getConfig());
        _impl.update(_actor);
    }

    /**
     * Sets the configuration of this sprite.
     */
    protected void setConfig (ConfigReference<ActorConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(ActorConfig.class, ref));
    }

    /**
     * Sets the configuration of this sprite.
     */
    protected void setConfig (ActorConfig config)
    {
        if (_config == config) {
            return;
        }
        if (_config != null) {
            _config.removeListener(this);
        }
        if ((_config = config) != null) {
            _config.addListener(this);
        }
        updateFromConfig();
    }

    /**
     * Updates the sprite to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getSpriteImplementation(_ctx, this, _impl);
        nimpl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
        if (_impl != nimpl) {
            if (_impl != null) {
                _impl.dispose();
            }
            _impl = nimpl;
        }
    }

    /** The history that we use to find interpolated actor state. */
    protected ActorHistory _history;

    /** The "play head" actor with interpolated state. */
    protected Actor _actor;

    /** The actor configuration. */
    protected ActorConfig _config;

    /** The timestamp at which the actor was removed, if any. */
    protected int _removed = Integer.MAX_VALUE;

    /** The actor model. */
    @Bound
    protected Model _model;

    /** The actor implementation (<code>null</code> until actually created). */
    protected Implementation _impl;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
