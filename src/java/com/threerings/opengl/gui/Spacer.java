//
// $Id$

package com.threerings.opengl.gui;

import com.threerings.opengl.gui.util.Dimension;

/**
 * Takes up space!
 */
public class Spacer extends Component
{
    /**
     * Creates a 1x1 spacer that will presumably be later resized by a layout
     * manager in some appropriate manner.
     */
    public Spacer ()
    {
        this(1, 1);
    }

    /**
     * Creates a spacer with the specified preferred dimensions.
     */
    public Spacer (int prefWidth, int prefHeight)
    {
        setPreferredSize(new Dimension(prefWidth, prefHeight));
    }
}
