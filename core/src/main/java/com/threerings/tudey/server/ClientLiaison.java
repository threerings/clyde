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

package com.threerings.tudey.server;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.presents.net.Transport;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.server.CrowdSession;

import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.CameraConfig;
import com.threerings.tudey.data.InputFrame;
import com.threerings.tudey.data.TudeySceneConfig;
import com.threerings.tudey.data.TudeySceneObject;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.dobj.ActorDelta;
import com.threerings.tudey.dobj.SceneDeltaEvent;
import com.threerings.tudey.server.logic.ActorLogic;
import com.threerings.tudey.server.logic.PawnLogic;
import com.threerings.tudey.util.TruncatedAverage;
import com.threerings.tudey.util.TudeySceneMetrics;

import static com.threerings.tudey.Log.log;

/**
 * Handles interaction with a single client.
 */
public class ClientLiaison
{
    /**
     * Creates a new liaison for the specified client.
     */
    public ClientLiaison (TudeySceneManager scenemgr, BodyObject bodyobj, CrowdSession session)
    {
        _scenemgr = scenemgr;
        _tsobj = (TudeySceneObject)scenemgr.getPlaceObject();
        _bodyobj = bodyobj;
        _session = session;

        // find the client's initial target
        int targetId = _tsobj.getPawnId(_bodyobj.getOid());
        if (targetId > 0) {
            _controlled = (PawnLogic)_scenemgr.getActorLogic(targetId);
        } else {
            targetId = _tsobj.getFirstPawnId();
        }
        _target = (PawnLogic)_scenemgr.getActorLogic(targetId);
        if (_controlled != null) {
            _controlled.bodyEntered(this);
        }
        _localInterest = _scenemgr.getDefaultLocalInterest();

        // insert the baseline (empty) tick record
        _records.add(new TickRecord());
    }

    /**
     * Notes that the client's occupant info has been updated.
     */
    public void bodyUpdated (OccupantInfo info)
    {
        if (info.status == OccupantInfo.DISCONNECTED) {
            // if they reconnect, they'll have to start again from the zero reference time
            _records.clear();
            _records.add(new TickRecord());
            _previousVisibleActors.clear();
            _visibleActors.clear();
            _receiving = false;
        }
    }

    /**
     * Sets the client's target actor.
     */
    public void setTarget (PawnLogic target)
    {
        _target = target;
    }

    /**
     * Returns a reference to the client's target.
     */
    public PawnLogic getTarget ()
    {
        return _target;
    }

    /**
     * Sets the client's camera parameters.
     */
    public void setCameraParams (CameraConfig config, float aspect)
    {
        _localInterest = TudeySceneMetrics.getLocalInterest(config, aspect);
    }

    /**
     * Computes and returns the difference between the time at which the client depicts actors that
     * it controls (its advanced time) and the time at which it depicts all other actors (its
     * delayed time).
     */
    public int getControlDelta ()
    {
        TudeySceneConfig config = (TudeySceneConfig)_scenemgr.getConfig();
        return _scenemgr.getBufferDelay() + config.getInputAdvance(_pingAverage.value());
    }

    /**
     * Returns the timestamp of the last input frame received from the client (or zero if none
     * have yet been received).
     */
    public int getLastInput ()
    {
        return _lastInput;
    }

    /**
     * Notes that the client has successfully entered the place.
     */
    public void enteredPlace ()
    {
        _receiving = true;
    }

    /**
     * Processes a request to enqueue input received from a client.
     *
     * @param ping the ping calculated from the current time and the client's time estimate.
     */
    public void enqueueInput (int acknowledge, int ping, InputFrame[] frames)
    {
        // ignore input send after disconnect
        if (!_receiving) {
            return;
        }

        // remove all tick records up to (but not including) the acknowledgement
        while (acknowledge > _records.get(0).getTimestamp()) {
            if (_records.size() > 1) {
                _records.remove(0);
            } else {
                log.warning("Received invalid acknowledgement.", "who", _bodyobj,
                    "acknowledge", acknowledge, "last", _records.get(0).getTimestamp());
                break;
            }
        }

        // remember ping
        _pingAverage.record(_ping = ping);

        // if we do not control the target, we do not process the input
        if (_controlled == null) {
            if (frames.length > 0) {
                log.warning("Got input frames for non-controlled pawn.", "who", _bodyobj,
                    "actor", (_target == null) ? null : _target.getActor());
            }
            return;
        }

        // enqueue input frames
        int timestamp = _scenemgr.getTimestamp();
        for (int ii = 0; ii < frames.length; ii++) {
            InputFrame frame = frames[ii];
            int input = frame.getTimestamp();
            if (input <= _lastInput) {
                continue; // already processed
            }
            _lastInput = input;

            // discard any out of date frames except for the last one,
            // which we will interpret as the most recent
            if (input > timestamp || ii == frames.length - 1) {
                _controlled.enqueueInput(frame);
            } else {
                log.debug("Discarding out-of-date frame.", "frame", frame);
            }
        }
    }

