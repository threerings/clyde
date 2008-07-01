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
    @Editable(min=1, hgroup="p")
    public float pointSize = 1f;

    /** If true, do not use a shared instance. */
    @Editable(hgroup="p")
    public boolean uniqueInstance;

    /**
     * Returns the corresponding point state.
     */
    public PointState getState ()
    {
        return uniqueInstance ? new PointState(pointSize) : PointState.getInstance(pointSize);
    }
}
