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

package com.threerings.opengl.effect.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.probs.FloatVariable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.effect.Counter;

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
        @Override
        public Counter createCounter ()
        {
            return new Counter() {
                public int count (float elapsed, int maximum) {
                    return maximum;
                }
                public void reset () {
                    // no-op
                }
            };
        }
    }

    /**
     * Releases particles at a constant rate.
     */
    public static class ConstantRate extends CounterConfig
    {
        /** The number of particles to release every second. */
        @Editable(min=0.0, step=0.1)
        public float rate = 10f;

        @Override
        public Counter createCounter ()
        {
            return new Counter() {
                public int count (float elapsed, int maximum) {
                    int count = (int)(_accum += elapsed * rate);
                    _accum -= count;
                    return Math.min(count, maximum);
                }
                public void reset () {
                    _accum = 0f;
                }
                protected float _accum;
            };
        }
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

        @Override
        public Counter createCounter ()
        {
            return new Counter() {
                public int count (float elapsed, int maximum) {
                    if ((_remaining -= elapsed) > 0f) {
                        return 0;
                    }
                    int count = Math.min(1, maximum);
                    while (count < maximum && (_remaining += getInterval()) < 0f) {
                        count++;
                    }
                    return count;
                }
                public void reset () {
                    _remaining = getInterval();
                }
                protected float getInterval () {
                    return Math.max(0f, interval.getValue());
                }
                protected float _remaining;
            };
        }
    }

    /**
     * Creates the counter corresponding to this config.
     */
    public abstract Counter createCounter ();
}
