//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.TudeySceneModel.AreaEntry;
import com.threerings.tudey.data.TudeySceneModel.Entry;

/**
 * Represents an area entry.
 */
public class AreaSprite extends EntrySprite
{
    /**
     * Creates a new area sprite.
     */
    public AreaSprite (GlContext ctx, TudeySceneView view, AreaEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    @Override // documentation inherited
    public Entry getEntry ()
    {
        return _entry;
    }

    @Override // documentation inherited
    public void update (Entry entry)
    {
        _entry = (AreaEntry)entry;
    }

    /** The scene entry. */
    protected AreaEntry _entry;
}
