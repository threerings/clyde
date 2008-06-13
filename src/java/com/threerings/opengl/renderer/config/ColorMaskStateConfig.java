//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.state.ColorMaskState;

/**
 * Configurable color mask state.
 */
public class ColorMaskStateConfig extends DeepObject
    implements Exportable
{
    /** Whether to write to the red channel. */
    @Editable(hgroup="m")
    public boolean red = true;

    /** Whether to write to the green channel. */
    @Editable(hgroup="m")
    public boolean green = true;

    /** Whether to write to the blue channel. */
    @Editable(hgroup="m")
    public boolean blue = true;

    /** Whether to write to the alpha channel. */
    @Editable(hgroup="m")
    public boolean alpha = true;

    /**
     * Returns the corresponding color mask state.
     */
    public ColorMaskState getState ()
    {
        return ColorMaskState.getInstance(red, green, blue, alpha);
    }
}
