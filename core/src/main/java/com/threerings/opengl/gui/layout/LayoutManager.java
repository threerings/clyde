//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
     * Returns a reference to the constraints associated with the specified component, or
     * <code>null</code> for none.  The default implementation always returns null.
     */
    public Object getConstraints (Component comp)
    {
        return null;
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
