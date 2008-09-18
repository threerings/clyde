//
// $Id$

package com.threerings.opengl.compositor.config;

import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;

/**
 * The configuration of a render scheme.
 */
public class RenderSchemeConfig extends ManagedConfig
{
    /** Whether or not the scheme is "special."  Normal schemes are compatible with all other
     * normal schemes and the null (default) scheme.  Special schemes are not compatible with
     * any other schemes. */
    @Editable
    public boolean special;

    /**
     * Checks this scheme for compatibility with another.
     */
    public boolean isCompatibleWith (RenderSchemeConfig other)
    {
        return !(special || (other != null && other.special));
    }
}
