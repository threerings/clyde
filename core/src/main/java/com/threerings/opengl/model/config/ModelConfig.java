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

package com.threerings.opengl.model.config;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeSet;

import proguard.annotation.Keep;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.util.ComparableTuple;
import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.config.Reference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.editor.Strippable;
import com.threerings.editor.util.EditorContext;
import com.threerings.editor.util.Validator;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Box;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.compositor.config.RenderSchemeConfig;
import com.threerings.opengl.effect.config.MetaParticleSystemConfig;
import com.threerings.opengl.effect.config.ParticleSystemConfig;
import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.gui.config.ComponentBillboardConfig;
import com.threerings.opengl.material.config.GeometryMaterial;
import com.threerings.opengl.material.config.MaterialConfig;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.CollisionMesh;
import com.threerings.opengl.model.tools.ModelDef;
import com.threerings.opengl.model.tools.xml.ModelParser;
import com.threerings.opengl.renderer.config.TextureConfig;
import com.threerings.opengl.scene.config.ViewerAffecterConfig;
import com.threerings.opengl.scene.config.SceneInfluencerConfig;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.GlContextWrapper;
import com.threerings.opengl.util.Preloadable;

import com.threerings.tudey.config.ActorModelConfig;
import com.threerings.tudey.shape.config.ShapeModelConfig;

import static com.threerings.opengl.Log.log;

/**
 * The configuration of a model.
 */
