//
// $Id$

package com.threerings.opengl.eff;

import com.threerings.expr.Scope;

import com.threerings.opengl.effect.config.ParticleSystemConfig;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.util.GlContext;

/**
 * The particle system model implementation.
 */
public class ParticleSystem extends Model.Implementation
{
    /**
     * Creates a new particle system implementation.
     */
    public ParticleSystem (GlContext ctx, Scope parentScope, ParticleSystemConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        setConfig(config);
    }

    /**
     * Sets the configuration of this model.
     */
    public void setConfig (ParticleSystemConfig config)
    {
        _config = config;
        updateFromConfig();
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
    }

    /**
     * Updates the model to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {

    }

    /** The application context. */
    protected GlContext _ctx;

    /** The system configuration. */
    protected ParticleSystemConfig _config;
}
