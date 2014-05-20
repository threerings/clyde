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

package com.threerings.tudey.client.sprite;

import java.util.List;
import java.util.Map;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.RandomUtil;

import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.Updater;
import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.AnimationConfig;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.scene.Scene.Transient;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.ActorConfig;
import com.threerings.tudey.config.ActorSpriteConfig;
import com.threerings.tudey.data.TudeyOccupantInfo;
import com.threerings.tudey.data.actor.Active;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.EntryState;
import com.threerings.tudey.data.actor.HasActor;
import com.threerings.tudey.data.actor.Mobile;
import com.threerings.tudey.shape.ShapeElement;
import com.threerings.tudey.util.ActorAdvancer;
import com.threerings.tudey.util.ActorHistory;
import com.threerings.tudey.util.TudeyContext;

import static com.threerings.tudey.Log.log;

/**
 * Represents an active element of the scene.
 */
public class ActorSprite extends Sprite
    implements TudeySceneView.TickParticipant, ConfigUpdateListener<ActorConfig>, HasActor
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
         * Returns the sprite's floor flags.
         */
        public int getFloorFlags ()
        {
            return 0x0;
        }

        /**
         * Returns the sprite's floor mask.
         */
        public int getFloorMask ()
        {
            return 0xFF;
        }

        /**
         * Determines whether the implementation is hoverable.
         */
        public boolean isHoverable ()
        {
            return false;
        }

        /**
         * Determines whether the implementation is clickable.
         */
        public boolean isClickable ()
        {
            return false;
        }

        /**
         * Returns the implementation's tooltip text, or <code>null</code> for none.
         */
        public String getTooltipText ()
        {
            return null;
        }

        /**
         * Returns the implementation's tooltip timeout, or -1 to use the default.
         */
        public float getTooltipTimeout ()
        {
            return -1f;
        }

        /**
         * Returns the implementation's tooltip window style.
         */
        public String getTooltipWindowStyle ()
        {
            return "Default/TooltipWindow";
        }

        /**
         * Creates a tooltip component for the implementation (will only be called if
         * {@link #getTooltipText} returns true).
         */
        public Component createTooltipComponent (String tiptext)
        {
            return null;
        }

        /**
         * Dispatches an event on the implementation.
         *
         * @return true if the implementation handled the event, false if it should be handled
         * elsewhere.
         */
        public boolean dispatchEvent (Event event)
        {
            return false;
        }

        /**
         * Returns true if the sprite should update on this tick.
         */
        public boolean shouldUpdate (boolean updated, Actor actor)
        {
            return updated;
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

        /**
         * Notes that the occupant controlling this actor has entered.
         */
        public void occupantEntered (TudeyOccupantInfo info)
        {
            // nothing by default
        }

        /**
         * Notes that the occupant controlling this actor has left.
         */
        public void occupantLeft (TudeyOccupantInfo info)
        {
            // nothing by default
        }

        /**
         * Notes that the occupant controlling this actor has been updated.
         */
        public void occupantUpdated (TudeyOccupantInfo oinfo, TudeyOccupantInfo ninfo)
        {
            // nothing by default
        }

        /**
         * Gives the sprite a chance to display a message sent by the occupant
         * controlling the actor.
         *
         * @return true if the sprite displayed the message.
         */
        public boolean displayMessage (UserMessage msg, boolean alreadyDisplayed)
        {
            return false;
        }

        /**
         * Requests that the sprite clear any chat messages being displayed.
         */
        public void clearMessages ()
        {
            // nothing by default
        }

        /**
         * Returns a scale factor for attached models.
         */
        public float getAttachedScale ()
        {
            return 1f;
        }

        /**
         * If the sprite should operate in static mode.
         */
        public boolean isStatic (Actor actor)
        {
            return actor.getOriginal().isStatic;
        }

        @Override
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
            this(ctx, parentScope);
            setConfig(config);
        }

        /**
         * Get the enclosing ActorSprite.
         */
        public final ActorSprite getActorSprite ()
        {
            return (ActorSprite)getParentScope();
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (ActorSpriteConfig config)
        {
            _config = config;
            _model.setConfig(getModelConfig());
            _model.getLocalTransform().setScale(config.scale);
        }

        @Override
        public int getFloorFlags ()
        {
            return _config.floorFlags;
        }

        @Override
        public int getFloorMask ()
        {
            return _config.floorMask;
        }

        @Override
        public Component createTooltipComponent (String tiptext)
        {
            return Component.createDefaultTooltipComponent(_ctx, tiptext);
        }

        @Override
        public void update (Actor actor)
        {
            // update the model transform
            Vector2f translation = actor.getTranslation();
            Transform3D mtrans = _model.getLocalTransform();
            float oldZ = mtrans.getTranslation().z;
            _view.getFloorTransform(
                translation.x, translation.y, actor.getRotation(), _config.floorMask, mtrans);
            if (_config.smoothZ > 0 && !_initZ) {
                float newZ = mtrans.getTranslation().z;
                newZ = oldZ + Math.signum(newZ - oldZ) * _config.smoothZ;
                mtrans.getTranslation().z = newZ;
            }
            _initZ = true;
            mtrans.promote(Transform3D.UNIFORM);
            _model.updateBounds();
            for (Map.Entry<Model, Boolean> entry : _attachedModels.entrySet()) {
                Model model = entry.getKey();
                if ((model != _model) && entry.getValue()) {
                    updateAttachedTransform(model, mtrans);
                }
            }
        }

        @Override
        public void wasCreated ()
        {
            getActorSprite().spawnTransientModel(_config.creationTransient);
        }

        @Override
        public void willBeDestroyed ()
        {
            getActorSprite().spawnTransientModel(_config.destructionTransient);
        }

        /**
         * Creates a new implementation without configuring it.
         */
        protected Original (TudeyContext ctx, Scope parentScope)
        {
            super(parentScope);
            _ctx = ctx;
        }

        /**
         * Returns the configuration to use for the actor model (gives subclasses a chance to
         * adjust the result).
         */
        protected ConfigReference<ModelConfig> getModelConfig ()
        {
            return _config.model;
        }

        /**
         * Returns true if this sprite is being controlled by the client.
         */
        protected boolean isControlled ()
        {
            return getActorSprite().getAdvancer() != null;
        }

        /** The renderer context. */
        protected TudeyContext _ctx;

        /** The sprite configuration. */
        protected ActorSpriteConfig _config;

        /** If we have initialized our z value. */
        protected boolean _initZ;

        /** The model. */
        @Bound
        protected Model _model;

        /** The owning view. */
        @Bound
        protected TudeySceneView _view;

        /** Other models attached to this sprite. */
        @Bound
        protected Map<Model, Boolean> _attachedModels;
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
            super(ctx, parentScope);
            setConfig(config);
        }

        @Override
        public void setConfig (ActorSpriteConfig config)
        {
            super.setConfig(config);

            ActorSpriteConfig.Moving mconfig = (ActorSpriteConfig.Moving)config;
            _idles = resolve(mconfig.idles);
            _idleWeights = mconfig.getIdleWeights();
            _movements = resolve(mconfig.movements);
            _rotations = resolve(mconfig.rotations);
        }

        @Override
        public void update (Actor actor)
        {
            super.update(actor);

            // update the base animation
            Animation base = getBase((Mobile)actor);
            if (base != null && !base.isPlaying()) {
                base.start();
            }
        }

        @Override
        public float getAttachedScale ()
        {
            return ((ActorSpriteConfig.Moving)_config).attachedScale;
        }

        @Override
        public boolean isStatic (Actor actor)
        {
            return false;
        }

        /**
         * Creates a new implementation without configuring it.
         */
        protected Moving (TudeyContext ctx, Scope parentScope)
        {
            super(ctx, parentScope);
        }

        /**
         * Resolves an array of weighted animations.
         */
        protected Animation[] resolve (ActorSpriteConfig.WeightedAnimation[] weighted)
        {
            Animation[] anims = new Animation[weighted.length];
            for (int ii = 0; ii < anims.length; ii++) {
                anims[ii] = _model.createAnimation(weighted[ii].animation);
            }
            return anims;
        }

        /**
         * Resolves the animations from an array of movement sets.
         */
        protected Animation[][] resolve (ActorSpriteConfig.MovementSet[] sets)
        {
            Animation[][] anims = new Animation[sets.length][];
            for (int ii = 0; ii < anims.length; ii++) {
                anims[ii] = sets[ii].resolve(_model);
            }
            return anims;
        }

        /**
         * Resolves the animations from an array of rotation sets.
         */
        protected Animation[][] resolve (ActorSpriteConfig.RotationSet[] sets)
        {
            Animation[][] anims = new Animation[sets.length][];
            for (int ii = 0; ii < anims.length; ii++) {
                anims[ii] = sets[ii].resolve(_model);
            }
            return anims;
        }

        /**
         * Returns the base animation for the actor.
         */
        protected Animation getBase (Mobile actor)
        {
            if (actor.isSet(Mobile.MOVING)) {
                Animation anim = getMovement(actor);
                if (anim != null) {
                    return anim;
                }
            }
            if (actor.getTurnDirection() != 0) {
                Animation anim = getRotation(actor);
                if (anim != null) {
                    return anim;
                }
            }
            return getIdle();
        }

        /**
         * Returns a reference to the idle animation that the sprite should be playing.
         */
        protected Animation getIdle ()
        {
            return getWeightedAnimation(_idles, _idleWeights);
        }

        /**
         * Returns the movement animation appropriate to the actor's speed and direction, or
         * <code>null</code> for none.
         */
        protected Animation getMovement (Mobile actor)
        {
            ActorSpriteConfig.Moving config = (ActorSpriteConfig.Moving)_config;
            return getMovement(actor, config.scale, config.movements, _movements);
        }

        /**
         * Returns the rotation animation appropriate to the actor's turn rate and direction, or
         * <code>null</code> for none.
         */
        protected Animation getRotation (Mobile actor)
        {
            ActorSpriteConfig.Moving config = (ActorSpriteConfig.Moving)_config;
            return getRotation(actor, config.rotations, _rotations);
        }

        /**
         * Returns a weighted random animation (unless one of the animations is already playing,
         * in which case the method will return that animation).
         */
        protected static Animation getWeightedAnimation (Animation[] anims, float[] weights)
        {
            if (anims.length == 0) {
                return null;
            }
            for (Animation anim : anims) {
                if (anim.isPlaying()) {
                    return anim;
                }
            }
            return anims[RandomUtil.getWeightedIndex(weights)];
        }

        /**
         * Configures and returns the appropriate movement animation for the actor.
         *
         * @param scale the actor scale.
         * @param sets the movement set configs.
         * @param movements the resolved movement animations.
         */
        protected static Animation getMovement (
            Mobile actor, float scale, ActorSpriteConfig.MovementSet[] sets,
            Animation[][] movements)
        {
            // make sure we have movement animations
            int mlen = movements.length;
            if (mlen == 0) {
                return null;
            }
            float sspeed = actor.getSpeed() / scale;
            int idx = 0;
            if (mlen > 1) {
                float cdiff = Math.abs(sspeed - sets[0].speed);
                for (int ii = 1; ii < sets.length; ii++) {
                    float diff = Math.abs(sspeed - sets[ii].speed);
                    if (diff < cdiff) {
                        cdiff = diff;
                        idx = ii;
                    }
                }
            }
            // include a bias towards the lateral directions so that we're sure to use them
            // when travelling diagonally
            float angle = (FloatMath.getAngularDifference(
                actor.getDirection(), actor.getRotation())/FloatMath.HALF_PI + 2.5f) % 4f;
            Animation movement;
            if (angle <= 0.01f || angle >= 2.99f) {
                movement = movements[idx][3];
            } else if (angle < 0.99f) {
                movement = movements[idx][0];
            } else if (angle <= 2.01f) {
                movement = movements[idx][1];
            } else {
                movement = movements[idx][2];
            }
            movement.setSpeed(sspeed / sets[idx].speed);
            return movement;
        }

        /**
         * Configures and returns the appropriate rotation animation for the actor.
         *
         * @param sets the rotation set configs.
         * @param rotations the resolved rotation animations.
         */
        protected static Animation getRotation (
            Mobile actor, ActorSpriteConfig.RotationSet[] sets, Animation[][] rotations)
        {
            // make sure we have rotation animations
            int rlen = rotations.length;
            if (rlen == 0) {
                return null;
            }
            float rate = actor.getTurnRate();
            int idx = 0;
            if (rlen > 1) {
                float cdiff = Math.abs(rate - sets[0].rate);
                for (int ii = 1; ii < sets.length; ii++) {
                    float diff = Math.abs(rate - sets[ii].rate);
                    if (diff < cdiff) {
                        cdiff = diff;
                        idx = ii;
                    }
                }
            }
            Animation rotation = rotations[idx][actor.getTurnDirection() > 0 ? 0 : 1];
            rotation.setSpeed(rate / sets[idx].rate);
            return rotation;
        }

        /** The resolved idle animations. */
        protected Animation[] _idles;

        /** The weights of the idle animations. */
        protected float[] _idleWeights;

        /** The movement animations. */
        protected Animation[][] _movements;

        /** The rotation animations. */
        protected Animation[][] _rotations;

        /** The current idle animation. */
        protected Animation _currentIdle;
    }

    /**
     * Depicts an active actor with activity animations.
     */
    public static class Acting extends Moving
    {
        /**
         * Creates a new implementation.
         */
        public Acting (TudeyContext ctx, Scope parentScope, ActorSpriteConfig.Moving config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        @Override
        public void update (Actor actor)
        {
            // update the activity
            Active active = (Active)actor;
            int activity = active.getActivity();
            int started = active.getActivityStarted();
            if (activity != _lastActivity || started > _lastActivityStarted) {
                activityChanged();
                Activity previous = _activity;
                Activity next = _activities.get(_lastActivity = activity);
                _lastActivityStarted = started;
                if (_activity != null) {
                    _activity.stop(next);
                }
                if ((_activity = next) != null) {
                    _activity.start(previous);
                }
            }
            if (_activity != null) {
                _activity.update();
            }

            super.update(actor);
        }

        @Override
        protected Animation getBase (Mobile actor)
        {
            // activities at priority zero override default base animation
            return (_activity != null && _activity.getPriority() == 0) ?
                null : super.getBase(actor);
        }

        /**
         * Creates a new implementation without configuring it.
         */
        protected Acting (TudeyContext ctx, Scope parentScope)
        {
            super(ctx, parentScope);
        }

        /**
         * Called when the activity has changed.
         */
        protected void activityChanged ()
        {
        }

        /**
         * Handles an activity.
         */
        protected class Activity
        {
            /**
             * Creates a new activity.
             */
            public Activity (String... anims)
            {
                List<Animation> list = Lists.newArrayListWithCapacity(anims.length);
                for (String anim : anims) {
                    Animation animation = _model.getAnimation(anim);
                    if (animation != null) {
                        list.add(animation);
                    }
                }
                _anims = list.toArray(new Animation[list.size()]);
            }

            /**
             * Creates a new activity.
             */
            public Activity (ConfigReference<AnimationConfig> anim)
            {
                setAnimation(anim);
            }

            /**
             * Creates a new activity.
             */
            public Activity (ConfigReference<AnimationConfig>... anims)
            {
                List<Animation> list = Lists.newArrayListWithCapacity(anims.length);
                for (ConfigReference<AnimationConfig> anim : anims) {
                    Animation animation = (anim == null) ? null : _model.createAnimation(anim);
                    if (anim != null) {
                        list.add(animation);
                    }
                }
                _anims = list.toArray(new Animation[list.size()]);
            }

            /**
             * Creates a new activity.
             */
            public Activity (Animation... anims)
            {
                _anims = anims;
            }

            /**
             * Returns a reference to the array of animations.
             */
            public Animation[] getAnimations ()
            {
                return _anims;
            }

            /**
             * Returns the priority of the activity.
             */
            public int getPriority ()
            {
                return (_anims.length == 0) ? 0 : _anims[0].getPriority();
            }

            /**
             * Starts the activity.
             */
            public void start (Activity previous)
            {
                if (_anims.length > 0) {
                    _anims[_idx = 0].start();
                }
            }

            /**
             * Stops the activity.
             */
            public void stop (Activity next)
            {
                if (_idx < _anims.length) {
                    _anims[_idx].stop();
                }
            }

            /**
             * Updates the activity.
             */
            public void update ()
            {
                if (_idx < _anims.length - 1 && !_anims[_idx].isPlaying()) {
                    _anims[++_idx].start();
                }
            }

            /**
             * Sets the activity animation.
             */
            protected void setAnimation (ConfigReference<AnimationConfig> anim)
            {
                Animation mation = (anim == null) ? null : _model.createAnimation(anim);
                _anims = (mation == null)
                    ? new Animation[0]
                    : new Animation[] { mation };
            }

            /** The activity's component animations. */
            protected Animation[] _anims;

            /** The index of the current animation. */
            protected int _idx;
        }

        /** The last activity applied. */
        protected int _lastActivity;

        /** The time at which the last activity was started. */
        protected int _lastActivityStarted;

        /** The activity handlers. */
        protected IntMap<Activity> _activities = IntMaps.newHashIntMap();

        /** The current activity. */
        protected Activity _activity;
    }

    /**
     * Executes animations on the corresponding entry sprite.
     */
    public static class StatefulEntry extends Original
    {
        /**
         * Creates a new implementation.
         */
        public StatefulEntry (
            TudeyContext ctx, Scope parentScope, ActorSpriteConfig.StatefulEntry config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        @Override
        public void setConfig (ActorSpriteConfig config)
        {
            super.setConfig(config);

            // find the corresponding entry sprite and its model
            EntryState estate = (EntryState)((ActorSprite)_parentScope).getActor();
            EntrySprite esprite = _view.getEntrySprite(estate.getKey());
            _entryModel = (esprite == null) ? null : esprite.getModel();
            if (_entryModel == null) {
                return;
            }

            // resolve the state animations
            ActorSpriteConfig.StatefulEntry sconfig = (ActorSpriteConfig.StatefulEntry)config;
            _states = new Animation[sconfig.states.length];
            for (int ii = 0; ii < _states.length; ii++) {
                _states[ii] = _entryModel.createAnimation(sconfig.states[ii].animation);
            }
        }

        @Override
        public void update (Actor actor)
        {
            super.update(actor);

            // update the state animation
            EntryState estate = (EntryState)actor;
            int entered = estate.getStateEntered();
            if (entered > _lastStateEntered) {
                int state = estate.getState();
                Animation anim =
                    (_states != null && state < _states.length) ? _states[state] : null;
                if (anim != null) {
                    anim.start();
                    anim.tick((_view.getDelayedTime() - entered) / 1000f);
                }
                _lastStateEntered = entered;
            }
        }

        /** The model corresponding to the entry sprite. */
        protected Model _entryModel;

        /** Animations for the various states. */
        protected Animation[] _states;

        /** The time at which we entered the last state. */
        protected int _lastStateEntered;
    }

    /**
     * Changes model configurations on the corresponding entry sprite.
     */
    public static class StatefulModelEntry extends Original
    {
        /**
         * Creates a new implementation.
         */
        public StatefulModelEntry (
            TudeyContext ctx, Scope parentScope, ActorSpriteConfig.StatefulModelEntry config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        @Override
        public void setConfig (ActorSpriteConfig config)
        {
            super.setConfig(config);

            // find the corresponding entry sprite and its model
            EntryState estate = (EntryState)((ActorSprite)_parentScope).getActor();
            EntrySprite esprite = _view.getEntrySprite(estate.getKey());
            _entryModel = (esprite == null) ? null : esprite.getModel();
        }

        @Override
        public void update (Actor actor)
        {
            super.update(actor);

            ActorSpriteConfig.StatefulModelEntry config =
                (ActorSpriteConfig.StatefulModelEntry)_config;

            // update the state animation
            EntryState estate = (EntryState)actor;
            int entered = estate.getStateEntered();
            if (entered > _lastStateEntered) {
                int state = estate.getState();
                ConfigReference<ModelConfig> model = (state < config.states.length) ?
                    config.states[state].model : null;
                if (model != null && _entryModel != null) {
                    _entryModel.setConfig(model);
                }
                _lastStateEntered = entered;
            }
        }

        /** The model corresponding to the entry sprite. */
        protected Model _entryModel;

        /** The time at which we entered the last state. */
        protected int _lastStateEntered;
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
            _history = new ActorHistory(timestamp, actor, view.getBufferDelay() * 4);
        }

        // create the model and the shape
        _model = new Model(ctx);
        _model.setUserObject(this);
        _attachedModels = Maps.newHashMap();
        _attachedModels.put(_model, true);
        _shape = new ShapeElement(_actor.getOriginal().getShape(ctx.getConfigManager()));
        _shape.setUserObject(this);

        // register as tick participant
        _view.addTickParticipant(this);

        // if the actor is created, add it immediately
        updateActor();
        if (isCreated()) {
            _view.getScene().add(_model);
            _view.getActorSpace().add(_shape);
            _model.tick(0f);
            update();
            if (timestamp == _actor.getCreated()) {
                _impl.wasCreated();
            }
        } else {
            _impl = null; // signifies that the actor has not yet been created
        }
    }

    /**
     * Reinitializes the sprite.
     */
    public void reinit (int timestamp, Actor actor)
    {
        actor.copy(_actor);
        if (_actor.isClientControlled(_ctx, _view)) {
            if (_advancer == null) {
                _advancer = _actor.createAdvancer(_view, timestamp);
                _history = null;
            } else {
                _advancer.init(_actor, timestamp);
            }
        } else {
            if (_history == null) {
                _history = new ActorHistory(timestamp, actor, _view.getBufferDelay() * 4);
                _advancer = null;
            } else {
                _history.init(timestamp, actor);
            }
        }
        updateActor();
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
     * Returns a reference to all the sprites models.
     */
    public Map<Model, Boolean> getModels ()
    {
        return _attachedModels;
    }

    /**
     * Returns a reference to the sprite implementation.
     */
    public Implementation getImplementation ()
    {
        return _impl;
    }

    /**
     * Attaches a model to this sprite, setting its scale to the product of its current scale and
     * an attachment scale specified by the implementation.
     */
    public void attachScaledModel (Model model)
    {
        attachScaledModel(model, model.getLocalTransform().approximateUniformScale());
    }

    /**
     * Attaches a model to this sprite, setting its scale to the product of the provided base scale
     * and an attachment scale specified by the implementation.
     */
    public void attachScaledModel (Model model, float baseScale)
    {
        Transform3D transform = model.getLocalTransform();
        transform.setScale(baseScale * getAttachedScale());
        transform.promote(Transform3D.UNIFORM);
        attachModel(model, true);
    }

    /**
     * Attaches a model to this sprite.
     */
    public void attachModel (Model model, boolean transform)
    {
        if (!(_attachedModels.containsKey(model) || _disposed)) {
            _attachedModels.put(model, transform);
            if (_impl != null && _attachedVisible) {
                if (transform) {
                    updateAttachedTransform(model, _model.getLocalTransform());
                }
                _view.getScene().add(model);
            }
        }
    }

    /**
     * Detaches a model from this sprite.
     */
    public void detachModel (Model model)
    {
        if (model == _model || model == null) {
            return;
        }
        if (_attachedModels.remove(model) != null && _impl != null && _attachedVisible) {
            _view.getScene().remove(model);
        }
    }

    /**
     * Updates the visibility of attached models.
     */
    public void setAttachedVisibility (boolean visible)
    {
        if (_attachedVisible != visible) {
            _attachedVisible = visible;
            for (Map.Entry<Model, Boolean> entry : _attachedModels.entrySet()) {
                Model model = entry.getKey();
                if (model == _model) {
                    continue;
                }
                if (_attachedVisible) {
                    if (entry.getValue()) {
                        updateAttachedTransform(model, _model.getLocalTransform());
                    }
                    _view.getScene().add(model);
                } else {
                    _view.getScene().remove(model);
                }
            }
        }
    }

    /**
     * Gets and attaches a transient model to this sprite.
     */
    public void spawnAttachedTransientModel (ConfigReference<ModelConfig> ref)
    {
        spawnAttachedTransientModel(ref, true);
    }

    /**
     * Gets and attaches a transient model to this sprite.
     *
     * @param rotate if true, update the transient's rotation as well as its translation.
     */
    public void spawnAttachedTransientModel (
        ConfigReference<ModelConfig> ref, final boolean rotate)
    {
        if (_impl == null) {
            return;
        }
        final Transient trans = _view.getScene().getFromTransientPool(ref);
        trans.setUpdater(new Updater() {
            public void update () {
                updateAttachedTransform(trans, _model.getLocalTransform(), rotate);
            }
        });
        updateAttachedTransform(trans, _model.getLocalTransform(), rotate);
        _view.getScene().add(trans);
    }

    /**
     * Spawns a transient model at the location (translation and rotation) of this sprite.
     */
    public Model spawnTransientModel (ConfigReference<ModelConfig> ref)
    {
        return spawnTransientModel(ref, true);
    }

    /**
     * Spawns a transient model at the location of this sprite.
     *
     * @param rotate if true, match the rotation as well as the translation of the sprite.
     */
    public Model spawnTransientModel (ConfigReference<ModelConfig> ref, boolean rotate)
    {
        return spawnOffsetTransientModel(ref, rotate, 0f);
    }

    /**
     * Spawns a transient model at a z-offset of the location of this sprite.
     *
     * @param rotate if true, match the rotation as well as the translation of the sprite.
     */
    public Model spawnOffsetTransientModel (ConfigReference<ModelConfig> ref, boolean rotate)
    {
        return spawnOffsetTransientModel(ref, rotate, getAttachedScale() - 1f);
    }

    /**
     * Updates this sprite with new state.
     */
    public void update (int timestamp, Actor actor, boolean updated)
    {
        if (_advancer == null) {
            _history.record(timestamp, actor, updated || !isStatic(actor));
        } else {
            _advancer.init((Actor)actor.copy(_advancer.getActor()), timestamp);
        }
    }

    /**
     * Notes that the actor has been removed.
     */
    public void remove (int timestamp)
    {
        _removed = timestamp;
    }

    /**
     * Notes that the occupant controlling this actor has entered.
     */
    public void occupantEntered (TudeyOccupantInfo info)
    {
        if (_impl != null) {
            _impl.occupantEntered(info);
        }
    }

    /**
     * Notes that the occupant controlling this actor has left.
     */
    public void occupantLeft (TudeyOccupantInfo info)
    {
        if (_impl != null) {
            _impl.occupantLeft(info);
        }
    }

    /**
     * Notes that the occupant controlling this actor has been updated.
     */
    public void occupantUpdated (TudeyOccupantInfo oinfo, TudeyOccupantInfo ninfo)
    {
        if (_impl != null) {
            _impl.occupantUpdated(oinfo, ninfo);
        }
    }

    /**
     * Gives the sprite a chance to display a message sent by the occupant controlling the actor.
     *
     * @return true if the sprite displayed the message.
     */
    public boolean displayMessage (UserMessage msg, boolean alreadyDisplayed)
    {
        return _impl != null && _impl.displayMessage(msg, alreadyDisplayed);
    }

    /**
     * Requests that the sprite clear any chat messages being displayed.
     */
    public void clearMessages ()
    {
        if (_impl != null) {
            _impl.clearMessages();
        }
    }

    // documentation inherited from interface TudeySceneView.TickParticipant
    public boolean tick (int delayedTime)
    {
        // update the actor for the current time
        boolean updated = updateActor();

        // handle pre-creation state
        if (_impl == null) {
            if (isCreated()) {
                for (Model attached : _attachedModels.keySet()) {
                    if (_attachedVisible || attached == _model) {
                        _view.getScene().add(attached);
                    }
                }
                _view.getActorSpace().add(_shape);

                // start off with a null implementation; that way nothing will break if updating
                // tries to access the objects we just added to the scene/actor space
                _impl = NULL_IMPLEMENTATION;
                update();
                _impl.wasCreated();
            } else {
                return true; // chill until actually created
            }
        } else if (_impl.shouldUpdate(updated, _actor)) {
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

    // documentation inherited
    public Actor getActor ()
    {
        return _actor;
    }

    /**
     * If we have an advancer, return the current actor, otherwise return the actor in the final
     * entry from the actor history.
     */
    public Actor getLastKnownActor ()
    {
        return _advancer == null ? _history.getLastKnownActor() : _actor;
    }

    @Override
    public Model getModel ()
    {
        return _model;
    }

    @Override
    public int getFloorFlags ()
    {
        return _impl.getFloorFlags();
    }

    @Override
    public int getFloorMask ()
    {
        return _impl.getFloorMask();
    }

    @Override
    public boolean isHoverable ()
    {
        return _impl.isHoverable();
    }

    @Override
    public boolean isClickable ()
    {
        return _impl.isClickable();
    }

    @Override
    public String getTooltipText ()
    {
        return _impl.getTooltipText();
    }

    @Override
    public float getTooltipTimeout ()
    {
        return _impl.getTooltipTimeout();
    }

    @Override
    public String getTooltipWindowStyle ()
    {
        return _impl.getTooltipWindowStyle();
    }

    @Override
    public Component createTooltipComponent (String tiptext)
    {
        return _impl.createTooltipComponent(tiptext);
    }

    @Override
    public boolean dispatchEvent (Event event)
    {
        return _impl.dispatchEvent(event);
    }

    @Override
    public void dispose ()
    {
        super.dispose();
        if (_impl != null) {
            _impl.dispose();
        }
        if (_config != null) {
            _config.removeListener(this);
        }
        for (Model attached : _attachedModels.keySet()) {
            if (_attachedVisible || _model == attached) {
                _view.getScene().remove(attached);
            }
        }
        _attachedModels.clear();
        _view.getActorSpace().remove(_shape);
        _disposed = true;
    }

    /**
     * Brings the state of the actor up-to-date with the current time.
     */
    protected boolean updateActor ()
    {
        if (_advancer == null) {
            return _history.get(_view.getDelayedTime(), _actor, isStatic(_actor));
        } else {
            _advancer.advance(_view.getAdvancedTime());
        }
        return true;
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
        return getActorTime() >= _removed;
    }

    /**
     * Returns the time value for the actor.
     */
    protected int getActorTime ()
    {
        return (_advancer == null) ? _view.getDelayedTime() : _view.getAdvancedTime();
    }

    /**
     * Returns true if this sprite should operate in static mode.
     */
    protected boolean isStatic (Actor actor)
    {
        return _impl != null && _impl.isStatic(actor);
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
        // resetting the config also updates the bounds
        _shape.setConfig(_actor.getOriginal().getShape(_ctx.getConfigManager()));
    }

    /**
     * Returns the scale to use for attachments.
     */
    protected float getAttachedScale ()
    {
        return (_impl == null) ? 1f : _impl.getAttachedScale();
    }

    /**
     * Spawns a transient model at a z-offset of the location of this sprite.
     *
     * @param rotate if true, match the rotation as well as the translation of the sprite.
     */
    public Model spawnOffsetTransientModel (
            ConfigReference<ModelConfig> ref, boolean rotate, float zoffset)
    {
        return spawnOffsetTransientModel(
                ref, rotate, zoffset != 0f ? new Vector3f(0f, 0f, zoffset) : Vector3f.ZERO);
    }

    /**
     * Spawns a transient model at an offset of the location of this sprite.
     *
     * @param rotate if true, match the rotation as well as the translation of the sprite.
     */
    public Model spawnOffsetTransientModel (
            ConfigReference<ModelConfig> ref, boolean rotate, Vector3f offset)
    {
        if (ref != null) {
            Transform3D mxform = _model.getLocalTransform();
            Transform3D txform = new Transform3D(Transform3D.UNIFORM);
            mxform.extractTranslation(txform.getTranslation());
            txform.getTranslation().addLocal(offset);
            if (rotate) {
                mxform.extractRotation(txform.getRotation());
            }
            return _view.getScene().spawnTransient(ref, txform);
        }
        return null;
    }

    /**
     * Updates the transform of an attached model based on that of the primary.
     */
    protected static void updateAttachedTransform (Model attached, Transform3D mtrans)
    {
        updateAttachedTransform(attached, mtrans, true);
    }

    /**
     * Updates the transform of an attached model based on that of the primary.
     */
    protected static void updateAttachedTransform (
        Model attached, Transform3D mtrans, boolean rotate)
    {
        Transform3D atrans = attached.getLocalTransform();
        atrans.set(mtrans.getTranslation(), rotate ? mtrans.getRotation() : Quaternion.IDENTITY,
            atrans.approximateUniformScale());
        attached.updateBounds();
    }

    /** The history that we use to find interpolated actor state. */
    protected ActorHistory _history;

    /** The advancer, if this is a controlled actor. */
    protected ActorAdvancer _advancer;

    /** The "play head" actor with interpolated or advanced state. */
    @Scoped
    protected Actor _actor;

    /** The actor configuration. */
    protected ActorConfig _config;

    /** The timestamp at which the actor was removed, if any. */
    protected int _removed = Integer.MAX_VALUE;

    /** The actor model. */
    @Scoped
    protected Model _model;

    /** Models attached to this sprite (including the primary model), and if their transforms
     * are automatically updated. */
    @Scoped
    protected Map<Model, Boolean> _attachedModels;

    /** If attached models are visible. */
    protected boolean _attachedVisible = true;

    /** The actor's shape element. */
    protected ShapeElement _shape;

    /** The actor implementation (<code>null</code> until actually created). */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** Set when the sprite has been disposed. */
    protected boolean _disposed;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
