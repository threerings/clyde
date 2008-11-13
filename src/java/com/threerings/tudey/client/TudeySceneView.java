//
// $Id$

package com.threerings.tudey.client;

import java.util.HashMap;

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

import com.threerings.tudey.client.sprite.EntrySprite;
import com.threerings.tudey.client.sprite.Sprite;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.util.TudeyContext;

import static com.threerings.tudey.Log.*;

/**
 * Displays a view of a Tudey scene.
 */
public class TudeySceneView extends SimpleScope
    implements GlView, PlaceView, TudeySceneModel.Observer
{
    /**
     * Creates a new scene view.
     */
    public TudeySceneView (TudeyContext ctx)
    {
        super(ctx.getScope());
        _ctx = ctx;
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

    /** The application context. */
    protected TudeyContext _ctx;

    /** A window used to gather input events. */
    protected Window _inputWindow;

    /** The OpenGL scene. */
    @Scoped
    protected HashScene _scene;

    /** The scene model. */
    protected TudeySceneModel _sceneModel;

    /** Sprites corresponding to the scene entries. */
    protected HashMap<Object, EntrySprite> _entrySprites = new HashMap<Object, EntrySprite>();
}
