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

package com.threerings.tudey.config;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.Strippable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.tudey.shape.config.ShapeConfig;

/**
 * Configurations for regions.
 */
@EditorTypes({
    RegionConfig.Default.class, RegionConfig.Transformed.class,
    RegionConfig.Fixed.class })
@Strippable
public abstract class RegionConfig extends DeepObject
    implements Exportable, Streamable
{
    /**
     * Base class for the located region configs.
     */
    public static abstract class Located extends RegionConfig
    {
        /** The location to use. */
        @Editable
        public TargetConfig location = new TargetConfig.Source();

        @Override
        public void invalidate ()
        {
            location.invalidate();
        }
    }

    /**
     * A potentially expanded or contracted version of the source shape.
     */
    public static class Default extends Located
    {
        /** The amount to expand the intersection shape. */
        @Editable(step=0.01)
        public float expansion;

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.RegionLogic$Default";
        }
    }

    /**
     * An explicitly specified shape that uses the source's translation and rotation.
     */
    public static class Transformed extends Located
    {
        /** The shape of the region. */
        @Editable
        public ShapeConfig shape = new ShapeConfig.Point();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.RegionLogic$Transformed";
        }

        @Override
        public void invalidate ()
        {
            super.invalidate();
            shape.invalidate();
        }
    }

    /**
     * A fixed (world space) region.
     */
    public static class Fixed extends RegionConfig
    {
        /** The shape of the region. */
        @Editable
        public ShapeConfig shape = new ShapeConfig.Point();

        @Override
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.RegionLogic$Fixed";
        }

        @Override
        public void invalidate ()
        {
            shape.invalidate();
        }
    }

    /**
     * Returns the name of the server-side logic class for this region.
     */
    public abstract String getLogicClassName ();

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }
}
