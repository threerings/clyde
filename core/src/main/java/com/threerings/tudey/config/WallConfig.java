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

import com.samskivert.util.IntTuple;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.tudey.data.TudeySceneModel.TileEntry;

/**
 * The configuration of a wall type.
 */
public class WallConfig extends PaintableConfig
{
    /**
     * Contains the actual implementation of the wall type.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        @Deprecated
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Returns a reference to the config's underlying original implementation.
         */
        public abstract Original getOriginal (ConfigManager cfgmgr);

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
    }

    /**
     * An original wall implementation.
     */
    public static class Original extends Implementation
    {
        /** The ground underlying the wall. */
        @Editable(nullable=true)
        public ConfigReference<GroundConfig> ground;

        /** The wall cases. */
        @Editable
        public Case[] cases = new Case[0];

        /**
         * Checks whether the specified entry matches any wall case.
         */
        public boolean isWall (TileEntry entry, int elevation)
        {
            for (Case caze : cases) {
                if (matchesAny(caze.tiles, entry, elevation)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Determines the case and allowed rotations of the wall tile that matches the specified
         * pattern.
         */
        public IntTuple getWallCaseRotations (int pattern)
        {
            return getCaseRotations(cases, pattern);
        }

        /**
         * Checks whether the specified entry qualifies as a wall tile.
         */
        public boolean isWall (TileEntry entry, IntTuple caseRotations, int elevation)
        {
            return matchesAny(cases, entry, caseRotations, elevation);
        }

        /**
         * Creates a new wall tile with the supplied case/rotations and maximum dimensions.
         */
        public TileEntry createWall (
            ConfigManager cfgmgr, IntTuple caseRotations,
            int maxWidth, int maxHeight, int elevation)
        {
            return createRandomEntry(
                cfgmgr, cases[caseRotations.left].tiles,
                caseRotations.right, maxWidth, maxHeight, elevation);
        }

        @Override
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        @Override
        public void invalidate ()
        {
            for (Case caze : cases) {
                caze.invalidate();
            }
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The wall reference. */
        @Editable(nullable=true)
        public ConfigReference<WallConfig> wall;

        @Override
        public Original getOriginal (ConfigManager cfgmgr)
        {
            WallConfig config = cfgmgr.getConfig(WallConfig.class, wall);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }
    }

    /** The actual wall implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Returns a reference to the config's underlying original implementation.
     */
    public Original getOriginal (ConfigManager cfgmgr)
    {
        return implementation.getOriginal(cfgmgr);
    }

    @Override
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }
}
