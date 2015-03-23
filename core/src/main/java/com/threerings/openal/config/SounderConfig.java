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

package com.threerings.openal.config;

import java.util.HashSet;

import com.threerings.io.Streamable;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.FileConstraints;
import com.threerings.export.Exportable;
import com.threerings.expr.BooleanExpression;
import com.threerings.expr.Scope;
import com.threerings.probs.FloatVariable;
import com.threerings.util.DeepObject;

import com.threerings.openal.Sounder;
import com.threerings.openal.util.AlContext;

/**
 * The configuration of a sounder.
 */
public class SounderConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the sounder.
     */
    @EditorTypes({
        Clip.class, MetaClip.class, VariableClip.class, Stream.class,
        MetaStream.class, Conditional.class, Compound.class, Sequential.class,
        Scripted.class, Random.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        @Deprecated
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

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
    }

    /**
     * The superclass of the implementations describing an original sounder, as opposed to one
     * derived from another configuration.
     */
    public static abstract class Original extends Implementation
    {
        /** Whether or not the position of the sound is relative to the listener. */
        @Editable(hgroup="s", weight=-2)
        public boolean sourceRelative;

        /** Whether or not the sound is directional. */
        @Editable(hgroup="s", weight=-2)
        public boolean directional;

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
        public float coneInnerAngle = 360f;

        /** The outer angle of the sound cone. */
        @Editable(min=-360, max=+360, hgroup="c")
        public float coneOuterAngle = 360f;

        /**
         * Returns the base gain (volume).
         */
        public abstract float getGain ();

        /**
         * Returns the pitch multiplier.
         */
        public abstract float getPitch ();
    }

    /**
     * An original config with fixed fields.
     */
    public static abstract class Fixed extends Original
    {
        /** The base gain (volume). */
        @Editable(min=0, step=0.01, hgroup="g", weight=-1)
        public float gain = 1f;

        /** The pitch multiplier. */
        @Editable(min=0, step=0.01, hgroup="g", weight=-1)
        public float pitch = 1f;

        @Override
        public float getGain ()
        {
            return gain;
        }

        @Override
        public float getPitch ()
        {
            return pitch;
        }
    }

    /**
     * An original config with variable fields.
     */
    public static abstract class Variable extends Original
    {
        /** The base gain (volume). */
        @Editable(min=0, step=0.01, weight=-1)
        public FloatVariable gain = new FloatVariable.Constant(1f);

        /** The pitch multiplier. */
        @Editable(min=0, step=0.01, weight=-1)
        public FloatVariable pitch = new FloatVariable.Constant(1f);

        @Override
        public float getGain ()
        {
            return gain.getValue();
        }

        @Override
        public float getPitch ()
        {
            return pitch.getValue();
        }
    }

    /**
     * Base class for {@link Clip} and {@link MetaClip}.
     */
    public static abstract class BaseClip extends Fixed
    {
        /** Whether or not the sound loops. */
        @Editable(weight=-3, hgroup="f")
        public boolean loop;
    }

    /**
     * Plays a sound clip.
     */
    public static class Clip extends BaseClip
    {
        /** The sound resource from which to load the clip. */
        @Editable(editor="resource", nullable=true, weight=-3, hgroup="f")
        @FileConstraints(
            description="m.sound_files_desc",
            extensions={".ogg"},
            directory="sound_dir")
        public String file;

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            if (file != null) {
                paths.add(file);
            }
        }

        @Override
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (file == null) {
                return null;
            }
            if (impl instanceof Sounder.Clip) {
                ((Sounder.Clip)impl).setConfig(this);
            } else {
                impl = new Sounder.Clip(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * Plays a randomly selected clip.
     */
    public static class MetaClip extends BaseClip
    {
        /** The files from which to choose. */
        @Editable(weight=-3)
        public PitchWeightedFile[] files = new PitchWeightedFile[0];

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            for (WeightedFile wfile : files) {
                if (wfile.file != null) {
                    paths.add(wfile.file);
                }
            }
        }

        @Override
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (files.length == 0) {
                return null;
            }
            if (impl instanceof Sounder.MetaClip) {
                ((Sounder.MetaClip)impl).setConfig(this);
            } else {
                impl = new Sounder.MetaClip(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * Plays a clip with variable parameters.
     */
    public static class VariableClip extends Variable
    {
        /** Whether or not the sound loops. */
        @Editable(weight=-3, hgroup="f")
        public boolean loop;

        /** The sound resource from which to load the clip. */
        @Editable(editor="resource", nullable=true, weight=-3, hgroup="f")
        @FileConstraints(
            description="m.sound_files_desc",
            extensions={".ogg"},
            directory="sound_dir")
        public String file;

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            if (file != null) {
                paths.add(file);
            }
        }

        @Override
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (file == null) {
                return null;
            }
            if (impl instanceof Sounder.VariableClip) {
                ((Sounder.VariableClip)impl).setConfig(this);
            } else {
                impl = new Sounder.VariableClip(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * Base class for {@link Stream} and {@link MetaStream}.
     */
    public static abstract class BaseStream extends Fixed
    {
        /** The interval over which to fade in the stream. */
        @Editable(min=0, step=0.01, hgroup="f")
        public float fadeIn;

        /** The interval over which to fade out the stream. */
        @Editable(min=0, step=0.01, hgroup="f")
        public float fadeOut;

        /** The stack this stream is on. */
        @Editable(hgroup="s")
        public String stack = "default";

        /** Wether we push onto the stack. */
        @Editable(hgroup="s")
        public boolean push;

        /** Whether to attenuate based on distance (used to provide
         * pseudo-spatialization for stereo streams). */
        @Editable
        public boolean attenuate;
    }

    /**
     * Plays a sound stream.
     */
    public static class Stream extends BaseStream
    {
        /** The files to enqueue in the stream. */
        @Editable(weight=-3)
        public QueuedFile[] queue = new QueuedFile[0];

        /**
         * Checks whether any of the files loop.
         */
        public boolean loops ()
        {
            for (QueuedFile queued : queue) {
                if (queued.loop) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            for (QueuedFile queued : queue) {
                if (queued.file != null) {
                    paths.add(queued.file);
                }
            }
        }

        @Override
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (queue.length == 0 || queue[0].file == null) {
                return null;
            }
            if (impl instanceof Sounder.Stream) {
                ((Sounder.Stream)impl).setConfig(this);
            } else {
                impl = new Sounder.Stream(ctx, scope, this);
            }
            return impl;
        }
    }

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

    /**
     * Selects between a number of substreams.
     */
    public static class MetaStream extends BaseStream
    {
        /** The files from which to choose. */
        @Editable(weight=-3)
        public WeightedFile[] files = new WeightedFile[0];

        /** The cross-fade between tracks. */
        @Editable(min=0, step=0.01, hgroup="f")
        public float crossFade;

        @Override
        public void getUpdateResources (HashSet<String> paths)
        {
            for (WeightedFile wfile : files) {
                if (wfile.file != null) {
                    paths.add(wfile.file);
                }
            }
        }

        @Override
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (files.length == 0) {
                return null;
            }
            if (impl instanceof Sounder.MetaStream) {
                ((Sounder.MetaStream)impl).setConfig(this);
            } else {
                impl = new Sounder.MetaStream(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * Combines a file to enqueue .
     */
    public static class WeightedFile extends DeepObject
        implements Exportable, Streamable
    {
        /** The file to stream. */
        @Editable(editor="resource", nullable=true, hgroup="f")
        @FileConstraints(
            description="m.sound_files_desc",
            extensions={".ogg"},
            directory="sound_dir")
        public String file;

        /** The weight of the file. */
        @Editable(min=0.0, step=0.01, hgroup="f")
        public float weight = 1f;

        /** The gain of the file. */
        @Editable(min=0.0, step=0.01)
        public float gain = 1f;
    }

    /**
     * A weighted file with a pitch multiplier.
     */
    public static class PitchWeightedFile extends WeightedFile
    {
        /** The pitch multiplier. */
        @Editable(min=0, step=0.01)
        public float pitch = 1f;
    }

    /**
     * Plays the first sounder whose condition evaluates to true.
     */
    public static class Conditional extends Implementation
    {
        /** The condition cases. */
        @Editable
        public Case[] cases = new Case[0];

        /** The default sounder reference. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> defaultSounder;

        @Override
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (impl instanceof Sounder.Conditional) {
                ((Sounder.Conditional)impl).setConfig(this);
            } else {
                impl = new Sounder.Conditional(ctx, scope, this);
            }
            return impl;
        }

        @Override
        public void invalidate ()
        {
            for (Case caze : cases) {
                caze.condition.invalidate();
            }
        }
    }

    /**
     * A case within a conditional sounder
     */
    public static class Case extends DeepObject
        implements Exportable
    {
        /** The condition for the case. */
        @Editable
        public BooleanExpression condition = new BooleanExpression.Constant(true);

        /** The sound reference. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> sounder;
    }

    /**
     * Plays multiple sounders simultaneously.
     */
    public static class Compound extends Implementation
    {
        /** The component sounders. */
        @Editable
        public ComponentSounder[] sounders = new ComponentSounder[0];

        @Override
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (sounders.length == 0) {
                return null;
            }
            if (impl instanceof Sounder.Compound) {
                ((Sounder.Compound)impl).setConfig(this);
            } else {
                impl = new Sounder.Compound(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * Plays multiple sounders in sequence.
     */
    public static class Sequential extends Implementation
    {
        /** Whether to loop the entire sequence. */
        @Editable
        public boolean loop;

        /** The component sounders. */
        @Editable
        public ComponentSounder[] sounders = new ComponentSounder[0];

        @Override
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (sounders.length == 0) {
                return null;
            }
            if (impl instanceof Sounder.Sequential) {
                ((Sounder.Sequential)impl).setConfig(this);
            } else {
                impl = new Sounder.Sequential(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A component sounder within a compound.
     */
    public static class ComponentSounder extends DeepObject
        implements Exportable, Streamable
    {
        /** The sound reference. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> sounder;
    }

    /**
     * Plays a scripted sequence of sounders.
     */
    public static class Scripted extends Implementation
    {
        /** The loop duration, or zero for unlooped. */
        @Editable(min=0.0, step=0.01)
        public float loopDuration;

        /** The sounders to play. */
        @Editable
        public TimedSounder[] sounders = new TimedSounder[0];

        @Override
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (sounders.length == 0) {
                return null;
            }
            if (impl instanceof Sounder.Scripted) {
                ((Sounder.Scripted)impl).setConfig(this);
            } else {
                impl = new Sounder.Scripted(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A sounder to play at a specific time.
     */
    public static class TimedSounder extends DeepObject
        implements Exportable
    {
        /** The time at which to play the sound. */
        @Editable(min=0, step=0.01)
        public float time;

        /** The sound reference. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> sounder;
    }

    /**
     * Plays a randomly selected sub-sounder.
     */
    public static class Random extends Implementation
    {
        /** The component sounders. */
        @Editable
        public WeightedSounder[] sounders = new WeightedSounder[0];

        @Override
        public Sounder.Implementation getSounderImplementation (
            AlContext ctx, Scope scope, Sounder.Implementation impl)
        {
            if (sounders.length == 0) {
                return null;
            }
            if (impl instanceof Sounder.Random) {
                ((Sounder.Random)impl).setConfig(this);
            } else {
                impl = new Sounder.Random(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A component sounder within a compound.
     */
    public static class WeightedSounder extends DeepObject
        implements Exportable
    {
        /** The weight of the sounder. */
        @Editable(min=0.0, step=0.01)
        public float weight = 1f;

        /** The sound reference. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> sounder;
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The sound reference. */
        @Editable(nullable=true)
        public ConfigReference<SounderConfig> sounder;

        @Override
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

    @Override
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }

    @Override
    protected void getUpdateResources (HashSet<String> paths)
    {
        implementation.getUpdateResources(paths);
    }
}
