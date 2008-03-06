//
// $Id$

package com.threerings.opengl.gui.layout;

import com.threerings.opengl.gui.Component;
import com.threerings.opengl.gui.Container;
import com.threerings.opengl.gui.util.Dimension;

/**
 * Layout managers implement a policy for laying out the children in a
 * container. They must provide routines for computing the preferred size of a
 * target container and for actually laying out its children.
 */
public abstract class LayoutManager
{
    /**
     * Components added to a container will result in a call to this method,
     * informing the layout manager of said constraints. The default
     * implementation does nothing.
     */
    public void addLayoutComponent (Component comp, Object constraints)
    {
    }

    /**
     * Components removed to a container for which a layout manager has been
     * configured will result in a call to this method. The default
     * implementation does nothing.
     */
    public void removeLayoutComponent (Component comp)
    {
    }

    /**
     * Computes the preferred size for the supplied container, based on the
     * preferred sizes of its children and the layout policy implemented by
     * this manager. <em>Note:</em> it is not necessary to add the container's
     * insets to the returned preferred size.
     */
    public abstract Dimension computePreferredSize (
        Container target, int whint, int hhint);

    /**
     * Effects the layout policy of this manager on the supplied target,
     * adjusting the size and position of its children based on the size and
     * position of the target at the time of this call. <em>Note:</em> the
     * target's insets must be accounted for when laying out the children.
     */
    public abstract void layoutContainer (Container target);
}
