//
// $Id$

package com.threerings.opengl.model.config;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.mod.Model;
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
        StaticConfig.class, StaticSetConfig.class, ArticulatedConfig.class,
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
            public String texture;

            /** The corresponding material. */
            @Editable(nullable=true)
            public ConfigReference<MaterialConfig> material;

            public MaterialMapping (String texture)
            {
                this.texture = texture;
            }

            public MaterialMapping ()
            {
            }

            /**
             * Returns the options available for the texture field.
             */
            public String[] getTextureOptions ()
            {
                TreeSet<String> textures = new TreeSet<String>();
                getTextures(textures);
                return textures.toArray(new String[textures.size()]);
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
                createDefaultMaterialMappings();
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
         * Creates default material mappings for any unmapped textures.
         */
        protected void createDefaultMaterialMappings ()
        {
            TreeSet<String> textures = new TreeSet<String>();
            getTextures(textures);
            ArrayList<MaterialMapping> mappings = new ArrayList<MaterialMapping>();
            Collections.addAll(mappings, materialMappings);
            for (String texture : textures) {
                if (getMaterialMapping(texture) == null) {
                    mappings.add(new MaterialMapping(texture));
                }
            }
            materialMappings = mappings.toArray(new MaterialMapping[mappings.size()]);
        }

        /**
         * Returns the material mapping for the specified texture (if any).
         */
        protected MaterialMapping getMaterialMapping (String texture)
        {
            for (MaterialMapping mapping : materialMappings) {
                if (texture.equals(mapping.texture)) {
                    return mapping;
                }
            }
            return null;
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

    /** The actual model implementation. */
    @Editable
    public Implementation implementation = new StaticConfig();

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
