//
// $Id$

package com.threerings.openal;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.MutableLong;
import com.threerings.expr.Scope;
import com.threerings.expr.Scoped;
import com.threerings.expr.ScopeEvent;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Transform3D;

import com.threerings.openal.SoundGroup;
import com.threerings.openal.config.SounderConfig;
import com.threerings.openal.util.AlContext;

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
         * Stops the animation.
         */
        public abstract void stop ();

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
        }

        @Override // documentation inherited
        public void start ()
        {
            if (_sound != null) {
                _sound.play(null, _config.loop);
            }
        }

        @Override // documentation inherited
        public void stop ()
        {
            if (_sound != null) {
                _sound.stop();
            }
        }

        /** The implementation configuration. */
        protected SounderConfig.Clip _config;

        /** The sound. */
        protected Sound _sound;
    }

    /**
     * Plays a sound stream.
     */
    public static class Stream extends Implementation
    {
        /**
         * Creates a new clip implementation.
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
            if (_stream != null) {
                _stream.dispose();
            }
            _stream = _config.createStream(_ctx);
            if (_stream != null) {
                _stream.play();
            }
        }

        @Override // documentation inherited
        public void stop ()
        {
            if (_stream != null) {
                _stream.dispose();
                _stream = null;
            }
        }

        /** The implementation configuration. */
        protected SounderConfig.Stream _config;

        /** The stream. */
        protected FileStream _stream;
    }

    /**
     * Creates a new sounder with a null configuration.
     */
    public Sounder (AlContext ctx, Scope parentScope)
    {
        this(ctx, parentScope, (SounderConfig)null);
    }

    /**
     * Creates a new sounder with the referenced configuration.
     */
    public Sounder (AlContext ctx, Scope parentScope, ConfigReference<SounderConfig> ref)
    {
        this(ctx, parentScope, ctx.getConfigManager().getConfig(SounderConfig.class, ref));
    }

    /**
     * Creates a new sounder with the given configuration.
     */
    public Sounder (AlContext ctx, Scope parentScope, SounderConfig config)
    {
        super(parentScope);
        _ctx = ctx;
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
        _impl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
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
