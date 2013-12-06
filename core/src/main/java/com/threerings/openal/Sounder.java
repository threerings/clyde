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

package com.threerings.openal;

import java.io.IOException;

import java.nio.ByteBuffer;

import org.lwjgl.openal.AL10;

import com.samskivert.util.RandomUtil;

import com.threerings.resource.ResourceManager;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.BooleanExpression;
import com.threerings.expr.Bound;
import com.threerings.expr.MutableBoolean;
import com.threerings.expr.MutableFloat;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.openal.Listener;
import com.threerings.openal.SoundGroup;
import com.threerings.openal.Source;
import com.threerings.openal.config.SounderConfig;
import com.threerings.openal.config.SounderConfig.QueuedFile;
import com.threerings.openal.config.SounderConfig.WeightedFile;
import com.threerings.openal.config.SounderConfig.PitchWeightedFile;
import com.threerings.openal.util.AlContext;

import static com.threerings.openal.Log.log;

/**
 * Plays a sound.
 */
public class Sounder extends SimpleScope
    implements ConfigUpdateListener<SounderConfig>
{
    /**
     * The actual sounder implementation.
     */
    public static abstract class Implementation extends SimpleScope
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (AlContext ctx, Scope parentScope)
        {
            super(parentScope);
            _ctx = ctx;
        }

        /**
         * Checks whether the configured sound loops.
         */
        public boolean loops ()
        {
            return false;
        }

        /**
         * Starts playing the sound.
         */
        public abstract void start ();

        /**
         * Stops the sound.
         */
        public abstract void stop ();

        /**
         * Checks whether the sound is currently playing.
         */
        public abstract boolean isPlaying ();

        /**
         * Updates the sound.
         */
        public void update ()
        {
            // nothing by default
        }

        @Override
        public String getScopeName ()
        {
            return "impl";
        }

        @Override
        public void dispose ()
        {
            super.dispose();
            stop();
        }

        /** The application context. */
        protected AlContext _ctx;

        /** The sound transform. */
        @Bound
        protected Transform3D _transform;

        /** Whether or not the sound has been started. */
        @Bound
        protected MutableBoolean _started;
    }

    /**
     * Base class for {@link Clip}, {@link MetaClip}, and {@link VariableClip}.
     */
    public static abstract class BaseClip extends Implementation
    {
        /**
         * Creates a new implementation.
         */
        public BaseClip (AlContext ctx, Scope parentScope)
        {
            super(ctx, parentScope);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.Original config)
        {
            _config = config;
        }

        @Override
        public void stop ()
        {
            if (_sound != null) {
                _sound.stop();
            }
        }

        @Override
        public boolean isPlaying ()
        {
            return _sound != null && (_sound.isPlaying() || _sound.isPending());
        }

        @Override
        public void update ()
        {
            if (_sound != null) {
                updateSoundTransform();
            }
        }

        @Override
        public void scopeUpdated (ScopeEvent event)
        {
            super.scopeUpdated(event);
            updateFromConfig();
        }

        /**
         * Updates the clip from its config.
         */
        protected abstract void updateFromConfig ();

        /**
         * Plays the sound.
         */
        protected void playSound (float gain)
        {
            if (_sound != null) {
                updateSoundTransform();
                SoundClipManager clipmgr = ScopeUtil.resolve(
                    _parentScope, "clipmgr", null, SoundClipManager.class);
                if (clipmgr != null && !loops()) {
                    clipmgr.playSound(_sound, gain);
                } else {
                    _sound.play(null, loops());
                }
            }
        }

        /**
         * Updates the position and direction of the sound.
         */
        protected void updateSoundTransform ()
        {
            _transform.extractTranslation(_vector);
            _sound.setPosition(_vector.x, _vector.y, _vector.z);
            if (_config.directional) {
                _transform.transformVector(Vector3f.UNIT_X, _vector).normalizeLocal();
                _sound.setDirection(_vector.x, _vector.y, _vector.z);
            }
        }

        /**
         * Retrieves the sound corresponding to the specified file.
         *
         * @param sound an existing sound to reuse, if appropriate.
         */
        protected Sound getSound (String file, Sound sound)
        {
            return getSound(file, 1f, 1f, sound);
        }

        /**
         * Retrieves the sound corresponding to the specified file.
         *
         * @param sound an existing sound to reuse, if appropriate.
         */
        protected Sound getSound (String file, float gain, float pitch, Sound sound)
        {
            // resolve the group
            SoundGroup group = ScopeUtil.resolve(
                _parentScope, "soundGroup", null, SoundGroup.class);
            if (sound == null || sound.getGroup() != group || sound.getBuffer() == null ||
                    !sound.getBuffer().getPath().equals(file)) {
                if (sound != null) {
                    sound.stop();
                }
                sound = (file == null || group == null) ? null : group.getSound(file);
            }
            if (sound != null) {
                sound.setGain(gain * _config.getGain());
                sound.setSourceRelative(_config.sourceRelative);
                sound.setMinGain(_config.minGain);
                sound.setMaxGain(_config.maxGain);
                sound.setReferenceDistance(_config.referenceDistance);
                sound.setRolloffFactor(_config.rolloffFactor);
                sound.setMaxDistance(_config.maxDistance);
                sound.setPitch(pitch * _config.getPitch());
                sound.setConeInnerAngle(_config.coneInnerAngle);
                sound.setConeOuterAngle(_config.coneOuterAngle);
                sound.setConeOuterGain(_config.coneOuterGain);
            }
            return sound;
        }

        /** The implementation configuration. */
        protected SounderConfig.Original _config;

        /** The (currently playing) sound. */
        protected Sound _sound;

        /** A result vector for computation. */
        protected Vector3f _vector = new Vector3f();
    }

    /**
     * Plays a sound clip.
     */
    public static class Clip extends BaseClip
    {
        /**
         * Creates a new clip implementation.
         */
        public Clip (AlContext ctx, Scope parentScope, SounderConfig.Clip config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.Clip config)
        {
            super.setConfig(_config = config);
            updateFromConfig();
        }

        @Override
        public boolean loops ()
        {
            return _config.loop;
        }

        @Override
        public void start ()
        {
            playSound(_config.gain);
        }

        @Override
        protected void updateFromConfig ()
        {
            boolean wasPlaying = isPlaying();
            _sound = getSound(_config.file, _sound);
            if ((wasPlaying || _started.value && _config.loop) && !isPlaying()) {
                start();
            }
        }

        /** The clip config. */
        protected SounderConfig.Clip _config;
    }

    /**
     * Plays a randomly selected clip.
     */
    public static class MetaClip extends BaseClip
    {
        /**
         * Creates a new clip implementation.
         */
        public MetaClip (AlContext ctx, Scope parentScope, SounderConfig.MetaClip config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.MetaClip config)
        {
            super.setConfig(_config = config);
            updateFromConfig();
        }

        @Override
        public boolean loops ()
        {
            return _config.loop;
        }

        @Override
        public void start ()
        {
            int idx = RandomUtil.getWeightedIndex(_weights);
            _sound = _sounds[idx];
            playSound(_config.gain * _config.files[idx].gain);
        }

        @Override
        protected void updateFromConfig ()
        {
            boolean wasPlaying = isPlaying();
            Sound[] osounds = _sounds;
            _sounds = new Sound[_config.files.length];
            _weights = new float[_config.files.length];
            for (int ii = 0; ii < _sounds.length; ii++) {
                PitchWeightedFile wfile = _config.files[ii];
                _sounds[ii] = getSound(wfile.file, wfile.gain, wfile.pitch,
                    (osounds != null && ii < osounds.length) ? osounds[ii] : null);
                _weights[ii] = wfile.weight;
            }
            if (osounds != null) {
                for (int ii = _sounds.length; ii < osounds.length; ii++) {
                    Sound osound = osounds[ii];
                    if (osound != null) {
                        osound.stop();
                    }
                }
            }
            if ((wasPlaying || _started.value && _config.loop) && !isPlaying()) {
                start();
            }
        }

        /** The implementation configuration. */
        protected SounderConfig.MetaClip _config;

        /** The sounds. */
        protected Sound[] _sounds;

        /** The weights. */
        protected float[] _weights;
    }

    /**
     * Plays a clip with variable parameters.
     */
    public static class VariableClip extends BaseClip
    {
        /**
         * Creates a new clip implementation.
         */
        public VariableClip (AlContext ctx, Scope parentScope, SounderConfig.VariableClip config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.VariableClip config)
        {
            super.setConfig(_config = config);
            updateFromConfig();
        }

        @Override
        public boolean loops ()
        {
            return _config.loop;
        }

        @Override
        public void start ()
        {
            float gain = _config.gain.getValue();
            if (_sound != null) {
                _sound.setGain(gain);
                _sound.setPitch(_config.pitch.getValue());
            }
            playSound(gain);
        }

        @Override
        protected void updateFromConfig ()
        {
            boolean wasPlaying = isPlaying();
            _sound = getSound(_config.file, _sound);
            if ((wasPlaying || _started.value && _config.loop) && !isPlaying()) {
                start();
            }
        }

        /** The implementation configuration. */
        protected SounderConfig.VariableClip _config;
    }

    /**
     * Base class for {@link Stream} and {@link MetaStream}.
     */
    public static abstract class BaseStream extends Implementation
    {
        /**
         * Creates a new implementation.
         */
        public BaseStream (AlContext ctx, Scope parentScope)
        {
            super(ctx, parentScope);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.BaseStream config)
        {
            _config = config;
        }

        @Override
        public void stop ()
        {
            stopStream(_config.fadeOut);
        }

        @Override
        public boolean isPlaying ()
        {
            return _stream != null && _stream.isPlaying();
        }

        /**
         * Starts the specified stream.
         */
        protected void startStream (StackedStream stream, float fadeIn)
        {
            stopStream(fadeIn);
            (_stream = stream).setGain(_config.gain * _streamGain.value);

            // configure the stream source
            Source source = _stream.getSource();
            source.setSourceRelative(_config.sourceRelative);
            source.setMinGain(_config.minGain);
            source.setMaxGain(_config.maxGain);
            source.setReferenceDistance(_config.referenceDistance);
            source.setRolloffFactor(_config.rolloffFactor);
            source.setMaxDistance(_config.maxDistance);
            source.setPitch(_config.pitch);
            source.setConeInnerAngle(_config.coneInnerAngle);
            source.setConeOuterAngle(_config.coneOuterAngle);
            source.setConeOuterGain(_config.coneOuterGain);

            // start playing
            _stream.push(fadeIn, !_config.push);
        }

        /**
         * Stops the current stream, if any.
         */
        protected void stopStream (float fadeOut)
        {
            if (_stream == null) {
                return;
            }
            _stream.pop(fadeOut);
            _stream = null;
        }

        /**
         * Updates the transform of the stream as it plays.
         */
        protected class TransformedStream extends StackedStream
        {
            /**
             * Creates a new transformed stream.
             */
            public TransformedStream (String file, boolean loop, String stack)
                throws IOException
            {
                super(_ctx.getSoundManager(), file, loop, stack);
            }

            @Override
            protected void update (float time)
            {
                setGain(getCombinedGain());
                super.update(time);
                if (_state == AL10.AL_PLAYING) {
                    updateSoundTransform();
                }
            }

            /**
             * Returns the combined gain.
             */
            protected float getCombinedGain ()
            {
                float base = _config.gain * _streamGain.value;
                if (!_config.attenuate) {
                    return base;
                }
                // the attenuation formula is the Inverse Distance Clamped Model described in
                // the OpenAL 1.1 specification (section 3.4.2)
                Listener listener = _ctx.getSoundManager().getListener();
                _transform.update(Transform3D.RIGID);
                Vector3f translation = _transform.getTranslation();
                float dx = listener.getPositionX() - translation.x;
                float dy = listener.getPositionY() - translation.y;
                float dz = listener.getPositionZ() - translation.z;
                float ref = _config.referenceDistance;
                float dist = FloatMath.clamp(FloatMath.sqrt(dx*dx + dy*dy + dz*dz),
                    ref, _config.maxDistance);
                return base * ref / (ref + _config.rolloffFactor * (dist - ref));
            }

            /**
             * Updates the stream's source transform.
             */
            protected void updateSoundTransform ()
            {
                _transform.update(Transform3D.RIGID);
                Vector3f translation = _transform.getTranslation();
                _source.setPosition(translation.x, translation.y, translation.z);
                if (_config.directional) {
                    _transform.getRotation().transformUnitX(_direction);
                    _source.setDirection(_direction.x, _direction.y, _direction.z);
                }
            }

            /** Holds the direction of the source for updates. */
            protected Vector3f _direction = new Vector3f();
        }

        /** The implementation configuration. */
        protected SounderConfig.BaseStream _config;

        /** The (current) stream. */
        protected StackedStream _stream;

        /** The stream gain. */
        @Bound
        protected MutableFloat _streamGain;
    }

    /**
     * Plays a sound stream.
     */
    public static class Stream extends BaseStream
    {
        /**
         * Creates a new stream implementation.
         */
        public Stream (AlContext ctx, Scope parentScope, SounderConfig.Stream config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.Stream config)
        {
            super.setConfig(_config = config);
            if (_started.value && config.loops() && !isPlaying()) {
                start();
            }
        }

        @Override
        public boolean loops ()
        {
            return _config.loops();
        }

        @Override
        public void start ()
        {
            if (!_ctx.getSoundManager().isInitialized()) {
                return;
            }
            QueuedFile[] queue = _config.queue;
            QueuedFile first = queue[0];
            try {
                StackedStream stream = new TransformedStream(first.file, first.loop, _config.stack);
                ResourceManager rsrcmgr = _ctx.getResourceManager();
                for (int ii = 1; ii < queue.length; ii++) {
                    QueuedFile queued = queue[ii];
                    if (queued.file != null) {
                        stream.queueResource(queued.file, queued.loop);
                    }
                }
                startStream(stream, _config.fadeIn);

            } catch (IOException e) {
                log.warning("Error opening stream.", "file", first.file, e);
            }
        }

        /** The implementation configuration. */
        protected SounderConfig.Stream _config;
    }

    /**
     * Selects from a number of streams.
     */
    public static class MetaStream extends BaseStream
    {
        /**
         * Creates a new stream implementation.
         */
        public MetaStream (AlContext ctx, Scope parentScope, SounderConfig.MetaStream config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.MetaStream config)
        {
            super.setConfig(_config = config);
            _weights = new float[config.files.length];
            for (int ii = 0; ii < _weights.length; ii++) {
                _weights[ii] = config.files[ii].weight;
            }
            if (_started.value && !isPlaying()) {
                start();
            }
        }

        @Override
        public boolean loops ()
        {
            return true;
        }

        @Override
        public void start ()
        {
            if (_ctx.getSoundManager().isInitialized()) {
                startNextStream(_config.fadeIn);
            }
        }

        /**
         * Plays the next stream.
         */
        protected void startNextStream (float fadeIn)
        {
            int idx = RandomUtil.getWeightedIndex(_weights);
            if (idx == -1) {
                return;
            }
            WeightedFile wfile = _config.files[idx];
            if (wfile.file == null) {
                return;
            }
            try {
                startStream(createStream(wfile), fadeIn);
            } catch (IOException e) {
                log.warning("Error opening stream.", "file", wfile.file, e);
            }
        }

        /**
         * Creates a stream to play the specified file.
         */
        protected StackedStream createStream (final WeightedFile wfile)
            throws IOException
        {
            return new TransformedStream(wfile.file, false, _config.stack) {
                @Override protected void update (float time) {
                    super.update(time);
                    if (_remaining == Float.MAX_VALUE || _transitioning ||
                            _fadeMode == FadeMode.OUT_DISPOSE) {
                        return;
                    }
                    if ((_remaining -= time) <= _config.crossFade) {
                        startNextStream(Math.max(_remaining, 0f));
                        _transitioning = true;
                    }
                }
                @Override protected float getCombinedGain () {
                    return super.getCombinedGain() * wfile.gain;
                }
                @Override protected int populateBuffer (ByteBuffer buf) throws IOException {
                    int read = super.populateBuffer(buf);
                    if (read < buf.capacity() && _remaining == Float.MAX_VALUE) {
                        // compute the amount of time remaining
                        int bytes = _qlen*getBufferSize() -
                            (_source.isPlaying() ? _source.getByteOffset() : 0) +
                                Math.max(read, 0);
                        int samples = bytes / (getFormat() == AL10.AL_FORMAT_MONO16 ? 2 : 4);
                        _remaining = (float)samples / getFrequency();
                    }
                    return read;
                }
                protected float _remaining = Float.MAX_VALUE;
                protected boolean _transitioning;
            };
        }

        /** The implementation configuration. */
        protected SounderConfig.MetaStream _config;

        /** The weights of the streams. */
        protected float[] _weights;
    }

    /**
     * Plays the first sounder whose condition evaluates to true.
     */
    public static class Conditional extends Implementation
    {
        /**
         * Creates a new conditional implementation.
         */
        public Conditional (AlContext ctx, Scope parentScope, SounderConfig.Conditional config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.Conditional config)
        {
            _config = config;
            updateFromConfig();
        }

        @Override
        public boolean loops ()
        {
            for (Sounder sounder : _sounders) {
                if (sounder.loops()) {
                    return true;
                }
            }
            return _defaultSounder.loops();
        }

        @Override
        public void start ()
        {
            for (int ii = 0; ii < _evaluators.length; ii++) {
                if (_evaluators[ii].evaluate()) {
                    (_sounder = _sounders[ii]).start();
                    return;
                }
            }
            (_sounder = _defaultSounder).start();
        }

        @Override
        public void stop ()
        {
            if (_sounder != null) {
                _sounder.stop();
            }
        }

        @Override
        public boolean isPlaying ()
        {
            return _sounder != null && _sounder.isPlaying();
        }

        @Override
        public void update ()
        {
            if (_sounder != null) {
                _sounder.update();
            }
        }

        @Override
        public void scopeUpdated (ScopeEvent event)
        {
            super.scopeUpdated(event);
            updateFromConfig();
        }

        /**
         * Updates the sounder from its configuration.
         */
        protected void updateFromConfig ()
        {
            boolean wasPlaying = isPlaying();

            // create the evaluators
            _evaluators = new BooleanExpression.Evaluator[_config.cases.length];
            for (int ii = 0; ii < _evaluators.length; ii++) {
                _evaluators[ii] = _config.cases[ii].condition.createEvaluator(this);
            }

            // create the case sounders
            Sounder[] osounders = _sounders;
            _sounders = new Sounder[_config.cases.length];
            for (int ii = 0; ii < _sounders.length; ii++) {
                Sounder sounder = (osounders == null || osounders.length <= ii) ?
                    new Sounder(_ctx, this, _transform) : osounders[ii];
                _sounders[ii] = sounder;
                sounder.setConfig(_config.cases[ii].sounder);
            }
            if (osounders != null) {
                for (int ii = _sounders.length; ii < osounders.length; ii++) {
                    osounders[ii].dispose();
                }
            }

            // create the default sounder
            if (_defaultSounder == null) {
                _defaultSounder = new Sounder(_ctx, this, _transform);
            }
            _defaultSounder.setConfig(_config.defaultSounder);

            // restart if appropriate
            if ((wasPlaying || _started.value && loops()) && !isPlaying()) {
                start();
            }
        }

        /** The sounder configuration. */
        protected SounderConfig.Conditional _config;

        /** The case evaluators. */
        protected BooleanExpression.Evaluator[] _evaluators;

        /** The case sounders. */
        protected Sounder[] _sounders;

        /** The default sounder. */
        protected Sounder _defaultSounder;

        /** The currently playing sounder, if any. */
        protected Sounder _sounder;
    }

    /**
     * Plays several sub-sounders at once.
     */
    public static class Compound extends Implementation
    {
        /**
         * Creates a new compound implementation.
         */
        public Compound (AlContext ctx, Scope parentScope, SounderConfig.Compound config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.Compound config)
        {
            boolean wasPlaying = (_sounders != null && isPlaying());

            // create the component sounders
            Sounder[] osounders = _sounders;
            _sounders = new Sounder[config.sounders.length];
            for (int ii = 0; ii < _sounders.length; ii++) {
                Sounder sounder = (osounders == null || osounders.length <= ii) ?
                    new Sounder(_ctx, this, _transform) : osounders[ii];
                _sounders[ii] = sounder;
                sounder.setConfig(config.sounders[ii].sounder);
            }
            if (osounders != null) {
                for (int ii = _sounders.length; ii < osounders.length; ii++) {
                    osounders[ii].dispose();
                }
            }

            // restart if appropriate
            if ((wasPlaying || _started.value && loops()) && !isPlaying()) {
                start();
            }
        }

        @Override
        public boolean loops ()
        {
            for (Sounder sounder : _sounders) {
                if (sounder.loops()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start ()
        {
            for (Sounder sounder : _sounders) {
                sounder.start();
            }
        }

        @Override
        public void stop ()
        {
            for (Sounder sounder : _sounders) {
                sounder.stop();
            }
        }

        @Override
        public boolean isPlaying ()
        {
            for (Sounder sounder : _sounders) {
                if (sounder.isPlaying()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void update ()
        {
            for (Sounder sounder : _sounders) {
                sounder.update();
            }
        }

        /** The component sounders. */
        protected Sounder[] _sounders;
    }

    /**
     * Plays multiple sub-sounders in sequence.
     */
    public static class Sequential extends Implementation
    {
        /**
         * Creates a new compound implementation.
         */
        public Sequential (AlContext ctx, Scope parentScope, SounderConfig.Sequential config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.Sequential config)
        {
            boolean wasPlaying = (_sounders != null && isPlaying());
            _config = config;

            // create the component sounders
            Sounder[] osounders = _sounders;
            _sounders = new Sounder[config.sounders.length];
            for (int ii = 0; ii < _sounders.length; ii++) {
                Sounder sounder = (osounders == null || osounders.length <= ii) ?
                    new Sounder(_ctx, this, _transform) : osounders[ii];
                _sounders[ii] = sounder;
                sounder.setConfig(config.sounders[ii].sounder);
            }
            if (osounders != null) {
                for (int ii = _sounders.length; ii < osounders.length; ii++) {
                    osounders[ii].dispose();
                }
            }

            // restart if appropriate
            if ((wasPlaying || _started.value && loops()) && !isPlaying()) {
                start();
            }
        }

        @Override
        public boolean loops ()
        {
            if (_config.loop) {
                return true;
            }
            for (Sounder sounder : _sounders) {
                if (sounder.loops()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start ()
        {
            _sounders[_sidx = 0].start();
            _completed = false;
        }

        @Override
        public void stop ()
        {
            for (Sounder sounder : _sounders) {
                sounder.stop();
            }
        }

        @Override
        public boolean isPlaying ()
        {
            for (Sounder sounder : _sounders) {
                if (sounder.isPlaying()) {
                    return true;
                }
            }
            return _started.value && !_completed;
        }

        @Override
        public void update ()
        {
            if (_completed) {
                return;
            }
            _sounders[_sidx].update();
            if (_sounders[_sidx].isPlaying()) {
                return;
            }
            _sidx++;

            // check for loop or completion
            if (_sidx >= _sounders.length) {
                if (_config.loop) {
                    _sidx %= _sounders.length;
                } else {
                    _sidx = _sidx - 1;
                    _completed = true;
                    return;
                }
            }
            _sounders[_sidx].start();
        }

        /** The implementation config. */
        protected SounderConfig.Sequential _config;

        /** The component sounders. */
        protected Sounder[] _sounders;

        /** The index of the current sounder. */
        protected int _sidx;

        /** If true, the script has completed. */
        protected boolean _completed;
    }

    /**
     * Plays a scripted sequence of sounders.
     */
    public static class Scripted extends Implementation
    {
        /**
         * Creates a new scripted implementation.
         */
        public Scripted (AlContext ctx, Scope parentScope, SounderConfig.Scripted config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.Scripted config)
        {
            boolean wasPlaying = (_sounders != null && isPlaying());
            _config = config;

            // create the component sounders
            Sounder[] osounders = _sounders;
            _sounders = new Sounder[config.sounders.length];
            _times = new float[_sounders.length];
            for (int ii = 0; ii < _sounders.length; ii++) {
                Sounder sounder = (osounders == null || osounders.length <= ii) ?
                    new Sounder(_ctx, this, _transform) : osounders[ii];
                _sounders[ii] = sounder;
                SounderConfig.TimedSounder tsounder = config.sounders[ii];
                sounder.setConfig(tsounder.sounder);
                _times[ii] = tsounder.time;
            }
            if (osounders != null) {
                for (int ii = _sounders.length; ii < osounders.length; ii++) {
                    osounders[ii].dispose();
                }
            }

            // restart if appropriate
            if ((wasPlaying || _started.value && loops()) && !isPlaying()) {
                start();
            }
        }

        @Override
        public boolean loops ()
        {
            return _config.loopDuration > 0f;
        }

        @Override
        public void start ()
        {
            _time = 0f;
            _sidx = 0;
            _last = _now.value;
            _completed = false;
        }

        @Override
        public void stop ()
        {
            for (Sounder sounder : _sounders) {
                sounder.stop();
            }
        }

        @Override
        public boolean isPlaying ()
        {
            for (Sounder sounder : _sounders) {
                if (sounder.isPlaying()) {
                    return true;
                }
            }
            return _started.value && !_completed;
        }

        @Override
        public void update ()
        {
            if (_completed) {
                return;
            }
            float elapsed = (_now.value - _last) / 1000f;
            _last = _now.value;
            _time += elapsed;
            startSounders();

            // check for loop or completion
            if (_config.loopDuration > 0f) {
                if (_time >= _config.loopDuration) {
                    _time %= _config.loopDuration;
                    _sidx = 0;
                    startSounders();
                }
            } else if (_sidx >= _sounders.length) {
                _completed = true;
            }
        }

        /**
         * Starts all sounders scheduled before or at the current time.
         */
        protected void startSounders ()
        {
            for (; _sidx < _sounders.length && _times[_sidx] < _time; _sidx++) {
                _sounders[_sidx].start();
            }
        }

        /** The implementation config. */
        protected SounderConfig.Scripted _config;

        /** The component sounders. */
        protected Sounder[] _sounders;

        /** The times of the sounders. */
        protected float[] _times;

        /** The amount of time elapsed. */
        protected float _time;

        /** The index of the current sounder. */
        protected int _sidx;

        /** The time of the last update. */
        protected long _last;

        /** If true, the script has completed. */
        protected boolean _completed;

        /** The current time. */
        @Bound
        protected MutableLong _now;
    }

    /**
     * Plays the first sounder whose condition evaluates to true.
     */
    public static class Random extends Implementation
    {
        /**
         * Creates a new conditional implementation.
         */
        public Random (AlContext ctx, Scope parentScope, SounderConfig.Random config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SounderConfig.Random config)
        {
            boolean wasPlaying = isPlaying();

            // create the component sounders
            Sounder[] osounders = _sounders;
            _sounders = new Sounder[config.sounders.length];
            _weights = new float[_sounders.length];
            for (int ii = 0; ii < _sounders.length; ii++) {
                Sounder sounder = (osounders == null || osounders.length <= ii) ?
                    new Sounder(_ctx, this, _transform) : osounders[ii];
                _sounders[ii] = sounder;
                SounderConfig.WeightedSounder wsounder = config.sounders[ii];
                sounder.setConfig(wsounder.sounder);
                _weights[ii] = wsounder.weight;
            }
            if (osounders != null) {
                for (int ii = _sounders.length; ii < osounders.length; ii++) {
                    osounders[ii].dispose();
                }
            }

            // restart if appropriate
            if ((wasPlaying || _started.value && loops()) && !isPlaying()) {
                start();
            }
        }

        @Override
        public boolean loops ()
        {
            for (Sounder sounder : _sounders) {
                if (sounder.loops()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start ()
        {
            (_sounder = _sounders[RandomUtil.getWeightedIndex(_weights)]).start();
        }

        @Override
        public void stop ()
        {
            if (_sounder != null) {
                _sounder.stop();
            }
        }

        @Override
        public boolean isPlaying ()
        {
            return _sounder != null && _sounder.isPlaying();
        }

        @Override
        public void update ()
        {
            if (_sounder != null) {
                _sounder.update();
            }
        }

        /** The sounder configuration. */
        protected SounderConfig.Conditional _config;

        /** The component sounders. */
        protected Sounder[] _sounders;

        /** The weights of the sounders. */
        protected float[] _weights;

        /** The currently playing sounder, if any. */
        protected Sounder _sounder;
    }

    /**
     * Creates a new sounder with a null configuration.
     *
     * @param transform a reference to the sound transform to use.
     */
    public Sounder (AlContext ctx, Scope parentScope, Transform3D transform)
    {
        this(ctx, parentScope, transform, (SounderConfig)null);
    }

    /**
     * Creates a new sounder with the referenced configuration.
     *
     * @param transform a reference to the sound transform to use.
     */
    public Sounder (
        AlContext ctx, Scope parentScope, Transform3D transform,
        ConfigReference<SounderConfig> ref)
    {
        this(ctx, parentScope, transform,
            ctx.getConfigManager().getConfig(SounderConfig.class, ref));
    }

    /**
     * Creates a new sounder with the given configuration.
     *
     * @param transform a reference to the sound transform to use.
     */
    public Sounder (AlContext ctx, Scope parentScope, Transform3D transform, SounderConfig config)
    {
        super(parentScope);
        _ctx = ctx;
        _transform = transform;
        setConfig(config);
    }

    /**
     * Sets the configuration of this sounder.
     */
    public void setConfig (ConfigReference<SounderConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(SounderConfig.class, ref));
    }

    /**
     * Sets the configuration of this sounder.
     */
    public void setConfig (SounderConfig config)
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
     * Checks whether the configured sound loops.
     */
    public boolean loops ()
    {
        return _impl.loops();
    }

    /**
     * Starts playing the sound.
     */
    public void start ()
    {
        resetEpoch();
        _started.value = true;
        _impl.start();
    }

    /**
     * Stops playing the sound.
     */
    public void stop ()
    {
        _started.value = false;
        _impl.stop();
    }

    /**
     * Checks whether the sound is currently playing.
     */
    public boolean isPlaying ()
    {
        return _impl.isPlaying();
    }

    /**
     * Updates the sound for the current frame.  Invocation of this method is not guaranteed;
     * in particular, while {@link com.threerings.opengl.scene.config.ViewerEffectConfig.Sound}
     * calls this method, {@link com.threerings.opengl.model.config.ActionConfig.PlaySound}
     * does not.
     */
    public void update ()
    {
        _impl.update();
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<SounderConfig> event)
    {
        updateFromConfig();
    }

    @Override
    public String getScopeName ()
    {
        return "sounder";
    }

    @Override
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        resetEpoch();
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
     * Updates the sounder to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getSounderImplementation(_ctx, this, _impl);
        nimpl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
        if (_impl != nimpl) {
            if (_impl.isPlaying()) {
                _impl.stop();
                nimpl.start();
            }
            _impl.dispose();
            _impl = nimpl;
        }
    }

    /**
     * Resets the epoch value to the current time.
     */
    protected void resetEpoch ()
    {
        _epoch.value = _now.value;
    }

    /** The application context. */
    protected AlContext _ctx;

    /** The sound transform reference. */
    @Scoped
    protected Transform3D _transform;

    /** The configuration of this sounder. */
    protected SounderConfig _config;

    /** The sounder implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** The container for the current time. */
    @Bound
    protected MutableLong _now;

    /** A container for the sound epoch. */
    @Scoped
    protected MutableLong _epoch;

    { // Initialize now/epoch against the same value.
        long now = System.currentTimeMillis();
        _now = new MutableLong(now);
        _epoch = new MutableLong(now);
    }

    /** Whether or not the sound has been started. */
    @Scoped
    protected MutableBoolean _started = new MutableBoolean();

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null, null) {
        public void start () { }
        public void stop () { }
        public boolean isPlaying () { return false; }
    };
}
