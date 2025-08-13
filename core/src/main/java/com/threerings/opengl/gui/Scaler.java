//
//

package com.threerings.opengl.gui;

import java.util.List;

import com.google.common.collect.Lists;

import static com.threerings.opengl.Log.log;

/**
 * An observeable value for tracking scale.
 */
public interface Scaler
{
    /**
     * Observes scale changes.
     */
    public interface Observer {
        /**
         * React to scale changing.
         */
        public void scaleUpdated (Scaler scaler);
    }

    /**
     * Get the current scale.
     */
    public float getScale ();

    /**
     * Add a scale Observer.
     */
    public void addObserver (Observer obs);

    /**
     * Remove a scale Observer.
     */
    public void removeObserver (Observer obs);

    /**
     * A simple Scaler that supports adjusting the scale.
     */
    public static class MutableScaler
        implements Scaler
    {
        public MutableScaler ()
        {
            this(1f);
        }

        public MutableScaler (float scale)
        {
            setScale(scale);
        }

        /**
         * Set the scale.
         */
        public void setScale (float scale)
        {
            if (_scale != scale) {
                _scale = scale;
                for (Observer obs : _observers) obs.scaleUpdated(this);
            }
        }

        // from Scaler
        public float getScale ()
        {
            return _scale;
        }

        // from Scaler
        public void addObserver (Observer obs)
        {
            _observers.add(obs);
        }

        // from Scaler
        public void removeObserver (Observer obs)
        {
            _observers.remove(obs);
        }

        protected float _scale;

        protected List<Observer> _observers = Lists.newArrayList();
    }

    /** A shared mutable scaler that is accessible to anyone. */
    public static final MutableScaler GLOBAL_SHARED_SCALER = new MutableScaler();
}
