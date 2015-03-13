//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// https://github.com/threerings/nenya
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.media.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Provides an interface via which images can be accessed in a way that allows them to optionally
 * be located in video memory where that affords performance improvements.
 */
public interface Mirage
{
    /**
     * Renders this mirage at the specified position in the supplied graphics context.
     */
    public void paint (Graphics2D gfx, int x, int y);

    /**
     * Returns the width of this mirage.
     */
    public int getWidth ();

    /**
     * Returns the height of this mirage.
     */
    public int getHeight ();

    /**
     * Returns true if this mirage contains a non-transparent pixel at the specified coordinate.
     */
    public boolean hitTest (int x, int y);

    /**
     * Returns a snapshot of this mirage as a buffered image. The snapshot should <em>not</em> be
     * modified by the caller.
     */
    public BufferedImage getSnapshot ();

    /**
     * Returns an estimate of the memory consumed by this mirage's image raster data.
     */
    public long getEstimatedMemoryUsage ();
}
