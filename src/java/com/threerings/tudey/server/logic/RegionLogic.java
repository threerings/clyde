//
// $Id$

package com.threerings.tudey.server.logic;

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.Lists;

import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.RegionConfig;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;

/**
 * Handles the resolution of regions.
 */
public abstract class RegionLogic extends Logic
{
    /**
     * Base class for located regions.
     */
    public static abstract class Located extends RegionLogic
    {
        @Override // documentation inherited
        public void resolve (Logic activator, Collection<Shape> results)
        {
            _location.resolve(activator, _locations);
            getShapes(results);
            _locations.clear();
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _location = createTarget(((RegionConfig.Located)_config).location, _source);
        }

        /**
         * Updates the shapes based on the targets.
         */
        protected abstract void getShapes (Collection<Shape> results);

        /** The region location. */
        protected TargetLogic _location;

        /** Holds the locations during processing. */
        protected ArrayList<Logic> _locations = Lists.newArrayList();
    }

    /**
     * Handles a normal region.
     */
    public static class Default extends Located
    {
        @Override // documentation inherited
        protected void getShapes (Collection<Shape> results)
        {
            float expansion = ((RegionConfig.Default)_config).expansion;
            for (int ii = 0, nn = _locations.size(); ii < nn; ii++) {
                Shape shape = _locations.get(ii).getShape();
                if (shape != null) {
                    results.add(expansion == 0f ? shape : shape.expand(expansion));
                }
            }
        }
    }

    /**
     * Handles an explicit, transformed region.
     */
    public static class Transformed extends Located
    {
        @Override // documentation inherited
        protected void getShapes (Collection<Shape> results)
        {
            Shape shape = ((RegionConfig.Transformed)_config).shape.getShape();
            for (int ii = 0, nn = _locations.size(); ii < nn; ii++) {
                Logic location = _locations.get(ii);
                results.add(shape.transform(location.getTransform(_transform)));
            }
        }

        /** The transform to reuse. */
        protected Transform2D _transform = new Transform2D();
    }

    /**
     * Handles a fixed (world space) region.
     */
    public static class Fixed extends RegionLogic
    {
        @Override // documentation inherited
        public void resolve (Logic activator, Collection<Shape> results)
        {
            results.add(((RegionConfig.Fixed)_config).shape.getShape());
        }
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, RegionConfig config, Logic source)
    {
        super.init(scenemgr);
        _config = config;
        _source = source;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Resolves the region into a collection of shapes.
     */
    public abstract void resolve (Logic activator, Collection<Shape> results);

    @Override // documentation inherited
    public Vector2f getTranslation ()
    {
        return _source.getTranslation();
    }

    @Override // documentation inherited
    public float getRotation ()
    {
        return _source.getRotation();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /** The region configuration. */
    protected RegionConfig _config;

    /** The action source. */
    protected Logic _source;
}
