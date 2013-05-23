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

package com.threerings.tudey.server.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Lists;

import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.RegionConfig;
import com.threerings.tudey.data.EntityKey;
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
        @Override
        public void resolve (Logic activator, Collection<Shape> results)
        {
            _location.resolve(activator, _locations);
            getShapes(results);
            _locations.clear();
        }

        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _location.transfer(((Located)source)._location, refs);
        }

        @Override
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
        @Override
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
        @Override
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
        @Override
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

    @Override
    public boolean isActive ()
    {
        return _source.isActive();
    }

    @Override
    public EntityKey getEntityKey ()
    {
        return _source.getEntityKey();
    }

    @Override
    public Vector2f getTranslation ()
    {
        return _source.getTranslation();
    }

    @Override
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
