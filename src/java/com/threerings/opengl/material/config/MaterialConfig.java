//
// $Id$

package com.threerings.opengl.material.config;

import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Describes a material.
 */
public class MaterialConfig extends ParameterizedConfig
{
    /**
     * A technique available to render the material.
     */
    public static class Technique extends DeepObject
        implements Exportable
    {
        /** The passes used to render the material. */
        @Editable(nullable=false)
        public Pass[] passes = new Pass[0];
    }

    /** The actual implementation of this material. */
    @Editable(nullable=false)
    public Technique[] techniques = new Technique[0];
}
