//
// $Id$

package com.threerings.tudey.util;

import com.threerings.crowd.util.CrowdContext;

import com.threerings.opengl.util.GlContext;
import com.threerings.openal.util.AlContext;

/**
 * Provides access to the services required by the Tudey system.
 */
public interface TudeyContext extends GlContext, AlContext, CrowdContext
{
}
