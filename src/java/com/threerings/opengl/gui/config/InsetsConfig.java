//
// $Id$

package com.threerings.opengl.gui.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.util.Insets;

/**
 * Contains a configurable set of insets.
 */
public class InsetsConfig extends DeepObject
    implements Exportable
{
    /** The inset parameters. */
    @Editable(hgroup="i")
    public int top, right, bottom, left;

    /**
     * Creates a set of insets corresponding to this config.
     */
    public Insets createInsets ()
    {
        return new Insets(left, top, right, bottom);
    }
}
