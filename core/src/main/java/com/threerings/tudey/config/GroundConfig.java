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
import com.threerings.util.DeepOmit;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Paint;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;
import com.threerings.tudey.util.Direction;

/**
 * The configuration of a ground type.
 */
public class GroundConfig extends PaintableConfig
{
    /**
     * Contains the actual implementation of the ground type.
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
     * An original ground implementation.
     */
    public static class Original extends Implementation
    {
        /** The priority of this ground type. */
        @Editable(hgroup="p")
        public int priority;

        /** Whether or not to extend the edge. */
        @Editable(hgroup="p")
        public boolean extendEdge;

        /** The base ground type, if any. */
        @Editable(nullable=true)
        public ConfigReference<GroundConfig> base;

        /** The floor tiles. */
        @Editable
        public Tile[] floor = new Tile[0];

        /** The edge cases (in order of priority). */
        @Editable
        public EdgeCase[] edgeCases = new EdgeCase[0];

        /**
         * Checks whether the specified location qualifies as a floor tile.
         *
         * @param scene the scene model to check.
         * @param ref the config reference of this ground config.
         * @param elevation the elevation of the brush.
         */
        public boolean isFloor (
            TudeySceneModel scene, ConfigReference<GroundConfig> ref, int x, int y, int elevation)
        {
            Paint paint = scene.getPaint(x, y);
            if (paint != null && paint.type != Paint.Type.WALL && paint.elevation == elevation) {
                if (paint.type == Paint.Type.FLOOR && paint.paintable.equals(ref)) {
                    return true;
                }
                GroundConfig config = paint.getConfig(
                    scene.getConfigManager(), GroundConfig.class);
                Original original = (config == null) ?
                    null : config.getOriginal(scene.getConfigManager());
                if (original != null && ref.equals(original.base)) {
                    return true;
                }
            }
            return matchesAny(floor, scene.getTileEntry(x, y), elevation);
        }

        /**
         * Creates a new floor tile with the supplied maximum dimensions.
         */
        public TileEntry createFloor (
            ConfigManager cfgmgr, int maxWidth, int maxHeight, int elevation)
        {
            return createRandomEntry(cfgmgr, floor, maxWidth, maxHeight, elevation);
        }

        /**
         * Checks whether the specified entry matches any edge case.
         */
        public boolean isEdge (TileEntry entry, int elevation)
        {
            for (Case caze : edgeCases) {
                if (matchesAny(caze.tiles, entry, elevation)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks whether the specified entry qualifies as an edge tile.
         */
        public boolean isEdge (TileEntry entry, IntTuple caseRotations, int elevation)
        {
            return matchesAny(edgeCases, entry, caseRotations, elevation);
        }

        /**
         * Determines the case and allowed rotations of the edge tile that matches the specified
         * pattern.
         */
        public IntTuple getEdgeCaseRotations (
            TudeySceneModel scene, ConfigReference<GroundConfig> ref, int x, int y, int elevation)
        {
            for (int ii = 0; ii < edgeCases.length; ii++) {
                int rotations = edgeCases[ii].getRotations(scene, ref, this, x, y, elevation);
                if (rotations != 0) {
                    return new IntTuple(ii, rotations);
                }
            }
            return null;
        }

        /**
         * Creates a new edge tile with the supplied case/rotations and maximum dimensions.
         */
        public TileEntry createEdge (
            ConfigManager cfgmgr, IntTuple caseRotations,
            int maxWidth, int maxHeight, int elevation)
        {
            return createRandomEntry(
                cfgmgr, edgeCases[caseRotations.left].tiles,
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
            for (Tile tile : floor) {
                tile.invalidate();
            }
            for (Case caze : edgeCases) {
                caze.invalidate();
            }
        }
    }

    /**
     * Ground edge case class.
     */
    @EditorTypes({ EdgeCase.class, TransitionCase.class })
    public static class EdgeCase extends Case
    {
        /**
         * Returns a bit set containing the rotations of this case that match the specified
         * pattern.
         */
        public int getRotations (
            TudeySceneModel scene, ConfigReference<GroundConfig> ref,
            GroundConfig.Original original, int x, int y, int elevation)
        {
            return getRotations(createPattern(scene, ref, original, x, y, elevation));
        }
    }

    /**
     * Transition edge case.
     */
    public static class TransitionCase extends EdgeCase
    {
        /** The "other" ground to which we are transitioning. */
        @Editable(nullable=true)
        public ConfigReference<GroundConfig> other;

        /** The constraints for the other ground. */
        @Editable(hgroup="d")
        public boolean on, onw, ow, osw, os, ose, oe, one;

        @Override
        public int getRotations (
            TudeySceneModel scene, ConfigReference<GroundConfig> ref,
            GroundConfig.Original original, int x, int y, int elevation)
        {
            int base = super.getRotations(scene, ref, original, x, y, elevation);
            GroundConfig oconfig = scene.getConfigManager().getConfig(GroundConfig.class, other);
            GroundConfig.Original ooriginal = (oconfig == null) ?
                null : oconfig.getOriginal(scene.getConfigManager());
            if (ooriginal == null) {
                return base;
            }
            return base & getRotations(getOtherPatterns(),
                createPattern(scene, other, ooriginal, x, y, elevation));
        }

        @Override
        public void invalidate ()
        {
            super.invalidate();
            _opatterns = null;
        }

        /**
         * Gets the cached other pattern rotations.
         */
        protected int[] getOtherPatterns ()
        {
            if (_opatterns == null) {
                _opatterns = new int[] {
                    createPattern(on, onw, ow, osw, os, ose, oe, one),
                    createPattern(oe, one, on, onw, ow, osw, os, ose),
                    createPattern(os, ose, oe, one, on, onw, ow, osw),
                    createPattern(ow, osw, os, ose, oe, one, on, onw)
                };
            }
            return _opatterns;
        }

        /** Constraint patterns for the other ground for each rotation. */
        @DeepOmit
        protected transient int[] _opatterns;
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The ground reference. */
        @Editable(nullable=true)
        public ConfigReference<GroundConfig> ground;

        @Override
        public Original getOriginal (ConfigManager cfgmgr)
        {
            GroundConfig config = cfgmgr.getConfig(GroundConfig.class, ground);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }
    }

    /** The actual ground implementation. */
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

    /**
     * Creates a bit pattern identifying the bordering locations matching the specified
     * ground config.
     */
    protected static int createPattern (
        TudeySceneModel scene, ConfigReference<GroundConfig> ref,
        GroundConfig.Original original, int x, int y, int elevation)
    {
        int pattern = 0;
        for (Direction dir : Direction.values()) {
            int dx = x + dir.getX(), dy = y + dir.getY();
            if (original.isFloor(scene, ref, dx, dy, elevation)) {
                pattern |= (1 << dir.ordinal());
            }
        }
        return pattern;
    }
}
