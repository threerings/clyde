//
// $Id$

package com.threerings.openal.util;

import com.threerings.config.ConfigManager;
import com.threerings.expr.DynamicScope;

import com.threerings.openal.ClipProvider;
import com.threerings.openal.SoundManager;

/**
 * Provides access to the OpenAL bits.
 */
public interface AlContext
{
    /**
     * Returns a reference to the scope.
     */
    public DynamicScope getScope ();

    /**
     * Returns a reference to the configuration manager.
     */
    public ConfigManager getConfigManager ();

    /**
     * Returns a reference to the sound manager.
     */
    public SoundManager getSoundManager ();

    /**
     * Returns a reference to the clip provider.
     */
    public ClipProvider getClipProvider ();
}
