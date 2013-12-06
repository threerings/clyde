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

import java.util.ArrayList;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import com.threerings.openal.ClipBuffer;
import com.threerings.openal.util.AlContext;

import static com.threerings.ClydeLog.log;

public class SoundClipManager
{
    /**
     * Creates a sound clip manager.
     */
    public SoundClipManager (AlContext ctx)
    {
        _ctx = ctx;
    }

    /**
     * Registers and plays a sound using the clip manager.
     */
    public void playSound (Sound sound, final float gain)
    {
        ClipBuffer buffer = sound.getBuffer();
        if (buffer == null) {
            return;
        }
        final String path = buffer.getPath();
        int count = _counts.count(path);
        boolean canStop = false;
        if (sound.isPlaying() || count > 0) {
            for (int ii = _sounds.size() - 1; ii >= 0; ii--) {
                SoundEntry entry = _sounds.get(ii);
                if (sound.isPlaying()) {
                    if (entry.sound == sound) {
                        _sounds.remove(ii);
                        _counts.remove(path);
                        count--;
                        log.debug("ClipManager replaying sound", "path", path);
                        break;
                    }
                } else if (entry.elapsed < MIN_GAP &&
                        entry.sound.getBuffer().getPath().equals(path)) {
                    log.debug("ClipManager prevent same sound", "path", path);
                    return;
                } else if (!canStop && entry.elapsed > MIN_STOP &&
                        entry.sound.getBuffer().getPath().equals(path)) {
                    canStop = true;
                }
            }
        }
        if (count + 1 >= GAIN_LEVEL.length) {
            if (!canStop) {
                log.debug("ClipManager prevent sound no popped", "path", path);
                return;
            }
            for (int ii = 0, nn = _sounds.size(); ii < nn; ii++) {
                SoundEntry entry = _sounds.get(ii);
                if (entry.sound.getBuffer().getPath().equals(sound.getBuffer().getPath())) {
                    _sounds.remove(ii);
                    _counts.remove(entry.sound.getBuffer().getPath());
                    log.debug("ClipManager sound popped", "path", path);
                    break;
                }
            }
        }
        _counts.add(path);
        sound.play(true, false, new Sound.StartObserver() {
            public void soundStarted (Sound sound) {
                if (sound == null) {
                    _counts.remove(path);
                    log.debug("Failed to start sound", "path", path);
                    return;
                }
                _sounds.add(new SoundEntry(sound, gain));
                int count = _counts.count(path);
                sound.setGain(gain * getGainModifier(count));
                log.debug("ClipManager play sound", "count", count, "path", path);
            }
            });

    }

    /**
     * Updates the sounds currently played.
     */
    public void tick (float elapsed)
    {
        for (int ii = _sounds.size() - 1; ii >= 0; ii--) {
            SoundEntry entry = _sounds.get(ii);
            if (!entry.sound.isPlaying()) {
                _sounds.remove(ii);
                _counts.remove(entry.sound.getBuffer().getPath());
                log.debug("ClipManager sound ended", "path", entry.sound.getBuffer().getPath());
            }
        }
        for (int ii = _sounds.size() - 1; ii >= 0; ii--) {
            SoundEntry entry = _sounds.get(ii);
            entry.elapsed += elapsed;
            int count = _counts.count(entry.sound.getBuffer().getPath());
            entry.sound.setGain(entry.gain * getGainModifier(count));
        }
    }

    /**
     * Returns the gain modifier
     */
    protected float getGainModifier (int count)
    {
        return _ctx.getSoundManager().getBaseGain() * GAIN_LEVEL[count - 1] / count;
    }

    /**
     * An entry for a sound and it's default gain.
     */
    protected class SoundEntry
    {
        /** The sound. */
        public Sound sound;

        /** The default gain level. */
        public float gain;

        /** The amount of time elapsed since the start. */
        public float elapsed;

        public SoundEntry (Sound sound, float gain)
        {
            this.sound = sound;
            this.gain = gain;
        }
    }

    /** The application context. */
    protected AlContext _ctx;

    /** The sound clips we're managing. */
    protected ArrayList<SoundEntry> _sounds = Lists.newArrayList();

    /** A count for currently playing clips. */
    protected Multiset<String> _counts = HashMultiset.create();

    /** A map for gain levels at different sound counts. */
    protected static final float[] GAIN_LEVEL = { 1f, 1.5f, 2.0f, 2.4f };

    /** The minimum time gap before playing an identical sound path. */
    protected static final float MIN_GAP = 0.05f;

    /** The minimum time gap before stopping a playing sound path. */
    protected static final float MIN_STOP = 0.15f;
}
