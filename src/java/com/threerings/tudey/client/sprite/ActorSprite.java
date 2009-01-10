//
// $Id$

package com.threerings.tudey.client.sprite;

import com.samskivert.util.RandomUtil;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Model;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.ActorSpriteConfig;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.shape.ShapeElement;
import com.threerings.tudey.util.ActorAdvancer;
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

        @Override // documentation inherited
        public void wasCreated ()
        {
            if (_config.creationTransient != null) {
                _view.getScene().spawnTransient(
                    _config.creationTransient, _model.getLocalTransform());
            }
        }

        @Override // documentation inherited
        public void willBeDestroyed ()
        {
            if (_config.destructionTransient != null) {
                _view.getScene().spawnTransient(
                    _config.destructionTransient, _model.getLocalTransform());
            }
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
     * Depicts a mobile actor with optional movement animations.
     */
    public static class Moving extends Original
    {
        /**
         * Creates a new implementation.
         */
        public Moving (TudeyContext ctx, Scope parentScope, ActorSpriteConfig.Moving config)
        {
            super(ctx, parentScope, config);
        }

        @Override // documentation inherited
        public void setConfig (ActorSpriteConfig config)
        {
            super.setConfig(config);
            ActorSpriteConfig.Moving mconfig = (ActorSpriteConfig.Moving)config;
            _idles = new Animation[mconfig.idles.length];
            for (int ii = 0; ii < _idles.length; ii++) {
                _idles[ii] = _model.getAnimation(mconfig.idles[ii].name);
            }
            _idleWeights = mconfig.getIdleWeights();

            _movements = new Animation[mconfig.movements.length][];
            for (int ii = 0; ii < _movements.length; ii++) {
                ActorSpriteConfig.MovementSet set = mconfig.movements[ii];
                _movements[ii] = new Animation[] {
                    _model.getAnimation(set.backward),
                    _model.getAnimation(set.right),
                    _model.getAnimation(set.forward),
                    _model.getAnimation(set.left)
                };
            }
        }

        @Override // documentation inherited
        public void update (Actor actor)
        {
            super.update(actor);

            // if we're moving, update our moving animation
            if (actor.isSet(Mobile.MOVING)) {
                Animation movement = getMovement((Mobile)actor);
                if (movement != null && !movement.isPlaying()) {
                    movement.start();
                    _currentIdle = null;
                }
            } else {
                if (_currentIdle == null || !_currentIdle.isPlaying()) {
                    (_currentIdle = getIdle()).start();
                }
            }
        }

        /**
         * Returns a random idle animation according to their weights, or returns <code>null</code>
         * for none.
         */
        protected Animation getIdle ()
        {
            return (_idles.length == 0) ? null : _idles[RandomUtil.getWeightedIndex(_idleWeights)];
        }

        /**
         * Returns the movement animation appropriate to the actor's speed and direction, or
         * <code>null</code> for none.
         */
        protected Animation getMovement (Mobile actor)
        {
            // make sure we have movement animations
            int mlen = _movements.length;
            if (mlen == 0) {
                return null;
            }
            Animation[] movement = _movements[0];
            if (mlen > 1) {
                float speed = actor.getSpeed();
                ActorSpriteConfig.MovementSet[] sets =
                    ((ActorSpriteConfig.Moving)_config).movements;
                for (int ii = 1; ii < _movements.length && speed >= sets[ii].speedThreshold;
                        ii++) {
                    movement = _movements[ii];
                }
            }
            float angle = FloatMath.getAngularDifference(
                actor.getDirection(), actor.getRotation()) + FloatMath.PI;
            return movement[Math.round(angle / FloatMath.HALF_PI) % 4];
        }

        /** The resolved idle animations. */
        protected Animation[] _idles;

        /** The weights of the idle animations. */
        protected float[] _idleWeights;

        /** The movement animations. */
        protected Animation[][] _movements;

        /** The speed thresholds of the movement animations. */
        protected float[] _movementSpeeds;

        /** The current idle animation. */
        protected Animation _currentIdle;
    }

    /**
     * Creates a new actor sprite.
     */
    public ActorSprite (TudeyContext ctx, TudeySceneView view, int timestamp, Actor actor)
    {
        super(ctx, view);

        // create the advancer if the actor is client-controlled; otherwise, the history
        _actor = (Actor)actor.clone();
        if ((_advancer = _actor.maybeCreateAdvancer(ctx, view, timestamp)) == null) {
            _history = new ActorHistory(timestamp, actor, view.getBufferDelay() * 2);
        }

        // create the model and the shape
        _model = new Model(ctx);
        _model.setUserObject(this);
        _shape = new ShapeElement(_actor.getOriginal().shape);
        _shape.setUserObject(this);

        // register as tick participant
        _view.addTickParticipant(this);

        // if the actor is created, add it immediately
        updateActor();
        if (isCreated()) {
            _view.getScene().add(_model);
            _view.getActorSpace().add(_shape);
            update();
        }
    }

    /**
     * Returns a reference to the "play head" actor containing interpolated state.
     */
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * Returns a reference to the advancer used to advance the state, if this is actor is
     * controlled by the client.
     */
    public ActorAdvancer getAdvancer ()
    {
        return _advancer;
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
        // update the actor for the current time
        updateActor();

        // handle pre-creation state
        if (_impl == null) {
            if (isCreated()) {
                _view.getScene().add(_model);
                _view.getActorSpace().add(_shape);
                update();
                _impl.wasCreated();
            } else {
                return true; // chill until actually created
            }
        } else {
            update();
        }

        // see if we're destroyed/removed
        if (isDestroyed()) {
            _impl.willBeDestroyed();
            dispose();
            return false;

        } else if (isRemoved()) {
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
        _view.getActorSpace().remove(_shape);
    }

    /**
     * Brings the state of the actor up-to-date with the current time.
     */
    protected void updateActor ()
    {
        if (_advancer == null) {
            _history.get(_view.getDelayedTime(), _actor);
        } else {
            _advancer.advance(_view.getAdvancedTime());
        }
    }

    /**
     * Determines whether the actor has been created.
     */
    protected boolean isCreated ()
    {
        return (_advancer == null) ? _history.isCreated(_view.getDelayedTime()) :
            _view.getAdvancedTime() >= _actor.getCreated();
    }

    /**
     * Determines whether the actor has been destroyed.
     */
    protected boolean isDestroyed ()
    {
        return (_advancer == null) ? _history.isDestroyed(_view.getDelayedTime()) :
            _view.getAdvancedTime() >= _actor.getDestroyed();
    }

    /**
     * Determines whether the actor has been removed.
     */
    protected boolean isRemoved ()
    {
        return (_advancer == null ? _view.getDelayedTime() : _view.getAdvancedTime()) >= _removed;
    }

    /**
     * Updates the configuration and implementation of the sprite.
     */
    protected void update ()
    {
        setConfig(_actor.getConfig());
        _impl.update(_actor);
        updateShape();
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

    /**
     * Updates the shape according to the state of the actor.
     */
    protected void updateShape ()
    {
        _shape.getTransform().set(_actor.getTranslation(), _actor.getRotation(), 1f);
        _shape.setConfig(_actor.getOriginal().shape); // also updates the bounds
    }

    /** The history that we use to find interpolated actor state. */
    protected ActorHistory _history;

    /** The advancer, if this is a controlled actor. */
    protected ActorAdvancer _advancer;

    /** The "play head" actor with interpolated or advanced state. */
    protected Actor _actor;

    /** The actor configuration. */
    protected ActorConfig _config;

    /** The timestamp at which the actor was removed, if any. */
    protected int _removed = Integer.MAX_VALUE;

    /** The actor model. */
    @Scoped
    protected Model _model;

    /** The actor's shape element. */
    protected ShapeElement _shape;

    /** The actor implementation (<code>null</code> until actually created). */
    protected Implementation _impl;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
