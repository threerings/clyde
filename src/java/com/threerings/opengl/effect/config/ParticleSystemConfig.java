//
// $Id$

package com.threerings.opengl.effect.config;

import com.threerings.expr.Scope;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.GlContext;

/**
 * The configuration of a particle system.
 */
public class ParticleSystemConfig extends ModelConfig.Implementation
{
    @Override // documentation inherited
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        return null;
    }
}
