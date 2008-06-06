//
// $Id$

package com.threerings.editor.util;

import com.threerings.resource.ResourceManager;
import com.threerings.util.MessageBundle;

import com.threerings.config.ConfigManager;

/**
 * Provides access to the services required by the editor.
 */
public interface EditorContext
{
    /**
     * Returns a reference to the resource manager.
     */
    public ResourceManager getResourceManager ();

    /**
     * Returns a reference to the configuration manager.
     */
    public ConfigManager getConfigManager ();

    /**
     * Returns a reference to the default message bundle.
     */
    public MessageBundle getMessageBundle ();
}
