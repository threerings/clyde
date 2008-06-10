//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.IntegerIdentified;
import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;

/**
 * The configuration for a single tile.
 */
public class TileConfig extends ManagedConfig
    implements IntegerIdentified
{
    /** The width of the tile. */
    @Editable(min=1, hgroup="d")
    public int width = 1;

    /** The height of the tile. */
    @Editable(min=1, hgroup="d")
    public int height = 1;

    /** Indicates where the tile is passable. */
    @Editable(width=3)
    public boolean[][] passable = new boolean[][] { { false } };

    /** Indicates where the tile is penetrable. */
    @Editable(width=3)
    public boolean[][] penetrable = new boolean[][] { { false } };

    /** Whether or not to render drop shadows on this tile. */
    @Editable
    public boolean receivesDropShadows;
}
