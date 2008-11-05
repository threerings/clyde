//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.SimpleScope;
import com.threerings.math.FloatMath;
import com.threerings.math.Quaternion;
import com.threerings.math.Vector3f;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.util.ShapeSceneElement;
import com.threerings.tudey.config.PathConfig;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.PathEntry;
import com.threerings.tudey.data.TudeySceneModel.Vertex;

/**
 * Represents a path entry.
 */
public class PathSprite extends EntrySprite
    implements ConfigUpdateListener<PathConfig>
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
         * Updates the implementation to match the path state.
         */
        public void update (PathEntry entry)
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
        public Original (GlContext ctx, Scope parentScope, PathConfig.Original config)
        {
            super(parentScope);
            _ctx = ctx;
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (PathConfig.Original config)
        {
            // update the color state
            _colorState.getColor().set(config.color);

            // update the footprint
            boolean selected = ((PathSprite)_parentScope).isSelected();
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
        public void update (PathEntry entry)
        {
            // update the vertex models
            _vertices = maybeResize(
                _vertices, entry.vertices.length, _ctx, _scene,
                VERTEX_MODEL, _colorState, _parentScope);
            float minz = updateVertices(entry.vertices, _vertices);

            // and the edge models
            _edges = maybeResize(
                _edges, entry.vertices.length - 1, _ctx, _scene,
                EDGE_MODEL, _colorState, _parentScope);
            updateEdges(entry.vertices, _edges);

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
            if (_footprint != null) {
                _scene.remove(_footprint);
            }
        }

        /** The renderer context. */
        protected GlContext _ctx;

        /** The models representing the vertices. */
        protected Model[] _vertices = new Model[0];

        /** The models representing the edges. */
        protected Model[] _edges = new Model[0];

        /** The shared color state. */
        protected ColorState _colorState = new ColorState();

        /** The footprint. */
        protected ShapeSceneElement _footprint;

        /** The scene to which we add our models/footprint. */
        @Bound
        protected Scene _scene;
    }

    /** The name of the model to use to represent path and area vertices. */
    public static final String VERTEX_MODEL = "editor/marker/vertex/model.dat";

    /** The name of the model to use to represent path and area edges. */
    public static final String EDGE_MODEL = "editor/marker/edge/model.dat";

    /**
     * Updates the supplied array of vertex models based on the vertex state.
     *
     * @return the minimum vertex z coordinate.
     */
    public static float updateVertices (Vertex[] vertices, Model[] models)
    {
        float minz = Float.MAX_VALUE;
        for (int ii = 0; ii < models.length; ii++) {
            Vertex vertex = vertices[ii];
            Model model = models[ii];
            model.getLocalTransform().getTranslation().set(vertex.x, vertex.y, vertex.z);
            model.updateBounds();
            minz = Math.min(minz, vertex.z);
        }
        return minz;
    }

    /**
     * Updates the supplied array of edge models based on the vertex state.
     */
    public static void updateEdges (Vertex[] vertices, Model[] models)
    {
        Vector3f translation = new Vector3f(), scale = new Vector3f(), vector = new Vector3f();
        Quaternion rotation = new Quaternion();
        for (int ii = 0; ii < models.length; ii++) {
            Vertex v1 = vertices[ii], v2 = vertices[(ii + 1) % vertices.length];
            vector.set(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
            float length = vector.length();
            if (length > FloatMath.EPSILON) {
                rotation.fromAnglesXZ(
                    FloatMath.asin(vector.z / length),
                    FloatMath.atan2(-vector.x, vector.y));
            } else {
                rotation.set(Quaternion.IDENTITY);
            }
            Model model = models[ii];
            model.getLocalTransform().set(
                translation.set(v1.x, v1.y, v1.z), rotation,
                scale.set(1f, length, 1f));
            model.updateBounds();
        }
    }

    /**
     * Resizes the specified array of models if necessary, adding new models or removing
     * models as required.
     */
    public static Model[] maybeResize (
        Model[] omodels, int ncount, GlContext ctx, Scene scene,
        String name, ColorState colorState, Object userObject)
    {
        if (omodels.length == ncount) {
            return omodels;
        }
        Model[] nmodels = new Model[ncount];
        System.arraycopy(omodels, 0, nmodels, 0, Math.min(omodels.length, ncount));
        for (int ii = omodels.length; ii < ncount; ii++) {
            Model model = nmodels[ii] = new Model(ctx);
            model.setColorState(colorState);
            model.setUserObject(userObject);
            scene.add(model);
            model.setConfig(name);
        }
        for (int ii = ncount; ii < omodels.length; ii++) {
            scene.remove(omodels[ii]);
        }
        return nmodels;
    }

    /**
     * Creates a new path sprite.
     */
    public PathSprite (GlContext ctx, TudeySceneView view, PathEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<PathConfig> event)
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
        setConfig((_entry = (PathEntry)entry).path);
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
    protected void setConfig (ConfigReference<PathConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(PathConfig.class, ref));
    }

    /**
     * Sets the configuration of this path.
     */
    protected void setConfig (PathConfig config)
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
     * Updates the path to match its new or modified configuration.
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
    protected PathEntry _entry;

    /** The path configuration. */
    protected PathConfig _config;

    /** The path implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
