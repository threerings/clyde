//
// $Id$

package com.threerings.tudey.client.cursor;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.shape.Shape;

/**
 * Represents an entry.
 */
public abstract class EntryCursor extends Cursor
{
    /**
     * Creates a new entry cursor.
     */
    public EntryCursor (GlContext ctx, TudeySceneView view)
    {
        super(ctx, view);
    }

    /**
     * Returns a reference to the most recently set entry state.
     */
    public abstract Entry getEntry ();

    /**
     * Returns a reference to the shape of the entry (or <code>null</code> for none).
     */
    public abstract Shape getShape ();

    /**
     * Updates the cursor with new entry state.
     */
    public abstract void update (Entry entry);

    /** The color to use for footprints. */
    protected static final Color4f FOOTPRINT_COLOR = Color4f.CYAN;
}
