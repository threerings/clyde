//
// $Id$

package com.threerings.tudey.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.samskivert.util.HashIntMap;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;
import com.threerings.math.Ray3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.GlView;
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
import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.Sprite;
import com.threerings.tudey.client.util.TimeSmoother;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.dobj.ActorDelta;
import com.threerings.tudey.dobj.AddedActor;
import com.threerings.tudey.dobj.RemovedActor;
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
    public long getDelayedTime ()
    {
        return _smoothedTime - getBufferDelay();
    }

    /**
     * Returns the smoothed estimate of the server time (plus network latency) calculated at
     * the start of each tick.
     */
    public long getSmoothedTime ()
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
     * Processes a scene delta received from the server.
     */
    public void processSceneDelta (SceneDeltaEvent event)
    {
        // create/update the timer smoother
        long timestamp = event.getTimestamp();
        if (_smoother == null) {
            _smoother = new TimeSmoother(timestamp);
            _smoothedTime = timestamp;
        } else {
            _smoother.update(timestamp);
        }

        // create sprites for the actors added
        AddedActor[] addedActors = event.getAddedActors();
        if (addedActors != null) {
            for (AddedActor added : addedActors) {
            }
        }

        // notify the sprites of updated actors
        ActorDelta[] updatedActors = event.getUpdatedActors();
        if (updatedActors != null) {
            for (ActorDelta updated : updatedActors) {
                ActorSprite sprite = _actorSprites.get(updated.getId());
                if (sprite != null) {

                } else {
                    log.warning("Missing sprite for updated actor.", "id", updated.getId());
                }
            }
        }

        // notify the sprites of removed actors
        RemovedActor[] removedActors = event.getRemovedActors();
        if (removedActors != null) {
            for (RemovedActor removed : removedActors) {
                ActorSprite sprite = _actorSprites.remove(removed.getId());
                if (sprite != null) {
                    sprite.wasRemoved(removed.getTimestamp());
                } else {
                    log.warning("Missing sprite for removed actor.", "id", removed.getId());
                }
            }
        }

        // enqueue the effects for handling
        Effect[] effects = event.getEffects();
        if (effects != null) {
            for (Effect effect : effects) {
                timestamp = effect.getTimestamp();
                if (timestamp <= _lastEffect) {
                    continue; // already processed
                }
                _lastEffect = timestamp;
                _pendingEffects.add(effect);
            }
        }
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
        if (_smoother != null) {
            _smoothedTime = _smoother.getTime();
            long delayedTime = getDelayedTime();

            // fire off any pending effects
            while (!_pendingEffects.isEmpty() &&
                    delayedTime >= _pendingEffects.get(0).getTimestamp()) {
                _pendingEffects.remove(0).handle(_ctx, this);
            }
        }

        // tick the controller, if present
        if (_ctrl != null) {
            _ctrl.tick(elapsed);
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
     * Returns the delay with which to display information received from the server in order to
     * compensate for network jitter and dropped packets.
     */
    protected long getBufferDelay ()
    {
        return 100L;
    }

    /**
     * Adds a sprite for the specified entry.
     */
    protected void addEntrySprite (Entry entry)
    {
        _entrySprites.put(entry.getKey(), entry.createSprite(_ctx, this));
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
    protected long _smoothedTime;

    /** Sprites corresponding to the scene entries. */
    protected HashMap<Object, EntrySprite> _entrySprites = new HashMap<Object, EntrySprite>();

    /** Sprites corresponding to the actors in the scene. */
    protected HashIntMap<ActorSprite> _actorSprites = new HashIntMap<ActorSprite>();

    /** The timestamp of the last effect processed. */
    protected long _lastEffect;

    /** Effects waiting to be handled. */
    protected ArrayList<Effect> _pendingEffects = new ArrayList<Effect>();
}
