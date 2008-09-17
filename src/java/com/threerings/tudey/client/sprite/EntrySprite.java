//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.TudeySceneModel.Entry;

/**
 * Represents a placeable object.
 */
public abstract class EntrySprite extends Sprite
{
    /**
     * Creates a new entry sprite.
     */
    public EntrySprite (GlContext ctx, TudeySceneView view)
    {
        super(ctx, view);
    }

    /**
     * Returns a reference to the most recently set entry state.
     */
    public abstract Entry getEntry ();

    /**
     * Updates the sprite with new entry state.
     */
    public abstract void update (Entry entry);
}
