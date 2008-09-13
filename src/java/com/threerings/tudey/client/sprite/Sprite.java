//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.expr.SimpleScope;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;

/**
 * Represents a placeable object.
 */
public abstract class Sprite extends SimpleScope
{
    /**
     * Creates a new sprite.
     */
    public Sprite (GlContext ctx, TudeySceneView view)
    {
        super(view);
        _ctx = ctx;
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "sprite";
    }

    /** The renderer context. */
    protected GlContext _ctx;
}
