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

package com.threerings.tudey.client;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.samskivert.util.HashIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.ObserverList;
import com.samskivert.util.RunAnywhere;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;

import com.threerings.math.FloatMath;
import com.threerings.math.Plane;
import com.threerings.math.Ray3D;
import com.threerings.math.Vector2f;
import com.threerings.math.Vector3f;

import com.threerings.opengl.camera.MouseOrbiter;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.Root;
import com.threerings.opengl.gui.Window;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.event.MouseListener;
import com.threerings.opengl.gui.event.MouseMotionListener;
import com.threerings.opengl.gui.event.MouseWheelListener;
import com.threerings.opengl.gui.util.PseudoKeys;
import com.threerings.opengl.scene.SceneElement;
import com.threerings.opengl.util.Tickable;

import com.threerings.tudey.client.sprite.ActorSprite;
import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.Sprite;
import com.threerings.tudey.config.ClientActionConfig;
import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.data.TudeyBodyObject;
import com.threerings.tudey.data.TudeyOccupantInfo;
import com.threerings.tudey.data.TudeySceneConfig;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Pawn;
import com.threerings.tudey.dobj.SceneDeltaEvent;
import com.threerings.tudey.dobj.SceneDeltaListener;
import com.threerings.tudey.util.PawnAdvancer;
import com.threerings.tudey.util.TudeyContext;

import static com.threerings.tudey.Log.log;

/**
 * The basic Tudey scene controller class.
 */
