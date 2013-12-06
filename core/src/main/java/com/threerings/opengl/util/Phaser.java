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

package com.threerings.opengl.util;

import java.lang.reflect.Method;

import static com.threerings.opengl.Log.log;

/**
 * Provides a convenient way to effect updates that proceed through a set of discrete phases (for
 * example: fade in, linger, fade out).  Updates are always generated for phase end boundaries.
 * For example, if there are two phases (0.5, 0.5) and 10.0 seconds elapse, one update is generated
 * for the end of the first phase and one is generated for the end of the second phase.
 */
public abstract class Phaser
    implements Tickable
{
    /**
     * Creates a new phaser.
     *
     * @param params the names of the update methods to call for each phase and their corresponding
     * phase durations (example: <code>"fadeIn", 1f, null, 2f, "fadeOut", 1f</code>).  Update
     * methods should be member methods with zero arguments.
     */
    public Phaser (Object... params)
    {
        int nparams = params.length;
        if (nparams % 2 != 0) {
            throw new IllegalArgumentException("Number of parameters must be even.");
        }
        int nphases = nparams / 2;
        _methods = new Method[nphases];
        _durations = new float[nphases];
        for (int ii = 0; ii < nphases; ii++) {
            try {
                String mname = (String)params[ii*2];
                if (mname != null) {
                    _methods[ii] = getClass().getDeclaredMethod(mname);
                    _methods[ii].setAccessible(true);
                }
                _durations[ii] = (Float)params[ii*2 + 1];

            } catch (Exception e) { // ClassCastException, NoSuchMethodException
                throw new IllegalArgumentException("Invalid phase parameter.", e);
            }
        }
    }

    // documentation inherited from interface Tickable
    public void tick (float elapsed)
    {
        int nphases = _durations.length;
        if (_phase == nphases) {
            return; // completed
        }
        _accum += elapsed;
        while (_accum >= _durations[_phase]) {
            update(_phase, 1f);
            _accum -= _durations[_phase];
            if (++_phase == nphases) {
                completed();
                return;
            }
        }
        update(_phase, _accum / _durations[_phase]);
    }

    /**
     * Performs an update for the indexed phase.
     *
     * @param pct the percentage of the phase completed, from zero to one.
     */
    protected void update (int phase, float pct)
    {
        // update the percentage and its complement
        _pct = pct;
        _cpct = 1f - pct;

        // invoke the phase's update method, if any
        Method method = _methods[phase];
        if (method != null) {
            try {
                method.invoke(this);
            } catch (Exception e) {
                log.warning("Error invoking method.", "method", method.getName(), e);
            }
        }
    }

    /**
     * Called when the last phase is completed.
     */
    protected void completed ()
    {
        // nothing by default
    }

    /** The methods for each phase. */
    protected Method[] _methods;

    /** The durations of each phase. */
    protected float[] _durations;

    /** The index of the current phase. */
    protected int _phase;

    /** The time accumulated towards the next phase. */
    protected float _accum;

    /** The percentage of the current phase completed, from zero to one. */
    protected float _pct;

    /** The complement of the percentage completed (<code>1f - _pct</code>). */
    protected float _cpct;
}
