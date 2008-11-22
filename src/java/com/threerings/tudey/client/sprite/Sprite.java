//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents a placeable object.
 */
public abstract class Sprite extends SimpleScope
{
    /**
     * Creates a new sprite.
     */
    public Sprite (TudeyContext ctx, TudeySceneView view)
    {
        super(view);
        _ctx = ctx;
        _view = view;
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "sprite";
    }

    /** The application context. */
    protected TudeyContext _ctx;

    /** The parent view. */
    @Scoped
    protected TudeySceneView _view;
}
