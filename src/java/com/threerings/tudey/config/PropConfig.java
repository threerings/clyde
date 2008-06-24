//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;

import com.threerings.opengl.model.config.ModelConfig;

/**
 * The configuration for a single type of prop.
 */
public class PropConfig extends ManagedConfig
{
    /** The model to use for the prop. */
    @Editable(nullable=true)
    public ConfigReference<ModelConfig> model;

    /** The shape of the prop. */
    @Editable
    public ShapeConfig shape = new ShapeConfig.Circle();

    /** Whether or not the prop is passable. */
    @Editable(hgroup="p")
    public boolean passable;

    /** Whether or not the prop is penetrable. */
    @Editable(hgroup="p")
    public boolean penetrable;
}
