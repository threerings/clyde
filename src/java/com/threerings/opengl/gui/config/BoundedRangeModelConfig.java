//
// $Id$

package com.threerings.opengl.gui.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.BoundedRangeModel;
import com.threerings.opengl.gui.BoundedSnappingRangeModel;

/**
 * Contains the configuration of a range model.
 */
@EditorTypes({ BoundedRangeModelConfig.class, BoundedRangeModelConfig.Snapping.class })
public class BoundedRangeModelConfig extends DeepObject
    implements Exportable
{
    /**
     * A solid border.
     */
    public static class Snapping extends BoundedRangeModelConfig
    {
        /** The snap value. */
        @Editable
        public int snap;

        @Override // documentation inherited
        public BoundedRangeModel createBoundedRangeModel ()
        {
            return new BoundedSnappingRangeModel(min, value, extent, max, snap);
        }
    }

    /** The minimum value. */
    @Editable(hgroup="v")
    public int min;

    /** The initial value. */
    @Editable(hgroup="v")
    public int value;

    /** The covered extent. */
    @Editable(hgroup="v")
    public int extent = 10;

    /** The maximum value. */
    @Editable(hgroup="v")
    public int max = 100;

    /**
     * Creates a bounded range model from this config.
     */
    public BoundedRangeModel createBoundedRangeModel ()
    {
        return new BoundedRangeModel(min, value, extent, max);
    }
}
