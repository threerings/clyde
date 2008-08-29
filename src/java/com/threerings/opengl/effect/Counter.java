//
// $Id$

package com.threerings.opengl.effect;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.probs.FloatVariable;
import com.threerings.util.DeepObject;

import com.threerings.math.FloatMath;

/**
 * Determines how many particles to emit at each frame.
 */
@EditorTypes({
    Counter.Unlimited.class, Counter.ConstantRate.class,
    Counter.RandomIntervals.class })
public abstract class Counter extends DeepObject
    implements Exportable
{
    /**
     * Always releases the maximum number of particles.
     */
    public static class Unlimited extends Counter
    {
        @Override // documentation inherited
        public int count (float elapsed, int maximum)
        {
            return maximum;
        }
    }

    /**
     * Releases particles at a constant rate.
     */
    public static class ConstantRate extends Counter
    {
        /** The number of particles to release every second. */
        @Editable(min=0.0, step=0.1)
        public float rate = 10f;

        @Override // documentation inherited
        public int count (float elapsed, int maximum)
        {
            int count = (int)(_accum += elapsed * rate);
            _accum -= count;
            return Math.min(count, maximum);
        }

        @Override // documentation inherited
        public void reset ()
        {
            _accum = 0f;
        }

        /** The number of fractional particles accumulated. */
        protected transient float _accum;
    }

    /**
     * Superclass for counters that determine the number of particles to release by generating
     * sequences of interarrival times (intervals).
     */
    public static class RandomIntervals extends Counter
    {
        /** The interval variable. */
        @Editable(min=0.0, step=0.01)
        public FloatVariable interval = new FloatVariable.Uniform(0.07f, 0.2f);

        @Override // documentation inherited
        public int count (float elapsed, int maximum)
        {
            if ((_remaining -= elapsed) > 0f) {
                return 0;
            }
            int count = Math.min(1, maximum);
            while (count < maximum && (_remaining += getInterval()) < 0f) {
                count++;
            }
            return count;
        }

        @Override // documentation inherited
        public void reset ()
        {
            _remaining = getInterval();
        }

        /**
         * Returns a random interval according to our distribution.
         */
        protected float getInterval ()
        {
            return Math.max(0f, interval.getValue());
        }

        /** The amount of time remaining until we're to fire the next particle. */
        protected transient float _remaining;
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] { Unlimited.class, ConstantRate.class, RandomIntervals.class };
    }

    /**
     * Returns the number of particles to release at this frame.
     */
    public abstract int count (float elapsed, int maximum);

    /**
     * Resets the counter to its initial state.
     */
    public void reset ()
    {
        // nothing by default
    }
}
