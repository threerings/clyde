//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.state.PointState;

/**
 * Configurable line state.
 */
public class PointStateConfig extends DeepObject
    implements Exportable
{
    /** The point size. */
    @Editable(min=1)
    public float pointSize = 1f;

    /**
     * Returns the corresponding point state.
     */
    public PointState getState ()
    {
        return PointState.getInstance(pointSize);
    }
}
