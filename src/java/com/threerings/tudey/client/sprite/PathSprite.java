//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.PathEntry;

/**
 * Represents a path entry.
 */
public class PathSprite extends EntrySprite
{
    /**
     * Creates a new path sprite.
     */
    public PathSprite (GlContext ctx, TudeySceneView view, PathEntry entry)
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
        _entry = (PathEntry)entry;
    }

    /** The scene entry. */
    protected PathEntry _entry;
}
