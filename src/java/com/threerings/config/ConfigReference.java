//
// $Id$

package com.threerings.config;

import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * A reference to a configuration that can be embedded in, for example, other configurations.
 */
public class ConfigReference<T extends ManagedConfig> extends DeepObject
    implements Exportable
{
    /**
     * Creates a new reference to the named configuration.
     */
    public ConfigReference (String name)
    {
        _name = name;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ConfigReference ()
    {
    }

    /**
     * Returns the name of the referenced config.
     */
    public String getName ()
    {
        return _name;
    }

    /** The name of the referenced configuration. */
    protected String _name;
}
