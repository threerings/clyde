//
// $Id$

package com.threerings.tudey.data;

import com.threerings.tudey.client.ActorSprite;
import com.threerings.tudey.geom.Rectangle;

/**
 * Represents a block in the scene.
 */
public class Block extends Actor
{
    /**
     * Creates a new block of the specified type.
     */
    public Block (int blockId)
    {
        _blockId = (short)blockId;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Block ()
    {
    }

    /**
     * Returns a reference to the configuration of this block.
     */
    public BlockConfig getConfig ()
    {
        return BlockConfig.getConfig(_blockId);
    }

    /**
     * Returns the name of the server-side logic class for blocks of this type.
     */
    public String getLogicClassName ()
    {
        return getConfig().logic;
    }

    /**
     * Sets the contents of this block.
     */
    public void setContents (Object contents)
    {
        _contents = contents;
    }

    /**
     * Returns the contents of this block, or <code>null</code> if the block is empty.
     */
    public Object getContents ()
    {
        return _contents;
    }

    /**
     * Checks whether the configuration of this block is valid.
     */
    public boolean isValid ()
    {
        return getConfig() != null;
    }

    @Override // documentation inherited
    public ActorSprite createSprite ()
    {
        return null;
    }

    @Override // documentation inherited
    public Rectangle getBounds ()
    {
        if (_bounds == null) {
            _bounds = new Rectangle();
        }
        _bounds.set(x, y, x + 1, y + 1);
        return _bounds;
    }

    @Override // documentation inherited
    public void getResources (java.util.Set<SceneResource> results)
    {
        getConfig().getResources(results);
    }

    /** The block's type identifier. */
    protected short _blockId;

    /** The contents of the block. */
    protected Object _contents;

    /** The bounds of the block. */
    protected transient Rectangle _bounds;
}
