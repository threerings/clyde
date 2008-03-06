//
// $Id$

package com.threerings.opengl.gui;

/**
 * Provides a Bounded range model where values snap to a period.
 */
public class BoundedSnappingRangeModel extends BoundedRangeModel
{
    /**
     * Creates a bounded range model with the specified minimum,
     * current, extent and maximum values, and a snap period.
     */
    public BoundedSnappingRangeModel (
            int min, int value, int extent, int max, int snap)
    {
        super(min, value, extent, max);
        _snap = snap;
    }

    /**
     * Configures the value of this model.  The new value will be
     * adjusted if it does not fall within the range of <code>min
     * <= value <= max - extent<code> or if value is not a modulus
     * of <code>snap</code>.
     */
    public void setValue (int value)
    {
        int val = Math.min(_max - _extent, Math.max(_min, value));
        val = val - (val % _snap);
        setRange(_min, val, _extent, _max);
    }

    // documentation inherited
    public int getScrollIncrement ()
    {
        return _snap;
    }

    protected int _snap;
}
