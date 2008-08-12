//
// $Id$

package com.threerings.opengl.geom.config;

import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Deformer configuration.
 */
@EditorTypes({ DeformerConfig.Skin.class })
public abstract class DeformerConfig extends DeepObject
    implements Exportable
{
    /**
     * Performs software skinning.
     */
    public static class Skin extends DeformerConfig
    {
    }
}