public class TudeySceneController extends SceneController
    implements SceneDeltaListener, MessageListener, PseudoKeys.Observer,
        MouseListener, MouseMotionListener, MouseWheelListener,  Tickable
{
    /**
     * Returns the interval at which we transmit our input frames.
     */
    public int getTransmitInterval ()
    {
        return ((TudeySceneConfig)_config).getTransmitInterval();
    }

    /**
     * Returns the id of the actor that the camera should track.
     */
    public int getTargetId ()
    {
        return _targetId;
    }

    /**
     * Returns the id of the actor that is being controlled.
     */
    public int getControlledId ()
    {
        return _controlledId;
    }

    /**
     * Checks whether the specified actor id is that of the controlled target.
     */
    public boolean isControlledId (int actorId)
    {
        return actorId == _controlledId;
    }

    /**
     * Returns a reference to the hover sprite, if any.
     */
    public Sprite getHoverSprite ()
    {
        return _hsprite;
    }

    /**
     * Called by the view when we first add our controlled actor.
     */
    public void controlledActorAdded (int timestamp, Actor actor)
    {
        _advancer = (PawnAdvancer)actor.maybeCreateAdvancer(_tctx, _tsview, timestamp);
        _advancer.advance(_tsview.getAdvancedTime());
    }

    /**
     * Called by the view when we receive an update for our controlled actor.
     */
    public void controlledActorUpdated (int timestamp, Actor actor)
    {
        // remove outdated states
        while (!_states.isEmpty() && timestamp >= _states.get(0).getFrame().getTimestamp()) {
            _states.remove(0);
        }

        // clone the actor and set it up in the advancer
        Actor oactor = _advancer.getActor();
        Actor nactor = (Actor)actor.clone();
        _advancer.init(nactor, timestamp);

        // verify the remaining states
        int advancedTime = _tsview.getAdvancedTime();
        for (int ii = 0, nn = _states.size(); ii < nn; ii++) {
            PawnState state = _states.get(ii);
            Pawn spawn = state.getPawn();
            _advancer.advance(state.getFrame());
            if (spawn.equals(nactor)) {
                _advancer.init(oactor, advancedTime);
                return; // cut out early; they'll all be the same from here on
            }
            nactor.copy(spawn);
        }

        // make sure we have the most recent frame, advance to current time
        if (_lastFrame != null) {
            _advancer.advance(_lastFrame);
        }
        _advancer.advance(advancedTime);

        // copy everything *except* the translation (which will be smoothed) to the sprite
        Actor sactor = _tsview.getControlledSprite().getActor();
        _translation.set(sactor.getTranslation());
        nactor.copy(sactor);
        if (!sactor.isSet(Actor.WARP)) {
            sactor.getTranslation().set(_translation);
        }
    }

    /**
     * Called by the view when we remove our controlled sprite.
     */
    public void controlledSpriteRemoved (long timestamp)
    {
        _advancer = null;
    }

    /**
     * Submits a named request to the server.
     */
    public void submitRequest (Sprite source, String name)
    {
        if (source instanceof ActorSprite) {
            _tsobj.tudeySceneService.submitActorRequest(
                ((ActorSprite)source).getActor().getId(), name);
        } else if (source instanceof EntrySprite) {
            _tsobj.tudeySceneService.submitEntryRequest(
                ((EntrySprite)source).getEntry().getKey(), name);
        } else {
            log.warning("Tried to submit request from unknown sprite type.",
                "source", source, "name", name);
        }
    }

    // documentation inherited from interface SceneDeltaListener
    public void sceneDeltaReceived (SceneDeltaEvent event)
    {
        // make sure it refers to this scene
        if (event.getSceneOid() != _tsobj.getOid()) {
            log.info("Received delta event for wrong scene.", "event", event);
            return;
        }
        // make sure it's not a out of order or a repeat
        int timestamp = event.getTimestamp();
        if (timestamp <= _lastDelta) {
            return;
        }

        // prune all acknowledged input frames
        int acknowledge = event.getAcknowledge();
        while (!_input.isEmpty() && _input.get(0).getTimestamp() <= acknowledge) {
            _input.remove(0);
        }

        // pass it on to the view for visualization
        if (_tsview.processSceneDelta(event)) {
            _lastDelta = timestamp;
        }
    }

    // documentation inherited from interface MessageListener
    public void messageReceived (MessageEvent event)
    {
        if (event.getName().equals(TudeyBodyObject.FORCE_CLIENT_ACTION)) {
            Object[] args = event.getArgs();
            ((ClientActionConfig)args[0]).execute(
                _tctx, _tsview, _tsview.getSprite((EntityKey)args[1]));
        }
    }

    // documentation inherited from interface PseudoKeys.Observer
    public void keyPressed (final long when, final int key, final float amount)
    {
        if (!mouseCameraEnabled()) {
            ObserverList<PseudoKeys.Observer> list = _keyObservers.get(key);
            if (list != null) {
                list.apply(new ObserverList.ObserverOp<PseudoKeys.Observer>() {
                    public boolean apply (PseudoKeys.Observer observer) {
                        observer.keyPressed(when, key, amount);
                        return true;
                    }
                });
            }
        }
    }

    // documentation inherited from interface PseudoKeys.Observer
    public void keyReleased (final long when, final int key)
    {
        if (!mouseCameraEnabled()) {
            ObserverList<PseudoKeys.Observer> list = _keyObservers.get(key);
            if (list != null) {
                list.apply(new ObserverList.ObserverOp<PseudoKeys.Observer>() {
                    public boolean apply (PseudoKeys.Observer observer) {
                        observer.keyReleased(when, key);
                        return true;
                    }
                });
            }
        }
    }

    // documentation inherited from interface MouseListener
    public void mousePressed (MouseEvent event)
    {
        if (!inputWindowHovered()) {
            return;
        }
        if (_hsprite != null && _hsprite.dispatchEvent(event)) {
            event.consume();
        }
        if (event.getButton() == MouseEvent.BUTTON1) {
            _holdHover = true;
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseReleased (MouseEvent event)
    {
        if (!inputWindowHovered()) {
            return;
        }
        if (_hsprite != null && _hsprite.dispatchEvent(event)) {
            event.consume();
        }
        if (event.getButton() == MouseEvent.BUTTON1) {
            _holdHover = false;
        }
    }

    // documentation inherited from interface MouseListener
    public void mouseClicked (MouseEvent event)
    {
        maybeDispatchToHoverSprite(event);
    }

    // documentation inherited from interface MouseListener
    public void mouseEntered (MouseEvent event)
    {
        // no-op
    }

    // documentation inherited from interface MouseListener
    public void mouseExited (MouseEvent event)
    {
        // no-op
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseMoved (MouseEvent event)
    {
        maybeDispatchToHoverSprite(event);
    }

    // documentation inherited from interface MouseMotionListener
    public void mouseDragged (MouseEvent event)
    {
        maybeDispatchToHoverSprite(event);
    }

    // documentation inherited from interface MouseWheelListener
    public void mouseWheeled (MouseEvent event)
    {
        maybeDispatchToHoverSprite(event);
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // increment the tick count
        _tickCount++;

        // update the hover sprite/input
        updateInput(elapsed);

        // perhaps transmit our acknowledgement and input frames
        long now = RunAnywhere.currentTimeMillis();
        if (now - _lastTransmit >= getTransmitInterval() && _lastDelta > 0) {
            transmitInput();
            _lastTransmit = now;
        }
    }

    @Override
    public void wasAdded ()
    {
        super.wasAdded();

        // perhaps create the mouse orbiter
        if (getMouseCameraModifiers() != 0) {
            OrbitCameraHandler camhand = _tsview.getCameraHandler();
            _tsview.getInputWindow().addListener(_orbiter = new MouseOrbiter(camhand, true) {
                public void mouseDragged (MouseEvent event) {
                    if (mouseCameraEnabled()) {
                        super.mouseDragged(event);
                    } else {
                        super.mouseMoved(event);
                    }
                }
                public void mouseWheeled (MouseEvent event) {
                    if (mouseCameraEnabled()) {
                        super.mouseWheeled(event);
                    }
                }
            });
        }
    }

    @Override
    public void wasRemoved ()
    {
        super.wasRemoved();

        // remove the mouse orbiter
        if (_orbiter != null) {
            _tsview.getInputWindow().removeListener(_orbiter);
            _orbiter = null;
        }
    }

    @Override
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
        _tsobj = (TudeySceneObject)plobj;

        // listen to the client object for delta events
        ClientObject clobj = _ctx.getClient().getClientObject();
        clobj.addListener(this);

        // listen for input events
        _tsview.getInputWindow().addListener(this);
        _tsview.getInputWindow().addListener(_unifier);

        // if the player controls a pawn, then the target by default.
        // otherwise, the target starts out being the first pawn in the occupant list
        _controlledId = _tsobj.getPawnId(clobj.getOid());
        if (_controlledId > 0) {
            _targetId = _controlledId;
        } else {
            _targetId = _tsobj.getFirstPawnId();
        }

        // bind keys to actions
        bindKeys();

        if (_tsobj.tudeySceneService != null) { // will be null when testing
            // notify the server that we're in (it will start sending updates)
            _tsobj.tudeySceneService.enteredPlace();
        }
    }

    @Override
    public void didLeavePlace (PlaceObject plobj)
    {
        super.didLeavePlace(plobj);

        // stop listening to the client object and input events
        _ctx.getClient().getClientObject().removeListener(this);
        _tsview.getInputWindow().removeListener(this);
        _tsview.getInputWindow().removeListener(_unifier);
    }

    @Override
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_tsview = new TudeySceneView((TudeyContext)ctx, this));
    }

    @Override
    protected void didInit ()
    {
        super.didInit();
        _tctx = (TudeyContext)_ctx;
    }

    /**
     * Binds pseudo-keys to observers that act on key press and/or release events.
     */
    protected void bindKeys ()
    {
        _unifier.clearModifierKeys();
        _unifier.clearModifierUsers();
        if (_controlledId > 0) {
            bindKeyMovement(PseudoKeys.KEY_BUTTON1, _relativeMoveAmounts, _relativeMovePresses, 0);
            bindKeyMovement(Keyboard.KEY_W, _absoluteMoveAmounts, _absoluteMovePresses, 0);
            bindKeyMovement(Keyboard.KEY_S, _absoluteMoveAmounts, _absoluteMovePresses, 1);
            bindKeyMovement(Keyboard.KEY_A, _absoluteMoveAmounts, _absoluteMovePresses, 2);
            bindKeyMovement(Keyboard.KEY_D, _absoluteMoveAmounts, _absoluteMovePresses, 3);
            bindKeyStrafe(Keyboard.KEY_C);
        } else {
            bindKeyCycle(Keyboard.KEY_LEFT, false);
            bindKeyCycle(Keyboard.KEY_RIGHT, true);
        }
    }

    /**
     * Binds a key to an input flag.
     */
    protected void bindKeyFlag (int key, final int flag)
    {
        addKeyObserver(key, new PseudoKeys.Observer() {
            public void keyPressed (long when, int key, float amount) {
                _flagPresses.put(flag, key);
                updateFlag(flag);
            }
            public void keyReleased (long when, int key) {
                _flagPresses.remove(flag, key);
                updateFlag(flag);
            }
        });
    }

    /**
     * Updates the state of the specified flag based on its presses.
     */
    protected void updateFlag (int flag)
    {
        if (flag == InputFrame.MOVE) {
            updateMoveFlag();
            return;
        }
        if (_flagPresses.containsKey(flag)) {
            _flags |= flag;
        } else {
            _flags &= ~flag;
        }
    }

    /**
     * Updates the move flag.
     */
    protected void updateMoveFlag ()
    {
        if (isPressed(_absoluteMoveAmounts) || isPressed(_relativeMoveAmounts) ||
                _flagPresses.containsKey(InputFrame.MOVE)) {
            _flags |= InputFrame.MOVE;
        } else {
            _flags &= ~InputFrame.MOVE;
        }
    }

    /**
     * Checks whether the supplied amounts count as a "press" for movement purposes.
     */
    protected boolean isPressed (float[] amounts)
    {
        float fx = amounts[3] - amounts[2];
        float fy = amounts[0] - amounts[1];
        return FloatMath.hypot(fx, fy) > 0.5f;
    }

    /**
     * Binds a key to cycle between targets.
     */
    protected void bindKeyCycle (int key, final boolean forward)
    {
        addKeyObserver(key, new PseudoKeys.Adapter() {
            public void keyPressed (long when, int key, float amount) {
                cycleTarget(forward);
            }
        });
    }

    /**
     * Binds a key to a movement direction.
     */
    protected void bindKeyMovement (
        int key, final float[] amounts, IntMap<Float>[] presses, final int idx)
    {
        final IntMap<Float> fpresses = presses[idx];
        addKeyObserver(key, new PseudoKeys.Observer() {
            public void keyPressed (long when, int key, float amount) {
                fpresses.put(key, Float.valueOf(amount));
                updateAmount();
            }
            public void keyReleased (long when, int key) {
                fpresses.remove(key);
                updateAmount();
            }
            protected void updateAmount () {
                float maximum = 0f;
                for (float value : fpresses.values()) {
                    maximum = Math.max(maximum, value);
                }
                amounts[idx] = maximum;
                updateMoveFlag();
            }
        });
    }

    /**
     * Binds a key to the strafe flag.
     */
    protected void bindKeyStrafe (int key)
    {
        addKeyObserver(key, new PseudoKeys.Observer() {
            public void keyPressed (long when, int key, float amount) {
                _strafePresses.add(key);
            }
            public void keyReleased (long when, int key) {
                _strafePresses.remove(key);
            }
        });
    }

    /**
     * Adds an observer for a single key.
     */
    protected void addKeyObserver (int key, PseudoKeys.Observer observer)
    {
        addKeyObserver(key, observer, true);
    }

    /**
     * Adds an observer for a single key.
     *
     * @param hold if true and the key is pressed and released in the same frame, hold the release
     * event until the next frame so that we have a chance to process the press.
     */
    protected void addKeyObserver (final int key, PseudoKeys.Observer observer, boolean hold)
    {
        if (hold) {
            final PseudoKeys.Observer obs = observer;
            class Holder implements PseudoKeys.Observer, Runnable {
                public void keyPressed (long when, int key, float amount) {
                    obs.keyPressed(when, key, amount);
                    _pressTick = (_pressTick == 0) ? _tickCount : _pressTick;
                    _released = 0L;
                }
                public void keyReleased (long when, int key) {
                    if (_tickCount == _pressTick) {
                        _released = when;
                        if (!_posted) {
                            _ctx.getClient().getRunQueue().postRunnable(this);
                            _posted = true;
                        }
                    } else {
                        obs.keyReleased(when, key);
                    }
                    _pressTick = 0;
                }
                public void run () {
                    if (_released > 0L) {
                        obs.keyReleased(_released, key);
                    }
                    _posted = false;
                }
                protected int _pressTick;
                protected long _released;
                protected boolean _posted;
            }
            observer = new Holder();
        }

        ObserverList<PseudoKeys.Observer> list = _keyObservers.get(key);
        if (list == null) {
            _keyObservers.put(key, list = ObserverList.newFastUnsafe());
        }
        list.add(observer);

        if (PseudoKeys.singleton().hasAnyModifierKeys(key)) {
            _unifier.addModifierUser(PseudoKeys.singleton().getBaseKey(key));
        }
    }

    /**
     * Dispatches the given event to the hover sprite if we have one and the input window is
     * hovered.
     */
    protected void maybeDispatchToHoverSprite (Event event)
    {
        if (inputWindowHovered() && _hsprite != null && _hsprite.dispatchEvent(event)) {
            event.consume();
        }
    }

    /**
     * Determines whether we should process mouse events on the input window.
     */
    protected boolean inputWindowHovered ()
    {
        return _tsview.getInputWindow().getState() == Component.HOVER && !mouseCameraEnabled();
    }

    /**
     * Determines whether the input window is receiving/should receive keyboard/controller events.
     */
    protected boolean inputWindowFocused ()
    {
        Root root = _tctx.getRoot();
        if (root.getFocus() != null || mouseCameraEnabled()) {
            return false;
        }
        Window inputWindow = _tsview.getInputWindow();
        for (int ii = root.getWindowCount() - 1; ii >= 0; ii--) {
            Window window = root.getWindow(ii);
            if (window == inputWindow) {
                return true;
            } else if (window.isModal()) {
                return false;
            }
        }
        return false;
    }

    /**
     * Determines whether the mouse camera is enabled.
     */
    protected boolean mouseCameraEnabled ()
    {
        int mods = getMouseCameraModifiers();
        return mods != 0 && (_tctx.getRoot().getModifiers() & mods) == mods;
    }

    /**
     * Returns the combination of modifiers that activates the mouse camera, or 0 if the mouse
     * camera is not enabled.
     */
    protected int getMouseCameraModifiers ()
    {
        return 0;
    }

    /**
     * Updates the input for the current tick.
     */
    protected void updateInput (float elapsed)
    {
        boolean hovered = inputWindowHovered();
        Sprite nhsprite = null;
        if (hovered) {
            // get the pick ray
            Root root = _tctx.getRoot();
            _tctx.getCompositor().getCamera().getPickRay(
                root.getMouseX(), root.getMouseY(), _pick);

            // see if it intersects anything in the scene
            nhsprite = (_holdHover && (_hsprite == null || _hsprite.isClickable())) ?
                _hsprite : findHoverSprite(_pick);
        }

        // update the hover sprite
        if (_hsprite != nhsprite) {
            setHoverSprite(nhsprite);
        }

        // make sure we have our controllee for the rest
        ActorSprite controlledSprite = _tsview.getControlledSprite();
        if (controlledSprite == null) {
            return;
        }

        // update the direction if hovered
        float rotation = _lastRotation, direction = _lastDirection;
        if (hovered && controlledSprite == _tsview.getTargetSprite()) {
            // find the camera target plane
            Vector3f target = _tsview.getCameraHandler().getTarget();
            _tplane.set(Vector3f.UNIT_Z, -target.z);

            // determine where they intersect and use that to calculate the requested direction
            if (_tplane.getIntersection(_pick, _isect) && !_isect.equals(target)) {
                float dir = FloatMath.atan2(_isect.y - target.y, _isect.x - target.x);
                rotation = _strafePresses.isEmpty() ? dir : _lastRotation;
                direction = computeDirection(dir);
            }
        } else {
            direction = computeDirection(_lastRotation);
        }

        // clear the input if we don't have focus
        if (!inputWindowFocused()) {
            clearInput();
        }

        // perhaps enqueue an input frame
        long now = RunAnywhere.currentTimeMillis();
        if (rotation != _lastRotation || direction != _lastDirection ||
                _flags != _lastFlags || now >= _nextInput) {
            updateFrame(now, rotation, direction);

        // otherwise, just ensure that the advancers are up-to-date
        } else {
            int advancedTime = _tsview.getAdvancedTime();
            _advancer.advance(advancedTime);
            controlledSprite.getAdvancer().advance(advancedTime);
        }

        // have the sprite actor's translation smoothly approach that of the advancer actor
        controlledSprite.getActor().getTranslation().lerpLocal(
            _advancer.getActor().getTranslation(), 1f - FloatMath.exp(CONVERGENCE_RATE * elapsed));
    }

    /**
     * Updates the input frame.
     */
    protected void updateFrame (long now, float rotation, float direction)
    {
        // create and enqueue the frame
        InputFrame frame = _lastFrame = createInputFrame(
            _tsview.getAdvancedTime(), rotation, direction, _flags);
        _lastRotation = rotation;
        _lastDirection = direction;
        _lastFlags = _flags;
        _nextInput = now + _tsview.getElapsed();
        _input.add(frame);

        // apply it immediately to the controller and sprite advancers
        _advancer.advance(frame);
        ((PawnAdvancer)_tsview.getControlledSprite().getAdvancer()).advance(frame);

        // record the state
        _states.add(new PawnState(frame, (Pawn)_advancer.getActor().clone()));
    }

    /**
     * Finds the hover sprite that the pick ray intersects, if any.
     */
    protected Sprite findHoverSprite (Ray3D pick)
    {
        SceneElement element = _tsview.getScene().getIntersection(pick, _isect, HOVER_FILTER);
        return (element == null) ? null : (Sprite)element.getUserObject();
    }

    /**
     * Computes the direction of movement based on the requested direction.
     */
    protected float computeDirection (float dir)
    {
        // first check for an absolute direction, then for a relative one
        float fx = _absoluteMoveAmounts[3] - _absoluteMoveAmounts[2];
        float fy = _absoluteMoveAmounts[0] - _absoluteMoveAmounts[1];
        float flen = FloatMath.hypot(fx, fy);
        if (flen > 0.5f) {
            return FloatMath.atan2(fy, fx);
        }
        fx = _relativeMoveAmounts[3] - _relativeMoveAmounts[2];
        fy = _relativeMoveAmounts[0] - _relativeMoveAmounts[1];
        flen = FloatMath.hypot(fx, fy);
        return (flen > 0.5f) ? FloatMath.normalizeAngle(dir + FloatMath.atan2(-fx, fy)) : dir;
    }

    /**
     * Clears the input state, since the input window is not hovered.
     */
    protected void clearInput ()
    {
        int mask = ~getInputMask();
        _flags &= mask;
        _strafePresses.clear();
        clearDirection(_relativeMoveAmounts, _relativeMovePresses);
        clearDirection(_absoluteMoveAmounts, _absoluteMovePresses);
    }

    /**
     * Returns the set of all flags corresponding to input controls (i.e., the flags that should
     * be cleared when input is disabled).
     */
    protected int getInputMask ()
    {
        return InputFrame.MOVE;
    }

    /**
     * Clears directional state.
     */
    protected void clearDirection (float[] amounts, IntMap<Float>[] presses)
    {
        Arrays.fill(amounts, 0f);
        for (IntMap<Float> map : presses) {
            map.clear();
        }
    }

    /**
     * Creates an input frame.
     */
    protected InputFrame createInputFrame (
        int timestamp, float rotation, float direction, int flags)
    {
        return new InputFrame(timestamp, rotation, direction, flags);
    }

    /**
     * Sets the hover sprite.
     */
    protected void setHoverSprite (Sprite nhsprite)
    {
        Root root = _tctx.getRoot();
        if (_hsprite != null) {
            _hsprite.dispatchEvent(new MouseEvent(
                this, root.getTickStamp(), root.getModifiers(), MouseEvent.MOUSE_EXITED,
                root.getMouseX(), root.getMouseY()));
        }
        if ((_hsprite = nhsprite) != null) {
            _hsprite.dispatchEvent(new MouseEvent(
                this, root.getTickStamp(), root.getModifiers(), MouseEvent.MOUSE_ENTERED,
                root.getMouseX(), root.getMouseY()));
        }
        root.tipTextChanged(_tsview.getInputWindow());
    }

    /**
     * Sends all enqueued input to the server.
     */
    protected void transmitInput ()
    {
        // remove any input frames likely to be expired (except for the last one,
        // which the server will interpret as the most recent state); this means
        // any that precede the server's update window as estimated by adding ping
        // time to smoothed time (getting estimated server time of receipt) and
        // subtracting the elapsed time (plus a fudge factor) of the update
        int smoothedTime = _tsview.getSmoothedTime();
        int cutoffTime = smoothedTime + Math.max(0,
            _tsview.getPing() - Math.round(_tsview.getElapsed() * 1.1f));
        while (_input.size() > 1 && cutoffTime >= _input.get(0).getTimestamp()) {
            _input.remove(0);
        }

        // if we know we can't send datagrams, we know it will be received
        if (!_ctx.getClient().getTransmitDatagrams()) {
            _tsobj.tudeySceneService.enqueueInputReliable(
                _lastDelta, smoothedTime, _input.toArray(new InputFrame[_input.size()]));
            _input.clear();
            return;
        }

        // estimate the size of the transmission
        int size = 64; // various headers
        for (int ii = 0, nn = _input.size(); ii < nn; ii++) {
            size += _input.get(ii).getApproximateSize();
        }

        // remove frames until it's small enough
        int maxsize = (UPSTREAM_RATE_LIMIT * getTransmitInterval()) / 1000;
        while (size > maxsize) {
            size -= _input.remove(0).getApproximateSize();
        }
        _tsobj.tudeySceneService.enqueueInputUnreliable(
            _lastDelta, smoothedTime, _input.toArray(new InputFrame[_input.size()]));
    }

    /**
     * Switches to the next or previous potential target in the occupant list.
     */
    protected void cycleTarget (boolean forward)
    {
        // get all the potential targets in a list
        List<Integer> list = Lists.newArrayList();
        for (OccupantInfo info : _tsobj.occupantInfo) {
            int pawnId = ((TudeyOccupantInfo)info).pawnId;
            if (pawnId > 0) {
                list.add(pawnId);
            }
        }
        int size = list.size();
        if (size == 0) {
            return; // no available targets
        }

        // advance to the next or previous target
        int idx = Math.max(list.indexOf(_targetId), 0);
        int inc = forward ? 1 : (size - 1);
        setTarget(list.get((idx + inc) % size));
    }

    /**
     * Targets the pawn with the specified id.
     */
    protected void setTarget (int pawnId)
    {
        if (_targetId == pawnId) {
            return;
        }
        _tsobj.tudeySceneService.setTarget(_targetId = pawnId);
        _tsview.updateTargetSprite();
    }

    /**
     * Create the pseudokeys unifier we'll be using.
     */
    protected PseudoKeys.Unifier createUnifier ()
    {
        return new PseudoKeys.Unifier(this);
    }

    /**
     * Creates a map array to store per-direction pseudo-key presses.
     */
    protected static IntMap<Float>[] createDirectionPresses ()
    {
        @SuppressWarnings("unchecked")
        IntMap<Float>[] presses = (IntMap<Float>[])new IntMap<?>[4];
        for (int ii = 0; ii < 4; ii++) {
            presses[ii] = IntMaps.newHashIntMap();
        }
        return presses;
    }

    /**
     * Records the state of the controlled pawn at the time of an input frame (along with the
     * frame itself).
     */
    protected static class PawnState
    {
        /**
         * Creates a new pawn state.
         */
        public PawnState (InputFrame frame, Pawn pawn)
        {
            _frame = frame;
            _pawn = pawn;
        }

        /**
         * Returns a reference to the input frame.
         */
        public InputFrame getFrame ()
        {
            return _frame;
        }

        /**
         * Returns a reference to the pawn.
         */
        public Pawn getPawn ()
        {
            return _pawn;
        }

        /** The input frame. */
        protected InputFrame _frame;

        /** The pawn state. */
        protected Pawn _pawn;
    }

    /** A casted reference to the context. */
    protected TudeyContext _tctx;

    /** A casted reference to the scene view. */
    protected TudeySceneView _tsview;

    /** A casted reference to the scene object. */
    protected TudeySceneObject _tsobj;

    /** The orbiter used to control the camera, if any. */
    protected MouseOrbiter _orbiter;

    /** The id of the actor that the camera should track. */
    protected int _targetId;

    /** The id of the actor being controlled. */
    protected int _controlledId;

    /** The current hover sprite, if any. */
    protected Sprite _hsprite;

    /** When true, we hold the hover state. */
    protected boolean _holdHover;

    /** Translates various events into pseudo-key events. */
    protected PseudoKeys.Unifier _unifier = createUnifier();

    /** Maps pseudo-key codes to observers for individual keys. */
    protected IntMap<ObserverList<PseudoKeys.Observer>> _keyObservers = IntMaps.newHashIntMap();

    /** The current value of the input flags. */
    protected int _flags;

    /** Maps flags to corresponding presses pseudo-keys. */
    protected Multimap<Integer, Integer> _flagPresses = HashMultimap.create();

    /** The absolute move command amounts in each direction. */
    protected float[] _absoluteMoveAmounts = new float[4];

    /** The relative move command amounts in each direction. */
    protected float[] _relativeMoveAmounts = new float[4];

    /** Pseudo-key presses for each absolute direction. */
    protected IntMap<Float>[] _absoluteMovePresses = createDirectionPresses();

    /** Pseudo-key presses for each relative direction. */
    protected IntMap<Float>[] _relativeMovePresses = createDirectionPresses();

    /** The list of outgoing input frames. */
    protected List<InputFrame> _input = Lists.newArrayList();

    /** The last input frame added. */
    protected InputFrame _lastFrame;

    /** States recorded for input frames. */
    protected List<PawnState> _states = Lists.newArrayList();

    /** The advancer we use to update the controlled pawn state. */
    protected PawnAdvancer _advancer;

    /** The last rotation we transmitted. */
    protected float _lastRotation;

    /** The last direction we transmitted. */
    protected float _lastDirection;

    /** The last flags we transmitted. */
    protected int _lastFlags;

    /** The latest time at which we should enqueue an input frame. */
    protected long _nextInput;

    /** The time at which we last transmitted our input.  */
    protected long _lastTransmit;

    /** The timestamp of the last delta received from the server. */
    protected int _lastDelta;

    /** The set of keys pressed mapped to the strafe function. */
    protected HashIntSet _strafePresses = new HashIntSet();

    /** Incremented on each tick. */
    protected int _tickCount;

    /** Used for picking. */
    protected Ray3D _pick = new Ray3D();

    /** Contains the target plane for intersecting testing. */
    protected Plane _tplane = new Plane();

    /** Contains the result of the intersection test. */
    protected Vector3f _isect = new Vector3f();

    /** Holds averaged translation. */
    protected Vector2f _translation = new Vector2f();

    /** The exponential rate at which we converge upon the server-corrected translation. */
    protected static final float CONVERGENCE_RATE = 20f * FloatMath.log(0.5f);

    /** A rate limit (in bytes per second) for upstream traffic. */
    protected static final int UPSTREAM_RATE_LIMIT = 12 * 1024;

    /** Selects hoverable sprites. */
    protected static final Predicate<SceneElement> HOVER_FILTER = new Predicate<SceneElement>() {
        public boolean apply (SceneElement element) {
            Object obj = element.getUserObject();
            return obj instanceof Sprite && ((Sprite)obj).isHoverable();
        }
    };
}
