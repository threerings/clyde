//
// $Id$

package com.threerings.opengl.model.config;

import java.io.File;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.Shallow;

import com.threerings.opengl.model.tools.ModelDef;
import com.threerings.opengl.model.tools.xml.ModelParser;

import com.threerings.opengl.effect.config.ParticleSystemConfig;
import com.threerings.opengl.material.config.MaterialConfig;

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
    }

    /**
     * Superclass of the imported implementations.
     */
    public static abstract class Imported extends Implementation
    {
        /** The model scale. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float scale = 0.01f;

        /** If true, ignore the transforms of the top-level children. */
        @Editable
        public boolean ignoreRootTransforms;

        /** The mappings from texture name to material. */
        @Editable
        public MaterialMapping[] materialMappings = new MaterialMapping[0];

        /**
         * Sets the source file from which to load the animation data.
         */
        @Editable(nullable=true)
        @FileConstraints(
            description="m.exported_models",
            extensions={ ".mxml" },
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
                return;
            }
            if (_parser == null) {
                _parser = new ModelParser();
            }
            ModelDef def;
            try {
                def = _parser.parseModel(_source.toString());
            } catch (Exception e) {
                log.warning("Error parsing model [source=" + _source + "].", e);
                return;
            }
        }

        /** The file from which we read the model data. */
        protected File _source;
    }

    /**
     * An original static implementation.
     */
    public static class Static extends Imported
    {
    }

    /**
     * A set of models all loaded from the same source.
     */
    public static class StaticSet extends Imported
    {
    }

    /**
     * An original articulated implementation.
     */
    public static class Articulated extends Imported
    {
        /** The model's animation mappings. */
        @Editable
        public AnimationMapping[] animationMappings = new AnimationMapping[0];

        /** The model's attachment points. */
        @Editable
        public AttachmentPoint[] attachmentPoints = new AttachmentPoint[0];
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The model reference. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;
    }

    /**
     * Represents a mapping from texture name to material.
     */
    public static class MaterialMapping extends DeepObject
        implements Exportable
    {
        /** The name of the texture. */
        @Editable
        public String name = "";

        /** The corresponding material. */
        @Editable(nullable=true)
        public ConfigReference<MaterialConfig> material;
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

    /**
     * Represents an attachment point on an articulated model.
     */
    public static class AttachmentPoint extends DeepObject
        implements Exportable
    {
        /** The name of the node representing the attachment point. */
        @Editable
        public String node = "";

        /** The model to attach to the node. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> attachment;
    }

    /** The actual model implementation. */
    @Editable
    public Implementation implementation = new Static();

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
