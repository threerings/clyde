//
// $Id$

package com.threerings.opengl.renderer.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.state.LineState;

/**
 * Configurable line state.
 */
public class LineStateConfig extends DeepObject
    implements Exportable
{
    /** The line width. */
    @Editable(min=1, hgroup="l")
    public float lineWidth = 1f;

    /** If true, do not use a shared instance. */
    @Editable(hgroup="l")
    public boolean uniqueInstance;

    /**
     * Returns the corresponding line state.
     */
    public LineState getState ()
    {
        return uniqueInstance ? new LineState(lineWidth) : LineState.getInstance(lineWidth);
    }
}
