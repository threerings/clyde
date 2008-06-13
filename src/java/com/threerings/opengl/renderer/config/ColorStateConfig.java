//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;

/**
 * Configurable color state.
 */
public class ColorStateConfig extends DeepObject
    implements Exportable
{
    /** The color. */
    @Editable(mode="alpha")
    public Color4f color = new Color4f(Color4f.WHITE);

    /**
     * Returns the corresponding color state.
     */
    public ColorState getState ()
    {
        return ColorState.getInstance(color);
    }
}
