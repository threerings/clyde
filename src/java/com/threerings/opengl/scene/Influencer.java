//
// $Id$

package com.threerings.opengl.scene;

import java.util.ArrayList;

import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.Scoped;
import com.threerings.expr.Updater;
import com.threerings.math.Box;
import com.threerings.math.Transform3D;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.scene.config.InfluencerConfig;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlContext;

/**
 * A model implementation that exerts an influence over scene elements.
 */
public class Influencer extends Model.Implementation
{
    /**
     * Creates a new influencer implementation.
     */
    public Influencer (GlContext ctx, Scope parentScope, InfluencerConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        setConfig(config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (InfluencerConfig config)
    {
        _config = config;
        updateFromConfig();
    }

    @Override // documentation inherited
    public Box getBounds ()
    {
        return _bounds;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        // update the world transform
        if (_parentWorldTransform == null) {
            return;
        }
        _parentWorldTransform.compose(_localTransform, _worldTransform);

        // and the world bounds
        _config.extent.transformBounds(_worldTransform, _nbounds);
        if (!_bounds.equals(_nbounds)) {
            ((Model)_parentScope).boundsWillChange();
            _bounds.set(_nbounds);
            ((Model)_parentScope).boundsDidChange();

            // update the influence bounds if we're in a scene
            Scene scene = ((Model)_parentScope).getScene();
            if (scene != null) {
                scene.boundsWillChange(_influence);
            }
            _influence.getBounds().set(_nbounds);
            if (scene != null) {
                scene.boundsDidChange(_influence);
            }
        }
    }

    @Override // documentation inherited
    public void drawBounds ()
    {
        DebugBounds.draw(_bounds, Color4f.WHITE);
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        ((Model)_parentScope).getScene().add(_influence);
    }

    @Override // documentation inherited
    public void willBeRemoved ()
    {
        ((Model)_parentScope).getScene().remove(_influence);
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        // update the view transform
        _parentViewTransform.compose(_localTransform, _viewTransform);

        // update the updaters
        for (Updater updater : _updaters) {
            updater.update();
        }
    }

    @Override // documentation inherited
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        updateFromConfig();
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        // remove the old influence, if any
        Scene scene = ((Model)_parentScope).getScene();
        if (scene != null && _influence != null) {
            scene.remove(_influence);
        }

        // create the influence and the updaters
        ArrayList<Updater> updaters = new ArrayList<Updater>();
        _influence = _config.influence.createSceneInfluence(_ctx, this, updaters);
        _influence.getBounds().set(_bounds);
        _updaters = updaters.toArray(new Updater[updaters.size()]);

        // add to scene if we're in one
        if (scene != null) {
            scene.add(_influence);
        }

        // update the bounds
        updateBounds();
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model configuration. */
    protected InfluencerConfig _config;

    /** The influence. */
    protected SceneInfluence _influence;

    /** Updaters to update before enqueuing. */
    protected Updater[] _updaters;

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The parent view transform. */
    @Bound("viewTransform")
    protected Transform3D _parentViewTransform;

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The world transform. */
    @Scoped
    protected Transform3D _worldTransform = new Transform3D();

    /** The view transform. */
    @Scoped
    protected Transform3D _viewTransform = new Transform3D();

    /** The bounds of the system. */
    protected Box _bounds = new Box();

    /** Holds the bounds of the model when updating. */
    protected Box _nbounds = new Box();
}