    /**
     * Posts the scene delta for this client, informing it any all relevant changes to the scene
     * since its last acknowledged delta.
     */
    public void postDelta ()
    {
        // no need to do anything if not yet receiving
        if (!_receiving) {
            return;
        }

        // if any deltas were sent with reliable transport, we can consider them received
        for (int ii = _records.size() - 1; ii > 0; ii--) {
            if (_records.get(ii).event.getActualTransport() == Transport.RELIABLE_ORDERED) {
                _records.subList(0, ii).clear();
                break;
            }
        }

        // translate the local interest bounds based on the actor translation
        Vector2f translation = (_target == null) ?
            Vector2f.ZERO : _target.getActor().getTranslation();
        _localInterest.getMinimumExtent().add(translation, _worldInterest.getMinimumExtent());
        _localInterest.getMaximumExtent().add(translation, _worldInterest.getMaximumExtent());

        // find all currently visible actors and compare to previous set
        populateVisibleActors();
        for (ActorLogic actor : _visibleActors) {
            if (_previousVisibleActors.remove(actor)) {
                ActorDelta delta = actor.getSnapshotDelta();
                if (delta != null) {
                    _actorsUpdated.add(delta);
                }
            } else {
                _actorsAdded.add(actor.getSnapshot());
            }
        }
        for (ActorLogic actor : _previousVisibleActors) {
            _actorsRemoved.add(actor.getPreviousSnapshot());
        }
        _previousVisibleActors.clear();

        // swap the previous and current visible sets
        Set<ActorLogic> visibleActors = _visibleActors;
        _visibleActors = _previousVisibleActors;
        _previousVisibleActors = visibleActors;

        // if this is the first recorded tick, we need to add the complete set of static
        // actors; afterwards, just the delta
        if (_records.get(_records.size() - 1).getTimestamp() == 0) {
            for (ActorLogic actor : _scenemgr.getStaticActors()) {
                _actorsAdded.add(actor.getSnapshot());
            }
        } else {
            for (ActorLogic actor : _scenemgr.getStaticActorsAdded()) {
                _actorsAdded.add(actor.getSnapshot());
            }
            for (ActorLogic actor : _scenemgr.getStaticActorsUpdated()) {
                ActorDelta delta = actor.getSnapshotDelta();
                if (delta != null) {
                    _actorsUpdated.add(delta);
                }
            }
            for (ActorLogic actor : _scenemgr.getStaticActorsRemoved()) {
                _actorsRemoved.add(actor.getPreviousSnapshot());
            }
        }

        // record the tick
        int timestamp = _scenemgr.getTimestamp();
        TickRecord record = new TickRecord(
            timestamp,
            _actorsAdded.toArray(new Actor[_actorsAdded.size()]),
            _actorsUpdated.toArray(new ActorDelta[_actorsUpdated.size()]),
            _actorsRemoved.toArray(new Actor[_actorsRemoved.size()]),
            _scenemgr.getEffectsFired(_target, _worldInterest));
        _records.add(record);
        _actorsAdded.clear();
        _actorsUpdated.clear();
        _actorsRemoved.clear();

        // the last acknowledged tick is the reference
        TickRecord reference = _records.get(0);

        // merge actor updates and get all effects fired (not expired)
        for (int ii = 1, nn = _records.size(); ii < nn; ii++) {
            TickRecord orecord = _records.get(ii);
            for (Actor actor : orecord.getActorsAdded()) {
                int id = actor.getId();
                Actor removed = _removed.remove(id);
                if (removed == null) {
                    _added.put(id, actor);
                } else {
                    ActorDelta delta = new ActorDelta(removed, actor);
                    if (!delta.isEmpty()) {
                        _updated.put(id, delta);
                    }
                }
            }
            for (ActorDelta delta : orecord.getActorsUpdated()) {
                int id = delta.getId();
                Actor added = _added.get(id);
                if (added != null) {
                    _added.put(id, (Actor)delta.apply(added));
                } else {
                    ActorDelta odelta = _updated.put(id, delta);
                    if (odelta != null) {
                        _updated.put(id, (ActorDelta)odelta.merge(delta));
                    }
                }
            }
            for (Actor actor : orecord.getActorsRemoved()) {
                int id = actor.getId();
                if (_added.remove(id) == null) {
                    _updated.remove(id);
                    _removed.put(id, actor);
                }
            }
            for (Effect effect : orecord.getEffectsFired()) {
                if (timestamp < effect.getExpiry()) {
                    _fired.add(effect);
                }
            }
        }

        // if we know that we can't transmit datagrams, we may as well send the delta as reliable
        // and immediately consider it received
        Transport transport = Transport.UNRELIABLE_UNORDERED;
        if (_session == null || !_session.getTransmitDatagrams()) {
            transport = Transport.RELIABLE_ORDERED;
            _records.subList(0, _records.size() - 1).clear();
        }

        // create and post the event
        int nadded = _added.size();
        int nupdated = _updated.size();
        int nfired = _fired.size();
        record.event = new SceneDeltaEvent(
            _bodyobj.getOid(), _tsobj.getOid(), _lastInput,
            (short)_ping, reference.getTimestamp(), timestamp,
            (short)(timestamp - _scenemgr.getPreviousTimestamp()),
            (nadded == 0) ? null : _added.values().toArray(new Actor[nadded]),
            (nupdated == 0) ? null : _updated.values().toArray(new ActorDelta[nupdated]),
            _removed.isEmpty() ? null : _removed.intKeySet().toIntArray(),
            (nfired == 0) ? null : _fired.toArray(new Effect[nfired]));
        record.event.setTransport(transport);
        _bodyobj.postEvent(record.event);

        // clear the arrays
        _added.clear();
        _updated.clear();
        _removed.clear();
        _fired.clear();
    }

