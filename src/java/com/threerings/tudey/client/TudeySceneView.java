//
// $Id$

package com.threerings.tudey.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap.IntEntry;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.math.FloatMath;
import com.threerings.math.Ray3D;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.GlView;
import com.threerings.opengl.camera.OrbitCameraHandler;
import com.threerings.opengl.gui.StretchWindow;
import com.threerings.opengl.gui.StyleSheet;
import com.threerings.opengl.gui.Window;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.scene.HashScene;
import com.threerings.opengl.scene.SceneElement;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

import com.samskivert.util.Predicate;

import com.threerings.tudey.client.sprite.ActorSprite;
import com.threerings.tudey.client.sprite.EffectSprite;
import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.Sprite;
import com.threerings.tudey.client.sprite.TileSprite;
import com.threerings.tudey.client.util.TimeSmoother;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.dobj.ActorDelta;
import com.threerings.tudey.dobj.SceneDeltaEvent;
import com.threerings.tudey.util.TudeyContext;

import static com.threerings.tudey.Log.*;

/**
 * Displays a view of a Tudey scene.
 */
public class TudeySceneView extends SimpleScope
    implements GlView, PlaceView, TudeySceneModel.Observer
{
    /**
     * An interface for objects (such as sprites and observers) that require per-tick updates.
     */
    public interface TickParticipant
    {
        /**
         * Ticks the participant.
         *
         * @param delayedTime the current delayed client time.
         * @return true to continue ticking the participant, false to remove it from the list.
         */
        public boolean tick (int delayedTime);
    }

    /**
     * Creates a new scene view for use in the editor.
     */
    public TudeySceneView (TudeyContext ctx)
    {
        this(ctx, null);
    }

    /**
     * Creates a new scene view.
     */
    public TudeySceneView (TudeyContext ctx, TudeySceneController ctrl)
    {
        super(ctx.getScope());
        _ctx = ctx;
        _ctrl = ctrl;
        _scene = new HashScene(ctx, 64f, 6);
        _scene.setParentScope(this);

        // create the input window
        _inputWindow = new StretchWindow(ctx, null, null) {
            public boolean shouldShadeBehind () {
                return false;
            }
            protected void configureStyle (StyleSheet style) {
                // no need to configure style; we're just a transparent window
            }
        };
        _inputWindow.setModal(true);

        // insert the baseline (empty) update record
        _records.add(new UpdateRecord(0, new HashIntMap<Actor>()));
    }

    /**
     * Returns a reference to the scene controller.
     */
    public TudeySceneController getController ()
    {
        return _ctrl;
    }

    /**
     * Returns a reference to the window used to gather input events.
     */
    public Window getInputWindow ()
    {
        return _inputWindow;
    }

    /**
     * Returns a reference to the view scene.
     */
    public HashScene getScene ()
    {
        return _scene;
    }

    /**
     * Returns the delayed client time, which is the smoothed time minus a delay that compensates
     * for network jitter and dropped packets.
     */
    public int getDelayedTime ()
    {
        return _smoothedTime - getBufferDelay();
    }

    /**
     * Returns the delay with which to display information received from the server in order to
     * compensate for network jitter and dropped packets.
     */
    public int getBufferDelay ()
    {
        return 100;
    }

    /**
     * Returns the advanced time, which is the smoothed time plus an interval that compensates for
     * buffering and latency.
     */
    public int getAdvancedTime ()
    {
        return _smoothedTime + getInputAdvance();
    }

    /**
     * Returns the interval ahead of the smoothed server time (which estimates the server time plus
     * one-way latency) at which we schedule input events.  This should be at least the transmit
     * interval (which represents the maximum amount of time that events may be delayed) plus the
     * two-way latency.
     */
    public int getInputAdvance ()
    {
        // TODO: smooth the ping time and incorporate into advance
        return _ctrl.getTransmitInterval() + 100;
    }

    /**
     * Returns the smoothed estimate of the server time (plus network latency) calculated at
     * the start of each tick.
     */
    public int getSmoothedTime ()
    {
        return _smoothedTime;
    }

    /**
     * Sets the scene model for this view.
     */
    public void setSceneModel (TudeySceneModel model)
    {
        // clear out the existing sprites
        if (_sceneModel != null) {
            _sceneModel.removeObserver(this);
        }
        for (EntrySprite sprite : _entrySprites.values()) {
            sprite.dispose();
        }
        _entrySprites.clear();

        // create the new sprites
        (_sceneModel = model).addObserver(this);
        for (Entry entry : _sceneModel.getEntries()) {
            addEntrySprite(entry);
        }
    }

    /**
     * Returns the sprite corresponding to the entry with the given key.
     */
    public EntrySprite getEntrySprite (Object key)
    {
        return _entrySprites.get(key);
    }

    /**
     * Returns a reference to the target sprite.
     */
    public ActorSprite getTargetSprite ()
    {
        return _targetSprite;
    }

    /**
     * Checks for an intersection between the provided ray and the sprites in the scene.
     *
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first sprite intersected by the ray, or <code>null</code> for
     * none.
     */
    public Sprite getIntersection (Ray3D ray, Vector3f location)
    {
        Predicate<Sprite> filter = Predicate.trueInstance();
        return getIntersection(ray, location, filter);
    }

    /**
     * Checks for an intersection between the provided ray and the sprites in the scene.
     *
     * @param location a vector to populate with the location of the intersection, if any.
     * @return a reference to the first sprite intersected by the ray, or <code>null</code> for
     * none.
     */
    public Sprite getIntersection (Ray3D ray, Vector3f location, final Predicate<Sprite> filter)
    {
        SceneElement el = _scene.getIntersection(ray, location, new Predicate<SceneElement>() {
            public boolean isMatch (SceneElement element) {
                Object userObject = element.getUserObject();
                return userObject instanceof Sprite && filter.isMatch((Sprite)userObject);
            }
        });
        return (el == null) ? null : (Sprite)el.getUserObject();
    }

    /**
     * Gets the transform of an object on the floor with the provided coordinates.
     */
    public Transform3D getFloorTransform (float x, float y, float rotation)
    {
        return getFloorTransform(x, y, rotation, new Transform3D(Transform3D.UNIFORM));
    }

    /**
     * Gets the transform of an object on the floor with the provided coordinates.
     */
    public Transform3D getFloorTransform (float x, float y, float rotation, Transform3D result)
    {
        Vector3f translation = result.getTranslation();
        translation.set(x, y, getFloorZ(x, y, translation.z));
        result.getRotation().fromAngleAxis(FloatMath.HALF_PI + rotation, Vector3f.UNIT_Z);
        return result;
    }

    /**
     * Returns the z coordinate of the floor at the provided coordinates, or the provided default
     * if no floor is found.
     */
    public float getFloorZ (float x, float y, float defvalue)
    {
        _ray.getOrigin().set(x, y, 10000f);
        return (_scene.getIntersection(_ray, _isect, TILE_SPRITE_FILTER) == null) ?
            defvalue : _isect.z;
    }

    /**
     * Processes a scene delta received from the server.
     */
    public void processSceneDelta (SceneDeltaEvent event)
    {
        // create/update the timer smoother
        int timestamp = event.getTimestamp();
        if (_smoother == null) {
            _smoother = new TimeSmoother(timestamp);
            _smoothedTime = timestamp;
        } else {
            _smoother.update(timestamp);
        }

        // store the ping estimate
        _ping = event.getPing();

        // remove all records before the reference
        int reference = event.getReference();
        while (reference > _records.get(0).getTimestamp()) {
            _records.remove(0);
        }
        HashIntMap<Actor> oactors = _records.get(0).getActors();
        HashIntMap<Actor> actors = new HashIntMap<Actor>();

        // start with all the old actors
        actors.putAll(oactors);

        // add any new actors
        Actor[] added = event.getAddedActors();
        if (added != null) {
            for (Actor actor : added) {
                actor.init(_ctx.getConfigManager());
                Actor oactor = actors.put(actor.getId(), actor);
                if (oactor != null) {
                    log.warning("Replacing existing actor.", "oactor", oactor, "nactor", actor);
                }
            }
        }

        // update any updated actors
        ActorDelta[] updated = event.getUpdatedActorDeltas();
        if (updated != null) {
            for (ActorDelta delta : updated) {
                int id = delta.getId();
                Actor oactor = actors.get(id);
                if (oactor != null) {
                    Actor nactor = (Actor)delta.apply(oactor);
                    nactor.init(_ctx.getConfigManager());
                    actors.put(id, nactor);
                } else {
                    log.warning("Missing actor for delta.", "delta", delta);
                }
            }
        }

        // remove any removed actors
        int[] removed = event.getRemovedActorIds();
        if (removed != null) {
            for (int id : removed) {
                actors.remove(id);
            }
        }

        // record the update
        _records.add(new UpdateRecord(timestamp, actors));

        // create/update the sprites for actors in the set
        for (Actor actor : actors.values()) {
            int id = actor.getId();
            ActorSprite sprite = _actorSprites.get(id);
            if (sprite == null) {
                _actorSprites.put(id, sprite = new ActorSprite(_ctx, this, timestamp, actor));
                if (id == _ctrl.getTargetId()) {
                    _targetSprite = sprite;
                }
            } else {
                if (id == _ctrl.getTargetId() && _ctrl.isTargetControlled()) {
                    _ctrl.controlledTargetUpdated(timestamp, actor);
                } else {
                    sprite.update(timestamp, actor);
                }
            }
        }

        // remove sprites for actors no longer in the set
        for (Iterator<IntEntry<ActorSprite>> it = _actorSprites.intEntrySet().iterator();
                it.hasNext(); ) {
            IntEntry<ActorSprite> entry = it.next();
            if (!actors.containsKey(entry.getIntKey())) {
                ActorSprite sprite = entry.getValue();
                sprite.remove(timestamp);
                if (_targetSprite == sprite) {
                    _targetSprite = null;
                }
                it.remove();
            }
        }

        // create handlers for any effects fired since the last update
        Effect[] fired = event.getEffectsFired();
        if (fired != null) {
            int last = _records.get(_records.size() - 2).getTimestamp();
            for (Effect effect : fired) {
                if (effect.getTimestamp() > last) {
                    effect.init(_ctx.getConfigManager());
                    new EffectSprite(_ctx, this, effect);
                }
            }
        }
    }

    /**
     * Adds a participant to tick at each frame.
     */
    public void addTickParticipant (TickParticipant participant)
    {
        _tickParticipants.add(participant);
    }

    /**
     * Removes a participant from the tick list.
     */
    public void removeTickParticipant (TickParticipant participant)
    {
        _tickParticipants.remove(participant);
    }

    /**
     * Updates the target sprite based on the target id.
     */
    public void updateTargetSprite ()
    {
        _targetSprite = _actorSprites.get(_ctrl.getTargetId());
    }

    // documentation inherited from interface GlView
    public void wasAdded ()
    {
        _ctx.getRoot().addWindow(_inputWindow);
    }

    // documentation inherited from interface GlView
    public void wasRemoved ()
    {
        _ctx.getRoot().removeWindow(_inputWindow);
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // update the smoothed time, if possible
        if (_smoother != null) {
            _smoothedTime = _smoother.getTime();
        }

        // tick the controller, if present
        if (_ctrl != null) {
            _ctrl.tick(elapsed);
        }

        // tick the participants in reverse order, to allow removal
        int delayedTime = getDelayedTime();
        for (int ii = _tickParticipants.size() - 1; ii >= 0; ii--) {
            if (!_tickParticipants.get(ii).tick(delayedTime)) {
                _tickParticipants.remove(ii);
            }
        }

        // track the target sprite, if any
        if (_targetSprite != null) {
            Vector3f translation = _targetSprite.getModel().getLocalTransform().getTranslation();
            ((OrbitCameraHandler)_ctx.getCameraHandler()).getTarget().set(translation);
        }

        // tick the scene
        _scene.tick(elapsed);
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        _scene.enqueue();
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        setSceneModel((TudeySceneModel)_ctx.getSceneDirector().getScene().getSceneModel());
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        _smoother = null;
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (Entry entry)
    {
        addEntrySprite(entry);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        EntrySprite sprite = _entrySprites.get(nentry.getKey());
        if (sprite != null) {
            sprite.update(nentry);
        } else {
            log.warning("Missing sprite to update.", "entry", nentry);
        }
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (Entry oentry)
    {
        EntrySprite sprite = _entrySprites.remove(oentry.getKey());
        if (sprite != null) {
            sprite.dispose();
        } else {
            log.warning("Missing entry sprite to remove.", "entry", oentry);
        }
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "view";
    }

    /**
     * Adds a sprite for the specified entry.
     */
    protected void addEntrySprite (Entry entry)
    {
        _entrySprites.put(entry.getKey(), entry.createSprite(_ctx, this));
    }

    /**
     * Contains the state at a single update.
     */
    protected static class UpdateRecord
    {
        /**
         * Creates a new update record.
         */
        public UpdateRecord (int timestamp, HashIntMap<Actor> actors)
        {
            _timestamp = timestamp;
            _actors = actors;
        }

        /**
         * Returns the timestamp of this update.
         */
        public int getTimestamp ()
        {
            return _timestamp;
        }

        /**
         * Returns the map of actors.
         */
        public HashIntMap<Actor> getActors ()
        {
            return _actors;
        }

        /** The timestamp of the update. */
        protected int _timestamp;

        /** The states of the actors. */
        protected HashIntMap<Actor> _actors;
    }

    /** The application context. */
    protected TudeyContext _ctx;

    /** The controller that created this view. */
    protected TudeySceneController _ctrl;

    /** A window used to gather input events. */
    protected Window _inputWindow;

    /** The OpenGL scene. */
    @Scoped
    protected HashScene _scene;

    /** The scene model. */
    protected TudeySceneModel _sceneModel;

    /** Smoother used to provide a smoothed time estimate. */
    protected TimeSmoother _smoother;

    /** The smoothed time. */
    protected int _smoothedTime;

    /** The estimated ping time. */
    protected int _ping;

    /** Records of each update received from the server. */
    protected ArrayList<UpdateRecord> _records = new ArrayList<UpdateRecord>();

    /** Sprites corresponding to the scene entries. */
    protected HashMap<Object, EntrySprite> _entrySprites = new HashMap<Object, EntrySprite>();

    /** Sprites corresponding to the actors in the scene. */
    protected HashIntMap<ActorSprite> _actorSprites = new HashIntMap<ActorSprite>();

    /** The list of participants in the tick. */
    protected ArrayList<TickParticipant> _tickParticipants = new ArrayList<TickParticipant>();

    /** The sprite that the camera is tracking. */
    protected ActorSprite _targetSprite;

    /** Used to find the floor. */
    protected Ray3D _ray = new Ray3D(Vector3f.ZERO, new Vector3f(0f, 0f, -1f));

    /** Used to find the floor. */
    protected Vector3f _isect = new Vector3f();

    /** A predicate that only accepts elements whose user objects are tile sprites. */
    protected static final Predicate<SceneElement> TILE_SPRITE_FILTER =
        new Predicate<SceneElement>() {
        public boolean isMatch (SceneElement element) {
            return element.getUserObject() instanceof TileSprite;
        }
    };
}