public class ModelConfig extends ParameterizedConfig
    implements Preloadable.LoadableConfig
{
    /** The default tag for unskinned meshes. */
    public static final String DEFAULT_TAG = "default";

    /** The default tag for skinned meshes. */
    public static final String SKINNED_TAG = "skinned";

    /** Determines when this model is added as a transient to the scene. */
    public enum TransientPolicy { DEFAULT, NEVER, FRUSTUM, BOUNDS, ALWAYS };

    /**
     * Contains the actual implementation of the model.
     */
    @EditorTypes({
        StaticConfig.class, StaticSetConfig.class, MergedStaticConfig.class,
        GeneratedStaticConfig.class, ArticulatedConfig.class, ParticleSystemConfig.class,
        MetaParticleSystemConfig.class, SceneInfluencerConfig.class, ViewerAffecterConfig.class,
        ComponentBillboardConfig.class, ConditionalConfig.class, CompoundConfig.class,
        ScriptedConfig.class, ActorModelConfig.Wrapper.class,
        ShapeModelConfig.class, Derived.class, Schemed.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable, Preloadable.LoadableConfig
    {
        /**
         * Retrieves a reference to the underlying original implementation.
         */
        public Implementation getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        /**
         * Updates this implementation from its external source, if any.
         *
         * @param force if true, reload the source data even if it has already been loaded.
         */
        public void updateFromSource (EditorContext ctx, boolean force)
        {
            // nothing by default
        }

        @Deprecated
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Returns a reference to the config manager to use when resolving references.
         *
         * @param cfgmgr the config manager of the config containing the implementation.
         */
        public ConfigManager getConfigManager (ConfigManager cfgmgr)
        {
            return cfgmgr;
        }

        /**
         * Creates or updates a model implementation for this configuration.
         *
         * @param scope the model's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl);

        /**
         * Returns the {@link GeometryConfig} to use when this model is selected for use within a
         * particle system (or <code>null</code> if it cannot be used).
         */
        public GeometryConfig getParticleGeometry (GlContext ctx)
        {
            return null;
        }

        /**
         * Returns a reference to the material to use when this model is selected for use within a
         * particle system.
         */
        public ConfigReference<MaterialConfig> getParticleMaterial (GlContext ctx)
        {
            return null;
        }

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
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
            @Editable(editor="choice", hgroup="t")
            public String texture = "";

            /** The name of the tag. */
            @Editable(hgroup="t")
            public String tag = DEFAULT_TAG;

            /** The material for unskinned meshes. */
            @Editable(nullable=true)
            public ConfigReference<MaterialConfig> material;

            public MaterialMapping (String texture, String tag, String path)
            {
                this.texture = texture;
                this.tag = tag;

                this.material = new ConfigReference<MaterialConfig>(
                    SKINNED_TAG.equals(tag) ? SKINNED_MATERIAL : DEFAULT_MATERIAL,
                    "Texture", new ConfigReference<TextureConfig>(DEFAULT_TEXTURE, "File", path));
            }

            public MaterialMapping ()
            {
            }

            /**
             * Returns the options available for the texture field.
             */
            @Keep
            public String[] getTextureOptions ()
            {
                TreeSet<String> textures = new TreeSet<String>();
                getTextures(textures);
                return textures.toArray(new String[textures.size()]);
            }
        }

        /** The model scale. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float scale = 0.01f;

        /** A fixed amount by which to expand the bounds (to account for skinning). */
        @Editable(min=0, step=0.01, hgroup="s")
        public float boundsExpansion;

        /** If true, ignore the transforms of the top-level children. */
        @Editable(hgroup="i")
        public boolean ignoreRootTransforms;

        /** If true, generate tangent attributes for meshes. */
        @Editable(hgroup="i")
        public boolean generateTangents;

        /** The influences allowed to affect this model. */
        @Editable
        public InfluenceFlagConfig influences = new InfluenceFlagConfig();

        /** The mappings from texture name to material. */
        @Editable(depends={"source"})
        public MaterialMapping[] materialMappings = new MaterialMapping[0];

        /**
         * Sets the source file from which to load the animation data.
         */
        @Editable(editor="resource", nullable=true)
        @FileConstraints(
            description="m.exported_models",
            extensions={".mxml"},
            directory="exported_model_dir")
        public void setSource (String source)
        {
            _source = source;
            _reload = true;
        }

        /**
         * Returns the source file.
         */
        @Editable
        public String getSource ()
        {
            return _source;
        }

        @Override
        public void updateFromSource (EditorContext ctx, boolean force)
        {
            if (!(_reload || force)) {
                return;
            }
            _reload = false;
            if (_source == null) {
                updateFromSource(null);
                return;
            }
            if (_parser == null) {
                _parser = new ModelParser();
            }
            try {
                updateFromSource(_parser.parseModel(
                    ctx.getResourceManager().getResource(_source)));
                createDefaultMaterialMappings();
            } catch (Exception e) {
                log.warning("Error parsing model [source=" + _source + "].", e);
            }
        }

        @Override
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            return null;
        }

        @Override
        public GeometryConfig getParticleGeometry (GlContext ctx)
        {
            VisibleMesh mesh = getParticleMesh();
            return (mesh == null) ? null : mesh.geometry;
        }

        @Override
        public ConfigReference<MaterialConfig> getParticleMaterial (GlContext ctx)
        {
            VisibleMesh mesh = getParticleMesh();
            if (mesh == null) {
                return null;
            }
            MaterialMapping mapping = getMaterialMapping(mesh.texture, mesh.tag);
            return (mapping == null) ? null : mapping.material;
        }

        @Override
        public void preload (GlContext ctx)
        {
            for (MaterialMapping mapping : materialMappings) {
                new Preloadable.Config(MaterialConfig.class, mapping.material).preload(ctx);
            }
        }

        /**
         * Returns the {@link VisibleMesh} to use when this model is selected for use within a
         * particle system (or <code>null</code> if it cannot be used).
         */
        protected VisibleMesh getParticleMesh ()
        {
            return null;
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
            TreeSet<ComparableTuple<String, String>> pairs = Sets.newTreeSet();
            getTextureTagPairs(pairs);
            ArrayList<MaterialMapping> mappings = new ArrayList<MaterialMapping>();
            Collections.addAll(mappings, materialMappings);
            String pref = _source.substring(0, _source.lastIndexOf('/') + 1);
            for (ComparableTuple<String, String> pair : pairs) {
                String texture = pair.left, tag = pair.right;
                if (getMaterialMapping(texture, tag) == null) {
                    mappings.add(new MaterialMapping(
                        texture, tag, StringUtil.isBlank(texture) ? null : pref + texture));
                }
            }
            materialMappings = mappings.toArray(new MaterialMapping[mappings.size()]);
        }

        /**
         * Returns the material mapping for the specified texture (if any).
         */
        protected MaterialMapping getMaterialMapping (String texture, String tag)
        {
            for (MaterialMapping mapping : materialMappings) {
                if (Objects.equal(texture, mapping.texture) && tag.equals(mapping.tag)) {
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

        /**
         * Populates the supplied set with the names of all referenced texture/tag pairs.
         */
        protected void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
        {
            // nothing by default
        }

        /**
         * Creates the array of resolved geometry/material pairs.
         */
        protected static GeometryMaterial[] getGeometryMaterials (
            GlContext ctx, VisibleMesh[] meshes, MaterialMapping[] materialMappings)
        {
            GeometryMaterial[] gmats = new GeometryMaterial[meshes.length];
            Map<String, MaterialConfig> mmap = Maps.newHashMap();
            for (int ii = 0; ii < gmats.length; ii++) {
                VisibleMesh mesh = meshes[ii];
                gmats[ii] = new GeometryMaterial(mesh.geometry, Model.getMaterialConfig(
                    ctx, mesh.texture, mesh.tag, materialMappings, mmap));
            }
            return gmats;
        }

        /** The resource from which we read the model data. */
        protected String _source;

        /** Indicates that {@link #updateFromSource} should reload the data. */
        @DeepOmit
        protected transient boolean _reload;
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The model reference. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        @Override
        public Implementation getOriginal (ConfigManager cfgmgr)
        {
            ModelConfig config = cfgmgr.getConfig(ModelConfig.class, model);
            return (config == null) ? null : config.getOriginal();
        }

        @Override
        public void preload (GlContext ctx)
        {
            new Preloadable.Model(model).preload(ctx);
        }

        @Override
        public ConfigManager getConfigManager (ConfigManager cfgmgr)
        {
            ModelConfig config = cfgmgr.getConfig(ModelConfig.class, model);
            return (config == null) ? cfgmgr : config.getConfigManager();
        }

        @Override
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            ModelConfig config = ctx.getConfigManager().getConfig(ModelConfig.class, model);
            return (config == null) ? null : config.getModelImplementation(
                createContextWrapper(ctx, config), scope, impl);
        }

        @Override
        public GeometryConfig getParticleGeometry (GlContext ctx)
        {
            ModelConfig config = ctx.getConfigManager().getConfig(ModelConfig.class, model);
            return (config == null) ? null : config.getParticleGeometry(ctx);
        }

        @Override
        public ConfigReference<MaterialConfig> getParticleMaterial (GlContext ctx)
        {
            ModelConfig config = ctx.getConfigManager().getConfig(ModelConfig.class, model);
            return (config == null) ? null : config.getParticleMaterial(ctx);
        }
    }

    /**
     * Chooses different models based on the render scheme.
     */
    public static class Schemed extends Implementation
    {
        /** The models and their associated render schemes. */
        @Editable
        public SchemedModel[] models = new SchemedModel[0];

        @Override
        public void preload (GlContext ctx)
        {
            for (SchemedModel model : models) {
                new Preloadable.Model(model.model).preload(ctx);
            }
        }

        @Override
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            ConfigReference<ModelConfig> model = getModel(scope);
            ModelConfig config = ctx.getConfigManager().getConfig(ModelConfig.class, model);
            return (config == null) ? null : config.getModelImplementation(
                createContextWrapper(ctx, config), scope, impl);
        }

        /**
         * Returns the active model reference.
         */
        protected ConfigReference<ModelConfig> getModel (Scope scope)
        {
            // as we do with the material techniques, first look for an exact match
            String scheme = ScopeUtil.resolve(scope, "renderScheme", (String)null);
            for (SchemedModel smodel : models) {
                if (Objects.equal(smodel.scheme, scheme)) {
                    return smodel.model;
                }
            }
            // then return whatever's at the top of the list
            return (models.length > 0) ? models[0].model : null;
        }
    }

    /**
     * Combines a render scheme with a model reference.
     */
    public static class SchemedModel extends DeepObject
        implements Exportable
    {
        /** The render scheme with which this model is associated. */
        @Editable(nullable=true)
        @Reference(RenderSchemeConfig.class)
        public String scheme;

        /** The model reference. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;
    }

    /**
     * Base wrapper implementation.
     */
    public abstract static class BaseWrapper extends Implementation
    {
        @Override
        public ConfigManager getConfigManager (ConfigManager cfgmgr)
        {
            ModelConfig config = getModelConfig(cfgmgr);
            return (config == null) ? cfgmgr : config.getConfigManager();
        }

        @Override
        public Model.Implementation getModelImplementation (
            GlContext ctx, Scope scope, Model.Implementation impl)
        {
            ModelConfig config = getModelConfig(ctx.getConfigManager());
            return (config == null) ? null : config.getModelImplementation(
                createContextWrapper(ctx, config), scope, impl);
        }

        @Override
        public GeometryConfig getParticleGeometry (GlContext ctx)
        {
            ModelConfig config = getModelConfig(ctx.getConfigManager());
            return (config == null) ? null : config.getParticleGeometry(ctx);
        }

        @Override
        public ConfigReference<MaterialConfig> getParticleMaterial (GlContext ctx)
        {
            ModelConfig config = getModelConfig(ctx.getConfigManager());
            return (config == null) ? null : config.getParticleMaterial(ctx);
        }

        /**
         * Get the model config.
         */
        protected abstract ModelConfig getModelConfig (ConfigManager cfgmgr);
    }

    /**
     * Contains a set of meshes.
     */
    public static class MeshSet extends DeepObject
        implements Exportable
    {
        /** The bounds of the meshes. */
        public Box bounds;

        /** The visible meshes. */
        public VisibleMesh[] visible;

        /** The collision mesh. */
        public CollisionMesh collision;

        public MeshSet (VisibleMesh[] visible, CollisionMesh collision)
        {
            bounds = new Box();
            for (VisibleMesh mesh : (this.visible = visible)) {
                bounds.addLocal(mesh.geometry.getBounds());
            }
            if ((this.collision = collision) != null) {
                bounds.addLocal(collision.getBounds());
            }
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

        /**
         * Populates the supplied set with the names of all referenced texture/tag pairs.
         */
        public void getTextureTagPairs (TreeSet<ComparableTuple<String, String>> pairs)
        {
            for (VisibleMesh mesh : visible) {
                pairs.add(new ComparableTuple<String, String>(mesh.texture, mesh.tag));
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
        public String texture = "";

        /** The mesh tag. */
        public String tag = DEFAULT_TAG;

        /** The mesh geometry. */
        public GeometryConfig geometry;

        public VisibleMesh (String texture, String tag, GeometryConfig geometry)
        {
            this.texture = texture;
            this.tag = tag;
            this.geometry = geometry;
        }

        public VisibleMesh ()
        {
        }
    }

    /** The actual model implementation. */
    @Editable
    public Implementation implementation;

    /**
     * Default constructor.
     */
    public ModelConfig ()
    {
        implementation = new StaticConfig();
    }

    /**
     * Constructor that takes an already-created implementation.
     */
    public ModelConfig (Implementation impl)
    {
        implementation = impl;
    }

    /**
     * Retrieves a reference to the underlying original implementation.
     */
    public Implementation getOriginal ()
    {
        return implementation.getOriginal(_configs);
    }

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

    /**
     * Returns the {@link GeometryConfig} to use when this model is selected for use within a
     * particle system (or <code>null</code> if it cannot be used).
     */
    public GeometryConfig getParticleGeometry (GlContext ctx)
    {
        return implementation.getParticleGeometry(ctx);
    }

    /**
     * Returns a reference to the material to use when this model is selected for use within a
     * particle system.
     */
    public ConfigReference<MaterialConfig> getParticleMaterial (GlContext ctx)
    {
        return implementation.getParticleMaterial(ctx);
    }

    public void preload (GlContext ctx)
    {
        implementation.preload(ctx);
    }

    @Override
    public void init (ConfigManager cfgmgr)
    {
        _configs.init("model", cfgmgr);
        super.init(_configs);
    }

    @Override
    public ConfigManager getConfigManager ()
    {
        return implementation.getConfigManager(_configs);
    }

    @Override
    public void updateFromSource (EditorContext ctx, boolean force)
    {
        implementation.updateFromSource(ctx, force);
    }

    @Override
    public boolean validateReferences (Validator validator)
    {
        boolean valid = super.validateReferences(validator);
        validator.pushWhere("model.cfgmgr");
        try {
            return _configs.validateReferences(validator) & valid;
        } finally {
            validator.popWhere();
        }
    }

    @Override
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }

    /**
     * Creates a context wrapper that exposes the provided config's embedded config manager.
     */
    protected static GlContext createContextWrapper (GlContext ctx, final ModelConfig config)
    {
        return new GlContextWrapper(ctx) {
            @Override public ConfigManager getConfigManager () {
                return config.getConfigManager();
            }
        };
    }

    /** The model's local config library. */
    protected ConfigManager _configs = new ConfigManager();

    /** Parses model exports. */
    protected static ModelParser _parser;

    /** The default material for the default tag. */
    protected static final String DEFAULT_MATERIAL = "Model/Opaque";

    /** The default texture config (which we expect to take a single parameter, "File,"
     * representing the texture image path). */
    protected static final String DEFAULT_TEXTURE = "2D/File/Default";

    /** The material to use for SKINNED_TAG. */
    protected static final String SKINNED_MATERIAL = "Model/Skinned/Opaque";
}
