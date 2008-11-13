//
// $Id$

package com.threerings.tudey.util;

import com.threerings.whirled.util.WhirledContext;

import com.threerings.opengl.gui.Root;
import com.threerings.opengl.util.GlContext;

/**
 * Provides access to the required elements of the Tudey system.
 */
public interface TudeyContext extends GlContext, WhirledContext
{
    /**
     * Returns a reference to the UI root.
     */
    public Root getRoot ();
}
