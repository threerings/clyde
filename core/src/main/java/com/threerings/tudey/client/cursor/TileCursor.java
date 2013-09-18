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

package com.threerings.tudey.client.cursor;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;

import com.threerings.opengl.compositor.Compositable;
import com.threerings.opengl.compositor.RenderScheme;
import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.util.Tickable;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.util.RectangleElement;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;
import com.threerings.tudey.shape.Polygon;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.TudeyContext;

/**
 * A cursor for tiles.
 */
public class TileCursor extends EntryCursor
    implements ConfigUpdateListener<TileConfig>
{
    /**
     * The actual cursor implementation.
     */
    public static abstract class Implementation extends SimpleScope
        implements Tickable, Compositable
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Returns a reference to the transformed shape.
         */
        public Shape getShape ()
        {
            return null;
        }

        /**
         * Updates the cursor state.
         */
        public void update (TileEntry entry)
        {
            // nothing by default
        }

        // documentation inherited from interface Tickable
        public void tick (float elapsed)
        {
            // nothing by default
        }

        // documentation inherited from interface Compositable
        public void composite ()
        {
            // nothing by default
        }

        @Override
        public String getScopeName ()
        {
            return "impl";
        }
    }

    /**
     * The original implementation.
     */
    public static class Original extends Implementation
    {
        /**
         * Creates a new implementation.
         */
        public Original (TudeyContext ctx, Scope parentScope, TileConfig.Original config)
        {
            super(parentScope);
            _model = new Model(ctx);
            _model.setParentScope(this);
            _model.setRenderScheme(RenderScheme.TRANSLUCENT);
            _model.setColorState(new ColorState());
            _model.getColorState().getColor().set(0.5f, 0.5f, 0.5f, 0.45f);

            _footprint = new RectangleElement(ctx, true);
            _footprint.getColor().set(FOOTPRINT_COLOR);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (TileConfig.Original config)
        {
            _model.setConfig((_config = config).model);
        }

        @Override
        public Shape getShape ()
        {
            return _shape;
        }

        @Override
        public void update (TileEntry entry)
        {
            entry.getTransform(_config, _model.getLocalTransform());
            _model.updateBounds();

            Rectangle region = _footprint.getRegion();
            entry.getRegion(_config, region);
            _footprint.setElevation(entry.elevation);

            // update the shape
            _shape.getVertex(0).set(region.x, region.y);
            _shape.getVertex(1).set(region.x + region.width, region.y);
            _shape.getVertex(2).set(region.x + region.width, region.y + region.height);
            _shape.getVertex(3).set(region.x, region.y + region.height);
            _shape.updateBounds();
        }

        @Override
        public void tick (float elapsed)
        {
            _model.tick(elapsed);
        }

        @Override
        public void composite ()
        {
            _model.composite();
            _footprint.composite();
        }

        /** The tile configuration. */
        protected TileConfig.Original _config;

        /** The model. */
        protected Model _model;

        /** The tile footprint. */
        protected RectangleElement _footprint;

        /** The shape of the tile. */
        protected Polygon _shape = new Polygon(4);
    }

    /**
     * Creates a new tile cursor.
     */
    public TileCursor (TudeyContext ctx, TudeySceneView view, TileEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<TileConfig> event)
    {
        updateFromConfig();
        _impl.update(_entry);
    }

    @Override
    public Entry getEntry ()
    {
        return _entry;
    }

    @Override
    public Shape getShape ()
    {
        return _impl.getShape();
    }

    @Override
    public void update (Entry entry)
    {
        setConfig((_entry = (TileEntry)entry).tile);
        _impl.update(_entry);
    }

    @Override
    public void tick (float elapsed)
    {
        _impl.tick(elapsed);
    }

    @Override
    public void composite ()
    {
        _impl.composite();
    }

    @Override
    public void dispose ()
    {
        super.dispose();
        _impl.dispose();
        if (_config != null) {
            _config.removeListener(this);
        }
    }

    /**
     * Sets the configuration of the tile.
     */
    protected void setConfig (ConfigReference<TileConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(TileConfig.class, ref));
    }

    /**
     * Sets the configuration of the tile.
     */
    protected void setConfig (TileConfig config)
    {
        if (_config == config) {
            return;
        }
        if (_config != null) {
            _config.removeListener(this);
        }
        if ((_config = config) != null) {
            _config.addListener(this);
        }
        updateFromConfig();
    }

    /**
     * Updates this cursor to match its configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getCursorImplementation(_ctx, this, _impl);
        nimpl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
        if (_impl != nimpl) {
            _impl.dispose();
            _impl = nimpl;
        }
    }

    /** The prototype entry. */
    protected TileEntry _entry;

    /** The tile config. */
    protected TileConfig _config;

    /** The cursor implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
