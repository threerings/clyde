//
// $Id$

package com.threerings.opengl.effect.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.probs.FloatVariable;
import com.threerings.util.DeepObject;

/**
 * Determines how many particles to emit at each frame.
 */
@EditorTypes({
    CounterConfig.Unlimited.class, CounterConfig.ConstantRate.class,
    CounterConfig.RandomIntervals.class })
public abstract class CounterConfig extends DeepObject
    implements Exportable
{
    /**
     * Always releases the maximum number of particles.
     */
    public static class Unlimited extends CounterConfig
    {
    }

    /**
     * Releases particles at a constant rate.
     */
    public static class ConstantRate extends CounterConfig
    {
        /** The number of particles to release every second. */
        @Editable(min=0.0, step=0.1)
        public float rate = 10f;
    }

    /**
     * Superclass for counters that determine the number of particles to release by generating
     * sequences of interarrival times (intervals).
     */
    public static class RandomIntervals extends CounterConfig
    {
        /** The interval variable. */
        @Editable(min=0.0, step=0.01)
        public FloatVariable interval = new FloatVariable.Uniform(0.07f, 0.2f);
    }
}
