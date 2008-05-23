//
// $Id$

package com.threerings.opengl.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;

/**
 * A technique for rendering a material.
 */
public class TechniqueConfig
    implements Exportable
{
    /** The passes required to render using this technique. */
    @Editable
    public PassConfig[] passes = new PassConfig[0];
}
