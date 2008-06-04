//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;

/**
 * The configuration for a single tile.
 */
public class TileConfig extends ManagedConfig
{
    /** The width of the tile. */
    @Editable(min=1)
    public int width = 1;

    /** The height of the tile. */
    @Editable(min=1)
    public int height = 1;
}
