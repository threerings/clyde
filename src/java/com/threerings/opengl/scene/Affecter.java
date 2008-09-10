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
import com.threerings.opengl.scene.config.AffecterConfig;
import com.threerings.opengl.util.DebugBounds;
import com.threerings.opengl.util.GlContext;

/**
 * A model implementation that exerts an effect on the viewer.
 */
public class Affecter extends Model.Implementation
{
    /**
     * Creates a new affecter implementation.
     */
    public Affecter (GlContext ctx, Scope parentScope, AffecterConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        setConfig(config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (AffecterConfig config)
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

            // update the effect bounds if we're in a scene
            Scene scene = ((Model)_parentScope).getScene();
            if (scene != null) {
                scene.boundsWillChange(_effect);
            }
            _effect.getBounds().set(_nbounds);
            if (scene != null) {
                scene.boundsDidChange(_effect);
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
        ((Model)_parentScope).getScene().add(_effect);
    }

    @Override // documentation inherited
    public void willBeRemoved ()
    {
        ((Model)_parentScope).getScene().remove(_effect);
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
        // remove the old effect, if any
        Scene scene = ((Model)_parentScope).getScene();
        if (scene != null && _effect != null) {
            scene.remove(_effect);
        }

        // create the effect
        _effect = _config.effect.createViewerEffect(_ctx, this);
        _effect.getBounds().set(_bounds);

        // add to scene if we're in one
        if (scene != null) {
            scene.add(_effect);
        }

        // update the bounds
        updateBounds();
    }

    /** The application context. */
    protected GlContext _ctx;

    /** The model configuration. */
    protected AffecterConfig _config;

    /** The effect. */
    protected ViewerEffect _effect;

    /** The parent world transform. */
    @Bound("worldTransform")
    protected Transform3D _parentWorldTransform;

    /** The local transform. */
    @Bound
    protected Transform3D _localTransform;

    /** The world transform. */
    @Scoped
    protected Transform3D _worldTransform = new Transform3D();

    /** The bounds of the system. */
    protected Box _bounds = new Box();

    /** Holds the bounds of the model when updating. */
    protected Box _nbounds = new Box();
}
