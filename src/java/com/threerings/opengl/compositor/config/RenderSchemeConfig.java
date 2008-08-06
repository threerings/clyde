//
// $Id$

package com.threerings.opengl.compositor.config;

import com.threerings.config.ManagedConfig;

/**
 * The configuration of a render scheme.
 */
public class RenderSchemeConfig extends ManagedConfig
{
    /**
     * Checks this scheme for compatibility with another.
     */
    public boolean isCompatibleWith (RenderSchemeConfig other)
    {
        return true; // for now
    }
}
