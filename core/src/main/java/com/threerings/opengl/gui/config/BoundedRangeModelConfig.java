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

        @Override
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
