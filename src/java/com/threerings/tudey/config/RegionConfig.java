//
// $Id$

package com.threerings.tudey.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.tudey.shape.config.ShapeConfig;

/**
 * Configurations for regions.
 */
@EditorTypes({
    RegionConfig.Default.class, RegionConfig.Transformed.class,
    RegionConfig.Fixed.class })
public abstract class RegionConfig extends DeepObject
    implements Exportable
{
    /**
     * Base class for the located region configs.
     */
    public static abstract class Located extends RegionConfig
    {
        /** The location to use. */
        @Editable
        public TargetConfig location = new TargetConfig.Source();
    }

    /**
     * A potentially expanded or contracted version of the source shape.
     */
    public static class Default extends Located
    {
        /** The amount to expand the intersection shape. */
        @Editable(step=0.01)
        public float expansion;

        @Override // documentation inherited
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

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.RegionLogic$Transformed";
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

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.RegionLogic$Fixed";
        }
    }

    /**
     * Returns the name of the server-side logic class for this region.
     */
    public abstract String getLogicClassName ();
}
