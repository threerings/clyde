//
// $Id$

package com.threerings.opengl.model.config;

import java.io.File;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import com.samskivert.util.QuickSort;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;
import com.threerings.util.Shallow;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.mod.StaticImpl;
import com.threerings.opengl.model.CollisionMesh;
import com.threerings.opengl.model.tools.ModelDef;
import com.threerings.opengl.model.tools.xml.ModelParser;

import com.threerings.opengl.effect.config.ParticleSystemConfig;
import com.threerings.opengl.geom.config.GeometryConfig;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.util.GlContext;

import static com.threerings.opengl.Log.*;

/**
 * The configuration of a model.
 */
public class ModelConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the model.
     */
    @EditorTypes({
        Static.class, StaticSet.class, Articulated.class,
        ParticleSystemConfig.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Creates or updates a model implementation for this configuration.
         *
         * @param scope the model's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            return null;
        }
    }

    /**
     * Superclass of the imported implementations (this is not abstract because in order for the
     * exporter to create a prototype of MaterialMapping, it must be able to instantiate this
     * class).
     */
    public static class Imported extends Implementation
    {
        /**
         * Represents a mapping from texture name to material.
         */
        public class MaterialMapping extends DeepObject
            implements Exportable
        {
            /** The name of the texture. */
            @Editable(editor="choice")
            public String name;

            /** The corresponding material. */
            @Editable(nullable=true)
            public ConfigReference<MaterialConfig> material;

            /**
             * Returns the options available for the name field.
             */
            public String[] getNameOptions ()
            {
                TreeSet<String> names = new TreeSet<String>();
                getTextures(names);
                return names.toArray(new String[names.size()]);
            }
        }

        /** The model scale. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float scale = 0.01f;

        /** If true, ignore the transforms of the top-level children. */
        @Editable
        public boolean ignoreRootTransforms;

        /** If true, generate tangent attributes for meshes. */
        @Editable
        public boolean generateTangents;

        /** The mappings from texture name to material. */
        @Editable(depends={"source"})
        public MaterialMapping[] materialMappings = new MaterialMapping[0];

        /**
         * Sets the source file from which to load the animation data.
         */
        @Editable(nullable=true)
        @FileConstraints(
            description="m.exported_models",
            extensions={".mxml"},
            directory="exported_model_dir")
        public void setSource (File source)
        {
            _source = source;
            updateFromSource();
        }

        /**
         * Returns the source file.
         */
        @Editable
        public File getSource ()
        {
            return _source;
        }

        /**
         * (Re)reads the source data.
         */
        public void updateFromSource ()
        {
            if (_source == null) {
                updateFromSource(null);
                return;
            }
            if (_parser == null) {
                _parser = new ModelParser();
            }
            try {
                updateFromSource(_parser.parseModel(_source.toString()));
            } catch (Exception e) {
                log.warning("Error parsing model [source=" + _source + "].", e);
            }
        }

        /**
         * Updates from a parsed model definition.
         */
        protected void updateFromSource (ModelDef def)
        {
            // nothing by default
        }

        /**
         * Populates the supplied set with the names of all referenced textures.
         */
        protected void getTextures (TreeSet<String> textures)
        {
            // nothing by default
        }

        /** The file from which we read the model data. */
        protected File _source;
    }

    /**
     * An original static implementation.
     */
    public static class Static extends Imported
    {
        /** The meshes comprising this model. */
        public MeshSet meshes;

        @Override // documentation inherited
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            if (meshes == null) {
                return null;
            }
            if (impl instanceof StaticImpl) {
                ((StaticImpl)impl).setMeshes(meshes);
            } else {
                impl = new StaticImpl(ctx, scope, meshes);
            }
            return impl;
        }

        @Override // documentation inherited
        protected void updateFromSource (ModelDef def)
        {
            if (def == null) {
                meshes = null;
            } else {
                def.update(this);
            }
        }

        @Override // documentation inherited
        protected void getTextures (TreeSet<String> textures)
        {
            if (meshes != null) {
                meshes.getTextures(textures);
            }
        }
    }

    /**
     * A set of static models all loaded from the same source.
     */
    public static class StaticSet extends Imported
    {
        /** The selected model. */
        @Editable(editor="choice", depends={"source"})
        public String model;

        /** Maps top-level node names to meshes. */
        public TreeMap<String, MeshSet> meshes;

        /**
         * Returns the options for the model field.
         */
        public String[] getModelOptions ()
        {
            return (meshes == null) ?
                new String[0] : meshes.keySet().toArray(new String[meshes.size()]);
        }

        @Override // documentation inherited
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            MeshSet mset = (meshes == null) ? null : meshes.get(model);
            if (mset == null) {
                return null;
            }
            if (impl instanceof StaticImpl) {
                ((StaticImpl)impl).setMeshes(mset);
            } else {
                impl = new StaticImpl(ctx, scope, mset);
            }
            return impl;
        }

        @Override // documentation inherited
        protected void updateFromSource (ModelDef def)
        {
            if (def == null) {
                model = null;
                meshes = null;
            } else {
                def.update(this);
            }
        }

        @Override // documentation inherited
        protected void getTextures (TreeSet<String> textures)
        {
            if (meshes != null) {
                for (MeshSet set : meshes.values()) {
                    set.getTextures(textures);
                }
            }
        }
    }

    /**
     * An original articulated implementation.
     */
    public static class Articulated extends Imported
    {
        /**
         * Represents an attachment point on an articulated model.
         */
        public class AttachmentPoint extends DeepObject
            implements Exportable
        {
            /** The name of the node representing the attachment point. */
            @Editable(editor="choice")
            public String node;

            /** The model to attach to the node. */
            @Editable(nullable=true)
            public ConfigReference<ModelConfig> attachment;

            /**
             * Returns the options available for the node field.
             */
            public String[] getNodeOptions ()
            {
                if (root == null) {
                    return new String[0];
                }
                ArrayList<String> names = new ArrayList<String>();
                root.getNames(names);
                QuickSort.sort(names);
                return names.toArray(new String[names.size()]);
            }
        }

        /** The model's animation mappings. */
        @Editable
        public AnimationMapping[] animationMappings = new AnimationMapping[0];

        /** The model's attachment points. */
        @Editable(depends={"source"})
        public AttachmentPoint[] attachmentPoints = new AttachmentPoint[0];

        /** The root node. */
        public NodeConfig root;

        /** The skin meshes. */
        public MeshSet skin;

        @Override // documentation inherited
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            return null;
        }

        @Override // documentation inherited
        protected void updateFromSource (ModelDef def)
        {
            if (def == null) {
                root = null;
                skin = null;
            } else {
                def.update(this);
            }
        }

        @Override // documentation inherited
        protected void getTextures (TreeSet<String> textures)
        {
            if (root != null) {
                root.getTextures(textures);
            }
            if (skin != null) {
                skin.getTextures(textures);
            }
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The model reference. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        @Override // documentation inherited
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            if (model == null) {
                return null;
            }
            ModelConfig config = ctx.getConfigManager().getConfig(ModelConfig.class, model);
            return (config == null) ? null : config.getModelImplementation(ctx, scope, impl);
        }
    }

    /**
     * A node within an {@link Articulated} model.
     */
    public static class NodeConfig extends DeepObject
        implements Exportable
    {
        /** The name of the node. */
        public String name;

        /** The initial transform of the node. */
        public Transform3D transform;

        /** The children of the node. */
        public NodeConfig[] children;

        public NodeConfig (String name, Transform3D transform, NodeConfig[] children)
        {
            this.name = name;
            this.transform = transform;
            this.children = children;
        }

        public NodeConfig ()
        {
        }

        /**
         * Populates the supplied list with the names of all nodes.
         */
        public void getNames (ArrayList<String> names)
        {
            names.add(name);
            for (NodeConfig child : children) {
                child.getNames(names);
            }
        }

        /**
         * Populates the supplied set with the names of all textures.
         */
        public void getTextures (TreeSet<String> textures)
        {
            for (NodeConfig child : children) {
                child.getTextures(textures);
            }
        }
    }

    /**
     * A node containing a mesh.
     */
    public static class MeshNodeConfig extends NodeConfig
    {
        /** The node's visible mesh. */
        public VisibleMesh visible;

        /** The collision mesh. */
        public CollisionMesh collision;

        public MeshNodeConfig (
            String name, Transform3D transform, NodeConfig[] children,
            VisibleMesh visible, CollisionMesh collision)
        {
            super(name, transform, children);
            this.visible = visible;
            this.collision = collision;
        }

        public MeshNodeConfig ()
        {
        }

        @Override // documentation inherited
        public void getTextures (TreeSet<String> textures)
        {
            super.getTextures(textures);
            if (visible != null) {
                textures.add(visible.texture);
            }
        }
    }

    /**
     * Contains a set of meshes.
     */
    public static class MeshSet extends DeepObject
        implements Exportable
    {
        /** The visible meshes. */
        public VisibleMesh[] visible;

        /** The collision mesh. */
        public CollisionMesh collision;

        public MeshSet (VisibleMesh[] visible, CollisionMesh collision)
        {
            this.visible = visible;
            this.collision = collision;
        }

        public MeshSet ()
        {
        }

        /**
         * Populates the supplied set with the names of all referenced textures.
         */
        public void getTextures (TreeSet<String> textures)
        {
            for (VisibleMesh mesh : visible) {
                textures.add(mesh.texture);
            }
        }
    }

    /**
     * Pairs a texture name with a geometry config.
     */
    public static class VisibleMesh extends DeepObject
        implements Exportable
    {
        /** The name of the texture associated with the mesh. */
        public String texture;

        /** The mesh geometry. */
        public GeometryConfig geometry;

        public VisibleMesh (String texture, GeometryConfig geometry)
        {
            this.texture = texture;
            this.geometry = geometry;
        }

        public VisibleMesh ()
        {
        }
    }

    /**
     * A named animation reference.
     */
    public static class AnimationMapping extends DeepObject
        implements Exportable
    {
        /** The name of the reference. */
        @Editable
        public String name = "";

        /** Automatically play this animation on startup/reset. */
        @Editable
        public boolean playAutomatically;

        /** The animation associated with the name. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> animation;
    }

    /** The actual model implementation. */
    @Editable
    public Implementation implementation = new Static();

    /**
     * Creates or updates a model implementation for this configuration.
     *
     * @param scope the model's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public Model.Implementation getModelImplementation (
        GlContext ctx, Scope scope, Model.Implementation impl)
    {
        return implementation.getModelImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    public void init (ConfigManager cfgmgr)
    {
        _configs.init("model", cfgmgr);
        super.init(_configs);
    }

    /** The model's local config library. */
    protected ConfigManager _configs = new ConfigManager();

    /** Parses model exports. */
    protected static ModelParser _parser;
}
