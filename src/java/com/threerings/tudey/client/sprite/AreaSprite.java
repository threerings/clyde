//
// $Id$

package com.threerings.tudey.client.sprite;

import com.samskivert.util.ListUtil;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.SimpleScope;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.scene.Scene;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.util.AreaElement;
import com.threerings.tudey.client.util.ShapeSceneElement;
import com.threerings.tudey.config.AreaConfig;
import com.threerings.tudey.data.TudeySceneModel.AreaEntry;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents an area entry.
 */
public class AreaSprite extends EntrySprite
    implements ConfigUpdateListener<AreaConfig>
{
    /**
     * The actual sprite implementation.
     */
    public static abstract class Implementation extends SimpleScope
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Returns the index of the specified model within the list of vertex models, or -1 if it
         * is not a vertex.
         */
        public int getVertexIndex (Model model)
        {
            return -1;
        }

        /**
         * Returns the index of the specified model within the list of edge models, or -1 if it is
         * not an edge.
         */
        public int getEdgeIndex (Model model)
        {
            return -1;
        }

        /**
         * Updates the implementation to match the area state.
         */
        public void update (AreaEntry entry)
        {
            // nothing by default
        }

        @Override // documentation inherited
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
            _scene.add(_area = new AreaElement(ctx));
            _area.setUserObject(_parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (AreaConfig.Original config)
        {
            // update the color state
            _colorState.getColor().set(config.color);
            _area.getColor().set(config.color).multLocal(0.5f);

            // update the footprint
            boolean selected = ((AreaSprite)_parentScope).isSelected();
            if (selected && _footprint == null) {
                _footprint = new ShapeSceneElement(_ctx, true);
                _footprint.getColor().set(SELECTED_COLOR);
                _scene.add(_footprint);
            } else if (!selected && _footprint != null) {
                _scene.remove(_footprint);
                _footprint = null;
            }
        }

        @Override // documentation inherited
        public int getVertexIndex (Model model)
        {
            return ListUtil.indexOfRef(_vertices, model);
        }

        @Override // documentation inherited
        public int getEdgeIndex (Model model)
        {
            return ListUtil.indexOfRef(_edges, model);
        }

        @Override // documentation inherited
        public void update (AreaEntry entry)
        {
            // update the vertex models
            _vertices = PathSprite.maybeResize(
                _vertices, entry.vertices.length, _ctx, _scene,
                PathSprite.VERTEX_MODEL, _colorState, _parentScope);
            float minz = PathSprite.updateVertices(entry.vertices, _vertices);

            // and the edge models
            _edges = PathSprite.maybeResize(
                _edges, entry.vertices.length, _ctx, _scene,
                PathSprite.EDGE_MODEL, _colorState, _parentScope);
            PathSprite.updateEdges(entry.vertices, _edges);

            // and the area
            _area.setVertices(entry.vertices);

            // update the footprint's elevation and shape
            if (_footprint != null) {
                _footprint.getTransform().getTranslation().z = minz;
                _footprint.setShape(entry.createShape()); // this updates the bounds
            }
        }

        @Override // documentation inherited
        public void dispose ()
        {
            super.dispose();
            _scene.removeAll(_vertices);
            _scene.removeAll(_edges);
            _scene.remove(_area);
            if (_footprint != null) {
                _scene.remove(_footprint);
            }
        }

        /** The renderer context. */
        protected TudeyContext _ctx;

        /** The models representing the vertices. */
        protected Model[] _vertices = new Model[0];

        /** The models representing the edges. */
        protected Model[] _edges = new Model[0];

        /** Displays the actual area. */
        protected AreaElement _area;

        /** The shared color state. */
        protected ColorState _colorState = new ColorState();

        /** The footprint. */
        protected ShapeSceneElement _footprint;

        /** The scene to which we add our models/footprint. */
        @Bound
        protected Scene _scene;
    }

    /**
     * Creates a new area sprite.
     */
    public AreaSprite (TudeyContext ctx, TudeySceneView view, AreaEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    /**
     * Returns the index of the specified model within the list of vertex models, or -1 if it
     * is not a vertex.
     */
    public int getVertexIndex (Model model)
    {
        return _impl.getVertexIndex(model);
    }

    /**
     * Returns the index of the specified model within the list of edge models, or -1 if it is
     * not an edge.
     */
    public int getEdgeIndex (Model model)
    {
        return _impl.getEdgeIndex(model);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<AreaConfig> event)
    {
        updateFromConfig();
        _impl.update(_entry);
    }

    @Override // documentation inherited
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        updateFromConfig();
        _impl.update(_entry);
    }

    @Override // documentation inherited
    public Entry getEntry ()
    {
        return _entry;
    }

    @Override // documentation inherited
    public void update (Entry entry)
    {
        setConfig((_entry = (AreaEntry)entry).area);
        _impl.update(_entry);
    }

    @Override // documentation inherited
    public void setSelected (boolean selected)
    {
        super.setSelected(selected);
        updateFromConfig();
        _impl.update(_entry);
    }

    @Override // documentation inherited
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
            null : _config.getSpriteImplementation(_ctx, this, _impl);
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
