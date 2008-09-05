//
// $Id$

package com.threerings.opengl.scene;

import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.Scoped;
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
//        _meshes.bounds.transform(_worldTransform, _nbounds);
        if (!_bounds.equals(_nbounds)) {
            ((Model)_parentScope).boundsWillChange();
            _bounds.set(_nbounds);
            ((Model)_parentScope).boundsDidChange();
        }
    }

    @Override // documentation inherited
    public void drawBounds ()
    {
        DebugBounds.draw(_bounds, Color4f.WHITE);
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        // update the view transform
        _parentViewTransform.compose(_localTransform, _viewTransform);
    }

    @Override // documentation inherited
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        updateBounds();
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model configuration. */
    protected InfluencerConfig _config;

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
