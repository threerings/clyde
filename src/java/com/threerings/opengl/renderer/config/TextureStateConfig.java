//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.TextureUnit;
import com.threerings.opengl.renderer.state.TextureState;

/**
 * Configurable texture state.
 */
public class TextureStateConfig extends DeepObject
    implements Exportable
{
    /** The texture unit configurations. */
    @Editable(nullable=false)
    public TextureUnitConfig[] units = new TextureUnitConfig[0];

    /**
     * Returns the corresponding texture state.
     */
    public TextureState getState ()
    {
        if (units.length == 0) {
            return TextureState.DISABLED;
        }
        TextureUnit[] sunits = new TextureUnit[units.length];
        for (int ii = 0; ii < units.length; ii++) {
            sunits[ii] = units[ii].createUnit();
        }
        return new TextureState(sunits);
    }
}
