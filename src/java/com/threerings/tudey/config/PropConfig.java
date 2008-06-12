//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;

/**
 * The configuration for a single type of prop.
 */
public class PropConfig extends ManagedConfig
{
    /** Whether or not the prop is passable. */
    @Editable
    public boolean passable;

    /** Whether or not the prop is penetrable. */
    @Editable
    public boolean penetrable;
}
