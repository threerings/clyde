//
// $Id$

package com.threerings.opengl.config;

import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;

/**
 * Describes a material.
 */
public class MaterialConfig extends ManagedConfig
{
    /** The techniques available for rendering this material. */
    @Editable
    public TechniqueConfig[] techniques = new TechniqueConfig[0];
}
