//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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
        super.setValue(value - (value % _snap));
    }

    // documentation inherited
    public int getScrollIncrement ()
    {
        return _snap;
    }

    protected int _snap;
}
