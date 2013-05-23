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
import com.threerings.opengl.model.Model;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.util.Tickable;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.sprite.PathSprite;
import com.threerings.tudey.client.util.AreaElement;
import com.threerings.tudey.client.util.ShapeSceneElement;
import com.threerings.tudey.config.AreaConfig;
import com.threerings.tudey.data.TudeySceneModel.AreaEntry;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents an area entry.
 */
public class AreaCursor extends EntryCursor
    implements ConfigUpdateListener<AreaConfig>
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
         * Updates the implementation to match the area state.
         */
        public void update (AreaEntry entry)
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
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /**
         * Creates a new implementation.
         */
        public Original (TudeyContext ctx, Scope parentScope, AreaConfig.Original config)
        {
            super(parentScope);
            _ctx = ctx;
            _area = new AreaElement(ctx);
            _footprint = new ShapeSceneElement(ctx, true);
            _footprint.getColor().set(FOOTPRINT_COLOR);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (AreaConfig.Original config)
        {
            // update the color state
            _colorState.getColor().set(config.color).multLocal(0.5f);
            _area.getColor().set(config.color).multLocal(0.25f);
        }

        @Override
        public Shape getShape ()
        {
            return _footprint.getShape();
        }

        @Override
        public void update (AreaEntry entry)
        {
            // update the vertex models
            _vertices = PathCursor.maybeResize(
                _vertices, entry.vertices.length, _ctx, this,
                PathSprite.VERTEX_MODEL, _colorState);
            float minz = PathSprite.updateVertices(entry.vertices, _vertices);

            // and the edge models
            _edges = PathCursor.maybeResize(
                _edges, entry.vertices.length - 1, _ctx, this,
                PathSprite.EDGE_MODEL, _colorState);
            PathSprite.updateEdges(entry.vertices, _edges);

            // and the area
            _area.setVertices(entry.vertices);

            // update the footprint's elevation and shape (which also updates the bounds)
            _footprint.getTransform().getTranslation().z = minz;
            _footprint.setShape(entry.createShape(_ctx.getConfigManager()));
        }

        @Override
        public void tick (float elapsed)
        {
            for (Model model : _vertices) {
                model.tick(elapsed);
            }
            for (Model model : _edges) {
                model.tick(elapsed);
            }
        }

        @Override
        public void composite ()
        {
            for (Model model : _vertices) {
                model.composite();
            }
            for (Model model : _edges) {
                model.composite();
            }
            _footprint.composite();
            _area.composite();
        }

        /** The renderer context. */
        protected TudeyContext _ctx;

        /** The models representing the vertices. */
        protected Model[] _vertices = new Model[0];

        /** The models representing the edges. */
        protected Model[] _edges = new Model[0];

        /** Displays the actual area. */
        protected AreaElement _area;

        /** The footprint. */
        protected ShapeSceneElement _footprint;

        /** The shared color state. */
        protected ColorState _colorState = new ColorState();
    }

    /**
     * Creates a new area cursor.
     */
    public AreaCursor (TudeyContext ctx, TudeySceneView view, AreaEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<AreaConfig> event)
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
        setConfig((_entry = (AreaEntry)entry).area);
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
     * Sets the configuration of this path.
     */
    protected void setConfig (ConfigReference<AreaConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(AreaConfig.class, ref));
    }

    /**
     * Sets the configuration of this area.
     */
    protected void setConfig (AreaConfig config)
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
     * Updates the area to match its new or modified configuration.
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

    /** The scene entry. */
    protected AreaEntry _entry;

    /** The area configuration. */
    protected AreaConfig _config;

    /** The area implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
