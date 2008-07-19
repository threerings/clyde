//
// $Id$

package com.threerings.opengl.mat;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.ScopeUpdateListener;

import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.util.Renderable;

/**
 * A renderable surface.
 */
public class Surface
    implements Renderable, ConfigUpdateListener<MaterialConfig>, ScopeUpdateListener
{
    /**
     * Sets the material configuration of this surface.
     */
    public void setConfig (MaterialConfig config)
    {
        if (_config != null) {
            _config.removeListener(this);
        }
        if ((_config = config) != null) {
            _config.addListener(this);
        }
        updateFromConfig();
    }

    /**
     * Returns a reference to this surface's material configuration.
     */
    public MaterialConfig getConfig ()
    {
        return _config;
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<MaterialConfig> event)
    {
        updateFromConfig();
    }

    // documentation inherited from interface ScopeUpdateListener
    public void scopeUpdated (ScopeEvent event)
    {
    }

    /**
     * Updates the surface to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
    }

    /** The configuration of the surface material. */
    protected MaterialConfig _config;
}
