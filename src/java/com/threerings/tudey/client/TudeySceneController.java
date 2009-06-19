//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import java.util.ArrayList;

import org.lwjgl.input.Keyboard;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Predicate;
import com.samskivert.util.RunAnywhere;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;

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
import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.data.TudeyOccupantInfo;
import com.threerings.tudey.data.TudeySceneConfig;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.actor.Pawn;
import com.threerings.tudey.dobj.SceneDeltaEvent;
import com.threerings.tudey.dobj.SceneDeltaListener;
import com.threerings.tudey.util.PawnAdvancer;
import com.threerings.tudey.util.TudeyContext;

import static com.threerings.tudey.Log.*;

/**
 * The basic Tudey scene controller class.
 */
public class TudeySceneController extends SceneController
    implements SceneDeltaListener, PseudoKeys.Observer, MouseListener,
        MouseMotionListener, MouseWheelListener,  Tickable
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
     * Checks whether this client is in control of the target.
     */
    public boolean isTargetControlled ()
    {
        return _targetControlled;
    }

    /**
     * Returns a reference to the hover sprite, if any.
     */
    public Sprite getHoverSprite ()
    {
        return _hsprite;
    }

    /**
     * Called by the view when we first add our controlled target.
     */
    public void controlledTargetAdded (int timestamp, Actor actor)
    {
        _advancer = (PawnAdvancer)actor.maybeCreateAdvancer(_tctx, _tsview, timestamp);
        _advancer.advance(_tsview.getAdvancedTime());
    }

    /**
     * Called by the view when we receive an update for our controlled target.
     */
    public void controlledTargetUpdated (int timestamp, Actor actor)
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

        // advance to current time
        _advancer.advance(advancedTime);

        // copy everything *except* the translation (which will be smoothed) to the sprite
        Actor sactor = _tsview.getTargetSprite().getActor();
        _translation.set(sactor.getTranslation());
        nactor.copy(sactor);
        if (!sactor.isSet(Actor.WARP)) {
            sactor.getTranslation().set(_translation);
        }
    }

    /**
     * Called by the view when we remove our controlled target.
     */
    public void controlledTargetRemoved (long timestamp)
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
                _ctx.getClient(), ((ActorSprite)source).getActor().getId(), name);
        } else if (source instanceof EntrySprite) {
            _tsobj.tudeySceneService.submitEntryRequest(
                _ctx.getClient(), ((EntrySprite)source).getEntry().getKey(), name);
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

    // documentation inherited from interface PseudoKeys.Observer
    public void keyPressed (long when, int key, float amount)
    {
        if (inputWindowHovered()) {
            PseudoKeys.Observer observer = _keyObservers.get(key);
            if (observer != null) {
                observer.keyPressed(when, key, amount);
            }
        }
    }

    // documentation inherited from interface PseudoKeys.Observer
    public void keyReleased (long when, int key)
    {
        if (inputWindowHovered()) {
            PseudoKeys.Observer observer = _keyObservers.get(key);
            if (observer != null) {
                observer.keyReleased(when, key);
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
        // update the input if we control our target
        if (_targetControlled) {
            updateInput(elapsed);
        }

        // perhaps transmit our acknowledgement and input frames
        long now = RunAnywhere.currentTimeMillis();
        if (now - _lastTransmit >= getTransmitInterval() && _lastDelta > 0) {
            transmitInput();
            _lastTransmit = now;
        }
    }

    @Override // documentation inherited
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

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();

        // remove the mouse orbiter
        if (_orbiter != null) {
            _tsview.getInputWindow().removeListener(_orbiter);
            _orbiter = null;
        }
    }

    @Override // documentation inherited
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

        // if the player controls a pawn, then the target is and will always be that pawn.
        // otherwise, the target starts out being the first pawn in the occupant list
        _targetId = _tsobj.getPawnId(clobj.getOid());
        if (_targetId > 0) {
            _targetControlled = true;
        } else {
            _targetId = _tsobj.getFirstPawnId();
        }

        // bind keys to actions
        bindKeys();

        // notify the server that we're in (it will start sending updates)
        _tsobj.tudeySceneService.enteredPlace(_ctx.getClient());
    }

    @Override // documentation inherited
    public void didLeavePlace (PlaceObject plobj)
    {
        super.didLeavePlace(plobj);

        // stop listening to the client object and input events
        _ctx.getClient().getClientObject().removeListener(this);
        _tsview.getInputWindow().removeListener(this);
        _tsview.getInputWindow().removeListener(_unifier);
    }

    @Override // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_tsview = new TudeySceneView((TudeyContext)ctx, this));
    }

    @Override // documentation inherited
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
        if (_targetControlled) {
            bindKeyFlag(PseudoKeys.KEY_BUTTON1, InputFrame.MOVE);
            bindKeyFlag(Keyboard.KEY_C, InputFrame.STRAFE);
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
        _keyObservers.put(key, new PseudoKeys.Observer() {
            public void keyPressed (long when, int key, float amount) {
                _flags |= flag;
                _frameFlags |= flag;
            }
            public void keyReleased (long when, int key) {
                _flags &= ~flag;
            }
        });
    }

    /**
     * Binds a key to cycle between targets.
     */
    protected void bindKeyCycle (int key, final boolean forward)
    {
        _keyObservers.put(key, new PseudoKeys.Adapter() {
            public void keyPressed (long when, int key, float amount) {
                cycleTarget(forward);
            }
        });
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
        // make sure we have our target
        ActorSprite targetSprite = _tsview.getTargetSprite();
        if (targetSprite == null) {
            return;
        }

        // if the mouse is over the input window, update the direction
        float direction = _lastDirection;
        Sprite nhsprite = null;

        if (inputWindowHovered()) {
            // get the pick ray
            Root root = _tctx.getRoot();
            _tctx.getCompositor().getCamera().getPickRay(
                root.getMouseX(), root.getMouseY(), _pick);

            // see if it intersects anything in the scene
            if (_holdHover) {
                nhsprite = _hsprite;
            } else {
                SceneElement element = _tsview.getScene().getIntersection(
                    _pick, _isect, HOVER_FILTER);
                nhsprite = (element == null) ? null : (Sprite)element.getUserObject();
            }

            // find the camera target plane
            Vector3f target = _tsview.getCameraHandler().getTarget();
            _tplane.set(Vector3f.UNIT_Z, -target.z);

            // determine where they intersect and use that to calculate the requested direction
            if (_tplane.getIntersection(_pick, _isect) && !_isect.equals(target)) {
                direction = FloatMath.atan2(_isect.y - target.y, _isect.x - target.x);
            }
        }

        // update the hover sprite
        if (_hsprite != nhsprite) {
            setHoverSprite(nhsprite);
        }

        // perhaps enqueue an input frame
        if (direction != _lastDirection || _frameFlags != _lastFlags) {
            // create and enqueue the frame
            InputFrame frame = createInputFrame(_tsview.getAdvancedTime(), direction, _frameFlags);
            _lastDirection = direction;
            _lastFlags = _frameFlags;
            _input.add(frame);

            // apply it immediately to the controller and sprite advancers
            _advancer.advance(frame);
            ((PawnAdvancer)targetSprite.getAdvancer()).advance(frame);

            // record the state
            _states.add(new PawnState(frame, (Pawn)_advancer.getActor().clone()));

        // otherwise, just ensure that the advancers are up-to-date
        } else {
            int advancedTime = _tsview.getAdvancedTime();
            _advancer.advance(advancedTime);
            targetSprite.getAdvancer().advance(advancedTime);
        }

        // have the sprite actor's translation smoothly approach that of the advancer actor
        targetSprite.getActor().getTranslation().lerpLocal(
            _advancer.getActor().getTranslation(), 1f - FloatMath.exp(CONVERGENCE_RATE * elapsed));

        // reset the frame flags
        _frameFlags = _flags;
    }

    /**
     * Creates an input frame.
     */
    protected InputFrame createInputFrame (int timestamp, float direction, int flags)
    {
        return new InputFrame(timestamp, direction, flags);
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
    }

    /**
     * Sends all enqueued input to the server.
     */
    protected void transmitInput ()
    {
        // remove any input frames guaranteed to be expired
        int smoothedTime = _tsview.getSmoothedTime();
        while (!_input.isEmpty() && smoothedTime >= _input.get(0).getTimestamp()) {
            _input.remove(0);
        }

        // estimate the size of the transmission
        InputFrame[] input = _input.toArray(new InputFrame[_input.size()]);
        int size = 64; // various headers
        for (InputFrame frame : input) {
            size += frame.getApproximateSize();
        }

        // transmit as datagram if we know we can and the size is under the threshold
        if (_ctx.getClient().getTransmitDatagrams() && size <= Client.MAX_DATAGRAM_SIZE) {
            _tsobj.tudeySceneService.enqueueInputUnreliable(
                _ctx.getClient(), _lastDelta, smoothedTime, input);
        } else {
            _tsobj.tudeySceneService.enqueueInputReliable(
                _ctx.getClient(), _lastDelta, smoothedTime, input);
            _input.clear();
        }
    }

    /**
     * Switches to the next or previous potential target in the occupant list.
     */
    protected void cycleTarget (boolean forward)
    {
        // get all the potential targets in a list
        ArrayList<Integer> list = new ArrayList<Integer>();
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
        _tsobj.tudeySceneService.setTarget(_ctx.getClient(), _targetId = pawnId);
        _tsview.updateTargetSprite();
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

    /** Whether or not we are in control of the camera target. */
    protected boolean _targetControlled;

    /** The current hover sprite, if any. */
    protected Sprite _hsprite;

    /** When true, we hold the hover state. */
    protected boolean _holdHover;

    /** Translates various events into pseudo-key events. */
    protected PseudoKeys.Unifier _unifier = new PseudoKeys.Unifier(this);

    /** Maps pseudo-key codes to observers for individual keys. */
    protected IntMap<PseudoKeys.Observer> _keyObservers = IntMaps.newHashIntMap();

    /** The current value of the input flags. */
    protected int _flags;

    /** Contains all flags set during the current frame. */
    protected int _frameFlags;

    /** The list of outgoing input frames. */
    protected ArrayList<InputFrame> _input = new ArrayList<InputFrame>();

    /** States recorded for input frames. */
    protected ArrayList<PawnState> _states = new ArrayList<PawnState>();

    /** The advancer we use to update the controlled pawn state. */
    protected PawnAdvancer _advancer;

    /** The last direction we transmitted. */
    protected float _lastDirection;

    /** The last flags we transmitted. */
    protected int _lastFlags;

    /** The time at which we last transmitted our input.  */
    protected long _lastTransmit;

    /** The timestamp of the last delta received from the server. */
    protected int _lastDelta;

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

    /** Selects hoverable sprites. */
    protected static final Predicate<SceneElement> HOVER_FILTER = new Predicate<SceneElement>() {
        public boolean isMatch (SceneElement element) {
            Object obj = element.getUserObject();
            return obj instanceof Sprite && ((Sprite)obj).isHoverable();
        }
    };
}
