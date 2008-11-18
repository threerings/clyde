//
// $Id$

package com.threerings.tudey.client.handler;

import com.threerings.expr.SimpleScope;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.effect.Effect;

/**
 * Handles the display of a stateless effect.
 */
public abstract class EffectHandler extends SimpleScope
{
    /**
     * Creates a new handler.
     */
    public EffectHandler (GlContext ctx, TudeySceneView view)
    {
        super(view);
        _ctx = ctx;
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "handler";
    }

    /** The renderer context. */
    protected GlContext _ctx;
}
