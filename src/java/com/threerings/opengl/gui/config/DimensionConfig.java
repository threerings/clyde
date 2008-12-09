//
// $Id$

package com.threerings.opengl.gui.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.util.Dimension;

/**
 * Contains a configurable set of dimensions.
 */
public class DimensionConfig extends DeepObject
    implements Exportable
{
    /** The dimensions. */
    @Editable(hgroup="d")
    public int width, height;

    /**
     * Creates a dimension object corresponding to this config.
     */
    public Dimension createDimension ()
    {
        return new Dimension(width, height);
    }
}
