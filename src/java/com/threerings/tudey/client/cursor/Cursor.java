//
// $Id$

package com.threerings.tudey.client.cursor;

import com.threerings.expr.SimpleScope;

import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.util.TudeyContext;

/**
 * Base class for cursors (representations used when placing objects).
 */
public abstract class Cursor extends SimpleScope
    implements Tickable, Renderable
{
    /**
     * Creates the cursor.
     */
    public Cursor (TudeyContext ctx, TudeySceneView view)
    {
        super(view);
        _ctx = ctx;
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        // nothing by default
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // nothing by default
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "cursor";
    }

    /** The application context. */
    protected TudeyContext _ctx;
}
