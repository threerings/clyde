//
// $Id$

package com.threerings.tudey.data;

import java.util.Set;
import java.io.File;

import com.threerings.editor.Editable;
import com.threerings.editor.FileConstraints;

import com.threerings.opengl.renderer.Color4f;

/**
 * Represents the global environment of the scene.
 */
public class Environment extends SceneElement
{
    /**
     * One of the global directional lights.
     */
    public static class DirectionalLight extends SceneElement
    {
        /** The light color. */
        @Editable
        public Color4f color = new Color4f();

        /** The azimuth of the light direction. */
        @Editable(min=-180, max=+180, mode="wide")
        public float azimuth;

        /** The elevation of the light direction. */
        @Editable(min=-90, max=+90, mode="wide")
        public float elevation = 45f;
    }

    /**
     * Represents the global fog.
     */
    public abstract class Fog extends SceneElement
    {
        /**
         * Linear fog.
         */
        public class LinearFog extends Fog
        {
            /** The fog start distance. */
            @Editable(min=0, step=0.1)
            public float start;

            /** The fog end distance. */
            @Editable(min=0, step=0.1)
            public float end;
        }

        /**
         * Exponential fog.
         */
        public class ExponentialFog extends Fog
        {
            /** Whether or not to square the exponential function. */
            @Editable
            public boolean squared;

            /** The fog density. */
            @Editable(min=0, step=0.001)
            public float density;
        }

        /** The fog color. */
        @Editable
        public Color4f color = new Color4f();
    }

    /**
     * The ambient music.
     */
    public static class Music extends SceneElement
    {
        /** The music track. */
        @Editable
        public String track = "";

        /** The gain at which to play the music. */
        @Editable(min=0f, max=1f, step=0.01f, mode="wide")
        public float gain = 0.3f;
    }

    /**
     * The sky model.
     */
    public static class Sky extends SceneElement
    {
        /** Particle effects applied to the sky. */
        @Editable(nullable=false)
        public SkyParticleEffect[] particles = new SkyParticleEffect[0];

        /**
         * A sky particle effect.
         */
        public static class SkyParticleEffect extends SceneElement
        {
            /**
             * Sets the path to the effect to use.
             */
            @Editable
            @FileConstraints(
                description="m.particle_files",
                extensions={".dat"},
                directory="particle_dir")
            public void setEffect (File effect)
            {
                _effectFile = (effect != null) ? getResourcePath(effect) : null;
            }

            /**
             * Returns the path of the effect.
             */
            @Editable
            public File getEffect ()
            {
                return _effectFile != null ? new File(_effectFile) : null;
            }

            /**
             * Adds the effect's resources to the set.
             */
            public void getResources (Set<SceneResource> results)
            {
                if (_effectFile != null) {
                    results.add(new SceneResource.Model(_effectFile));
                }
            }

            /** Path to the particle effect. */
            protected String _effectFile;
        }

        /**
         * Sets the path to the model to use.
         */
        @Editable
        @FileConstraints(
            description="m.model_files",
            extensions={".properties", ".dat"},
            directory="sky_dir")
        public void setModel (File model)
        {
            _modelFile = (model != null) ? getResourcePath(model) : null;
        }

        /**
         * Returns the path of the model.
         */
        @Editable
        public File getModel ()
        {
            return _modelFile != null ? new File(_modelFile) : null;
        }

        /**
         * Sets the path to the animation to use.
         */
        @Editable
        @FileConstraints(
            description="m.animation_files",
            extensions={".properties", ".dat"},
            directory="sky_dir")
        public void setAnimation (File anim)
        {
            if (anim != null) {
                _animFile = getResourcePath(anim);
                // strip out the trailing /animation
                _animFile = _animFile.substring(0, _animFile.lastIndexOf('/'));
            } else {
                _animFile = null;
            }
        }

        /**
         * Returns the path of the model.
         */
        @Editable
        public File getAnimation ()
        {
            return _animFile != null ? new File(_animFile) : null;
        }

        @Override // documentation inherited
        public void getResources (Set<SceneResource> results)
        {
            if (_modelFile != null) {
                results.add(new SceneResource.Model(_modelFile));
            }
            if (_animFile != null) {
                results.add(new SceneResource.Animation(_animFile));
            }
            for (SkyParticleEffect particle : particles) {
                particle.getResources(results);
            }
        }

        /**
         * Returns the resource path for the given file.
         */
        protected static String getResourcePath (File file)
        {
            String path = file.getPath();
            // strip everything up to and including the rsrc directory
            path = path.substring(path.lastIndexOf("rsrc") + "rsrc/".length());
            // strip the extension
            path = path.substring(0, path.lastIndexOf("."));
            // fix the separator characters
            path = path.replace(File.separatorChar, '/');
            return path;
        }

        /** The path of the model. */
        protected String _modelFile;

        /** The path of the animation. */
        protected String _animFile;
    }

    /** The ambient light intensity. */
    @Editable
    public Color4f ambientLight = new Color4f();

    /** The primary directional light. */
    @Editable
    public DirectionalLight primaryLight;

    /** The secondary directional light. */
    @Editable
    public DirectionalLight secondaryLight;

    /** The fog parameters. */
    @Editable(types={ Fog.LinearFog.class, Fog.ExponentialFog.class })
    public Fog fog;

    /** The sky parameters. */
    @Editable
    public Sky sky;

    /** The music parameters. */
    @Editable
    public Music music;

    /** The intensity of the shadows. */
    @Editable(min=0.0, max=1.0, step=0.01)
    public float shadowIntensity = 0.25f;

    @Override // documentation inherited
    public void getResources (Set<SceneResource> results)
    {
        if (sky != null) {
            sky.getResources(results);
        }
        if (music != null) {
            music.getResources(results);
        }
    }
}
