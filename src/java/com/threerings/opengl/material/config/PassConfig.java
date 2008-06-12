//
// $Id$

package com.threerings.opengl.material.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.config.TextureStateConfig;

/**
 * Represents a single material pass.
 */
public class PassConfig extends DeepObject
    implements Exportable
{
    /** The texture state to use in this pass. */
    @Editable(nullable=false)
    public TextureStateConfig textureState = new TextureStateConfig();
}