    /**
     * Populates the set of visible actors.
     */
    protected void populateVisibleActors ()
    {
        _scenemgr.getVisibleActors(_target, _worldInterest, _visibleActors);
    }

    /**
     * Contains the state at a single tick.
     */
    protected static class TickRecord
    {
        /** A reference to the transmitted event. */
        public SceneDeltaEvent event;

        /**
         * Creates an empty reference record.
         */
        public TickRecord ()
        {
            this(0, new Actor[0], new ActorDelta[0], new Actor[0], new Effect[0]);
        }

        /**
         * Creates a new record.
         */
        public TickRecord (
            int timestamp, Actor[] actorsAdded, ActorDelta[] actorsUpdated,
            Actor[] actorsRemoved, Effect[] effectsFired)
        {
            _timestamp = timestamp;
            _actorsAdded = actorsAdded;
            _actorsUpdated = actorsUpdated;
            _actorsRemoved = actorsRemoved;
            _effectsFired = effectsFired;
        }

        /**
         * Returns the timestamp of this record.
         */
        public int getTimestamp ()
        {
            return _timestamp;
        }

        /**
         * Returns the actors added on this tick.
         */
        public Actor[] getActorsAdded ()
        {
            return _actorsAdded;
        }

        /**
         * Returns the deltas of actors updated on this tick.
         */
        public ActorDelta[] getActorsUpdated ()
        {
            return _actorsUpdated;
        }

        /**
         * Returns the states on the previous tick of actors removed on this tick.
         */
        public Actor[] getActorsRemoved ()
        {
            return _actorsRemoved;
        }

        /**
         * Returns the effects fired on this tick.
         */
        public Effect[] getEffectsFired ()
        {
            return _effectsFired;
        }

        /** The timestamp of this record. */
        protected int _timestamp;

        /** The actors added on this tick. */
        protected Actor[] _actorsAdded;

        /** The deltas of actors updated on this tick. */
        protected ActorDelta[] _actorsUpdated;

        /** The states on the previous tick of actors removed on this tick. */
        protected Actor[] _actorsRemoved;

        /** The effects fired on this tick. */
        protected Effect[] _effectsFired;
    }

    /** The scene manager that created the liaison. */
    protected TudeySceneManager _scenemgr;

    /** The scene object. */
    protected TudeySceneObject _tsobj;

    /** The client body object. */
    protected BodyObject _bodyobj;

    /** The client session. */
    protected CrowdSession _session;

    /** The pawn that the client's camera is tracking. */
    protected PawnLogic _target;

    /** The pawn that the client is controlling. */
    protected PawnLogic _controlled;

    /** The untranslated area of interest. */
    protected Rect _localInterest;

    /** The translated area of interest. */
    protected Rect _worldInterest = new Rect();

    /** Records of each update transmitted to the client. */
    protected List<TickRecord> _records = Lists.newArrayList();

    /** Set when we know that the client will be receiving on the client object. */
    protected boolean _receiving;

    /** The most recent ping time estimate. */
    protected int _ping;

    /** The trailing average of the ping times. */
    protected TruncatedAverage _pingAverage = new TruncatedAverage();

    /** The timestamp of the last input frame received from the client. */
    protected int _lastInput;

    /** The actors in the area of interest at the last update. */
    protected Set<ActorLogic> _previousVisibleActors = Sets.newHashSet();

    /** The actors in the area of interest on the current update. */
    protected Set<ActorLogic> _visibleActors = Sets.newHashSet();

    /** Holds actors added on the current tick. */
    protected List<Actor> _actorsAdded = Lists.newArrayList();

    /** Holds actors updated on the current tick. */
    protected List<ActorDelta> _actorsUpdated = Lists.newArrayList();

    /** Holds actors removed on the current tick. */
    protected List<Actor> _actorsRemoved = Lists.newArrayList();

    /** Stores added actors. */
    protected IntMap<Actor> _added = IntMaps.newHashIntMap();

    /** Stores updated actor deltas. */
    protected IntMap<ActorDelta> _updated = IntMaps.newHashIntMap();

    /** Stores removed actor. */
    protected IntMap<Actor> _removed = IntMaps.newHashIntMap();

    /** Stores effects fired. */
    protected List<Effect> _fired = Lists.newArrayList();
}
