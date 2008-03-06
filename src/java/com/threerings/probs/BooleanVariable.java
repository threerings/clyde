//
// $Id$

package com.threerings.probs;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.math.FloatMath;

/**
 * A boolean random variable.
 */
public class BooleanVariable extends DeepObject
    implements Exportable
{
    /** The probability that this variable is true. */
    @Editable(min=0.0, max=1.0, step=0.01)
    public float probability;

    /**
     * Returns a sample value from this variable.
     */
    public boolean getValue ()
    {
        return FloatMath.random() < probability;
    }
}
