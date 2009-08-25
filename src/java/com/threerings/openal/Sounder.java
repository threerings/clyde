//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import java.io.File;
import java.io.IOException;

import java.nio.ByteBuffer;

import org.lwjgl.openal.AL10;

import com.samskivert.util.RandomUtil;

import com.threerings.resource.ResourceManager;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.MutableFloat;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.openal.SoundGroup;
import com.threerings.openal.Source;
import com.threerings.openal.config.SounderConfig;
import com.threerings.openal.config.SounderConfig.QueuedFile;
import com.threerings.openal.config.SounderConfig.WeightedFile;
import com.threerings.openal.util.AlContext;

import static com.threerings.openal.Log.*;

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
         * Starts playing the sound.
         */
        public abstract void start ();

        /**
         * Stops the sound.
         */
        public abstract void stop ();

        /**
         * Updates the sound.
         */
        public void update ()
        {
            // nothing by default
        }

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "impl";
        }

        /**
         * (Re)configures the implementation.
         */
        protected void setConfig (SounderConfig.Original config)
        {
            _config = config;
        }

        /** The application context. */
        protected AlContext _ctx;

        /** The implementation configuration. */
        protected SounderConfig.Original _config;

        /** The sound transform. */
        @Bound
        protected Transform3D _transform;
    }

    /**
     * Plays a sound clip.
     */
    public static class Clip extends Implementation
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

            // resolve the group and use it to obtain a sound reference
            SoundGroup group = ScopeUtil.resolve(
                _parentScope, "soundGroup", null, SoundGroup.class);
            _sound = (group == null) ? null : group.getSound(config.file);
            if (_sound == null) {
                return;
            }

            // configure the sound
            _sound.setGain(config.gain);
            _sound.setSourceRelative(config.sourceRelative);
            _sound.setMinGain(config.minGain);
            _sound.setMaxGain(config.maxGain);
            _sound.setReferenceDistance(config.referenceDistance);
            _sound.setRolloffFactor(config.rolloffFactor);
            _sound.setMaxDistance(config.maxDistance);
            _sound.setPitch(config.pitch);
            _sound.setConeInnerAngle(config.coneInnerAngle);
            _sound.setConeOuterAngle(config.coneOuterAngle);
            _sound.setConeOuterGain(config.coneOuterGain);
        }

        @Override // documentation inherited
        public void start ()
        {
            if (_sound != null) {
                updateSoundTransform();
                SoundClipManager clipmgr = ScopeUtil.resolve(
                    _parentScope, "clipmgr", null, SoundClipManager.class);
                if (clipmgr != null) {
                    clipmgr.playSound(_sound, _config);
                } else {
                    _sound.play(null, _config.loop);
                }
            }
        }

        @Override // documentation inherited
        public void stop ()
        {
            if (_sound != null) {
                _sound.stop();
            }
        }

        @Override // documentation inherited
        public void update ()
        {
            if (_sound != null) {
                updateSoundTransform();
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

        /** The implementation configuration. */
        protected SounderConfig.Clip _config;

        /** The sound. */
        protected Sound _sound;

        /** A result vector for computation. */
        protected Vector3f _vector = new Vector3f();
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
            super.setConfig(_config = config);
        }

        @Override // documentation inherited
        public void stop ()
        {
            stopStream(_config.fadeOut);
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
         * A stream that is part of a stack where only the top level of the stack is playing.
         */
        protected class StackedStream extends FileStream
        {
            /**
             * Creates a new stacked stream.
             */
            public StackedStream (String file, boolean loop, String stack)
                throws IOException
            {
                super(_ctx.getSoundManager(), _ctx.getResourceManager().getResourceFile(file),
                        loop);
                _stack = stack;
            }

            /**
             * Returns the name of the stack.
             */
            public String getStack ()
            {
                return _stack;
            }

            /**
             * Returns the level of the stack.
             */
            public int getLevel ()
            {
                return _level;
            }

            /**
             * Pushes the stream onto the stack, stopping any streams on top of the stack.
             */
            public void push (float interval, boolean current)
            {
                if (_level > -1) {
                    log.warning("Can't push stream already on the stack", "level", _level);
                    return;
                }
                int top = getTop();
                if (top > -1 && !current) {
                    for (com.threerings.openal.Stream stream : _soundmgr.getStreams()) {
                        if (sameStack(stream) && ((StackedStream)stream).getLevel() == top) {
                            if (interval > 0f) {
                                stream.fadeOut(interval, false);
                            } else {
                                stream.stop();
                            }
                        }
                    }
                }
                _level = top;
                if (top == -1 || !current) {
                    _level++;
                }
                if (interval > 0f) {
                    fadeIn(interval);
                } else {
                    play();
                }
            }

            /**
             * Pops the stream off the stack, resuming any streams that were below it.
             */
            public void pop (float interval)
            {
                if (_level == -1) {
                    log.warning("Can't pop stream not on the stack");
                    return;
                }
                if (interval > 0f) {
                    fadeOut(interval, true);
                } else {
                    dispose();
                }
                int oldLevel = _level;
                _level = -1;
                int top = getTop();
                if (top > -1 && top < oldLevel) {
                    for (com.threerings.openal.Stream stream : _soundmgr.getStreams()) {
                        if (sameStack(stream) && ((StackedStream)stream).getLevel() == top) {
                            if (interval > 0f) {
                                stream.fadeIn(interval);
                            } else {
                                stream.play();
                            }
                        }
                    }
                }
            }

            /**
             * Returns true if this stream is on the same stack.
             */
            protected boolean sameStack (com.threerings.openal.Stream other)
            {
                if (other instanceof StackedStream) {
                    String stack = ((StackedStream)other).getStack();
                    return _stack == null ? stack == null : _stack.equals(stack);
                }
                return false;
            }

            /**
             * Returns the top level of the stack.
             */
            protected int getTop ()
            {
                int top = -1;
                for (com.threerings.openal.Stream stream : _soundmgr.getStreams()) {
                    if (sameStack(stream)) {
                        top = Math.max(top, ((StackedStream)stream).getLevel());
                    }
                }
                return top;
            }

            /** The stack this stream is on. */
            protected String _stack;

            /** The level of the stack this stream is on. */
            protected int _level = -1;
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
                super(file, loop, stack);
            }

            @Override // documentation inherited
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
                return _config.gain * _streamGain.value;
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
        }

        @Override // documentation inherited
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
                        stream.queueFile(rsrcmgr.getResourceFile(queued.file), queued.loop);
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
        }

        @Override // documentation inherited
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
                    if (_remaining == Float.MAX_VALUE || _transitioning) {
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
                @Override protected int populateBuffer (ByteBuffer buf)
                    throws IOException
                {
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
     * Starts playing the sound.
     */
    public void start ()
    {
        resetEpoch();
        _impl.start();
    }

    /**
     * Stops playing the sound.
     */
    public void stop ()
    {
        _impl.stop();
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

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "sounder";
    }

    @Override // documentation inherited
    public void scopeUpdated (ScopeEvent event)
    {
        super.scopeUpdated(event);
        resetEpoch();
    }

    @Override // documentation inherited
    public void dispose ()
    {
        super.dispose();
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
    protected MutableLong _now = new MutableLong(System.currentTimeMillis());

    /** A container for the sound epoch. */
    @Scoped
    protected MutableLong _epoch = new MutableLong(System.currentTimeMillis());

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null, null) {
        public void start () { }
        public void stop () { }
    };
}
