//
// $Id$

package com.threerings.openal.config;

import java.io.File;
import java.io.IOException;

import java.util.HashSet;

import com.threerings.resource.ResourceManager;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.openal.FileStream;
import com.threerings.openal.Sounder;
import com.threerings.openal.util.AlContext;

import static com.threerings.openal.Log.*;

/**
 * The configuration of a sounder.
 */
public class SounderConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the sounder.
     */
    @EditorTypes({ Clip.class, Stream.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        /**
         * Adds the implementation's update references to the provided set.
         */
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Adds the implementation's update resources to the provided set.
         */
        public void getUpdateResources (HashSet<String> paths)
        {
            // nothing by default
        }

        /**
         * Creates or updates a sounder implementation for this configuration.
         *
         * @param scope the sounder's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl);
    }

    /**
     * The superclass of the implementations describing an original sounder, as opposed to one
     * derived from another configuration.
     */
    public static abstract class Original extends Implementation
    {
        /** Whether or not the position of the sound is relative to the listener. */
        @Editable(hgroup="s")
        public boolean sourceRelative;

        /** Whether or not the sound is directional. */
        @Editable(hgroup="s")
        public boolean directional;

        /** The base gain (volume). */
        @Editable(min=0, max=1, step=0.01, hgroup="g")
        public float gain = 1f;

        /** The pitch multiplier. */
        @Editable(min=0, step=0.01, hgroup="g")
        public float pitch = 1f;

        /** The minimum gain for the source. */
        @Editable(min=0, max=1, step=0.01, hgroup="m")
        public float minGain;

        /** The maximum gain for the source. */
        @Editable(min=0, max=1, step=0.01, hgroup="m")
        public float maxGain = 1f;

        /** The distance at which the volume would normally drop by half (before being influenced
         * by the rolloff factor or the maximum distance). */
        @Editable(min=0, step=0.01, hgroup="r")
        public float referenceDistance = 1f;

        /** The rolloff rate. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float rolloffFactor = 1f;

        /** The distance at which attenuation stops. */
        @Editable(min=0, step=0.01, hgroup="m")
        public float maxDistance = Float.MAX_VALUE;

        /** The gain when outside the oriented cone. */
        @Editable(min=0, max=1, step=0.01, hgroup="m")
        public float coneOuterGain;

        /** The inner angle of the sound cone. */
        @Editable(min=-360, max=+360, hgroup="c")
        public int coneInnerAngle = 360;

        /** The outer angle of the sound cone. */
        @Editable(min=-360, max=+360, hgroup="c")
        public int coneOuterAngle = 360;
    }

    /**
     * Plays a sound clip.
     */
    public static class Clip extends Original
    {
        /** The sound resource from which to load the clip. */
        @Editable(editor="resource", nullable=true, weight=-1, hgroup="f")
        @FileConstraints(
            description="m.sound_files_desc",
            extensions={".ogg"},
            directory="sound_dir")
        public String file;

        /** Whether or not the sound loops. */
        @Editable(weight=-1, hgroup="f")
        public boolean loop;

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            if (file != null) {
                paths.add(file);
            }
        }

        @Override // documentation inherited
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (file == null) {
                return null;
            }
            if (impl instanceof Sounder.Clip) {
                ((Sounder.Clip)impl).setConfig(this);
            } else {
                if (impl != null) {
                    impl.dispose();
                }
                impl = new Sounder.Clip(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * Plays a sound stream.
     */
    public static class Stream extends Original
    {
        /**
         * Represents a file to enqueue in the stream.
         */
        public static class QueuedFile extends DeepObject
            implements Exportable
        {
            /** The file to stream. */
            @Editable(editor="resource", nullable=true, hgroup="f")
            @FileConstraints(
                description="m.sound_files_desc",
                extensions={".ogg"},
                directory="sound_dir")
            public String file;

            /** Whether or not to loop the file. */
            @Editable(hgroup="f")
            public boolean loop;
        }

        /** The files to enqueue in the stream. */
        @Editable(weight=-1)
        public QueuedFile[] fileQueue = new QueuedFile[0];

        /** The interval over which to fade in the stream. */
        @Editable(min=0, step=0.01)
        public float fadeIn;

        /**
         * Creates the stream corresponding to this config.
         */
        public FileStream createStream (AlContext ctx)
        {
            ResourceManager rsrcmgr = ctx.getResourceManager();
            FileStream stream;
            try {
                stream = new FileStream(
                    ctx.getSoundManager(), rsrcmgr.getResourceFile(fileQueue[0].file),
                    fileQueue[0].loop);
            } catch (IOException e) {
                log.warning("Error opening stream.", "file", fileQueue[0].file, e);
                return null;
            }
            for (int ii = 1; ii < fileQueue.length; ii++) {
                QueuedFile queued = fileQueue[ii];
                if (queued.file != null) {
                    stream.queueFile(rsrcmgr.getResourceFile(queued.file), queued.loop);
                }
            }
            return stream;
        }

        @Override // documentation inherited
        public void getUpdateResources (HashSet<String> paths)
        {
            for (QueuedFile queued : fileQueue) {
                if (queued.file != null) {
                    paths.add(queued.file);
                }
            }
        }

        @Override // documentation inherited
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (fileQueue.length == 0 || fileQueue[0].file == null) {
                return null;
            }
            if (impl instanceof Sounder.Stream) {
                ((Sounder.Stream)impl).setConfig(this);
            } else {
                if (impl != null) {
                    impl.dispose();
                }
                impl = new Sounder.Stream(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The sound reference. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> sounder;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(SounderConfig.class, sounder);
        }

        @Override // documentation inherited
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            SounderConfig config = ctx.getConfigManager().getConfig(SounderConfig.class, sounder);
            return (config == null) ? null : config.getSounderImplementation(ctx, scope, impl);
        }
    }

    /** The actual sound implementation. */
    @Editable
    public Implementation implementation = new Clip();

    /**
     * Creates or updates sounder implementation for this configuration.
     *
     * @param scope the sounder's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public Sounder.Implementation getSounderImplementation (
        AlContext ctx, Scope scope, Sounder.Implementation impl)
    {
        return implementation.getSounderImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }

    @Override // documentation inherited
    protected void getUpdateResources (HashSet<String> paths)
    {
        implementation.getUpdateResources(paths);
    }
}
