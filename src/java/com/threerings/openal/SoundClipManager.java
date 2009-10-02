//
// $Id$

package com.threerings.openal;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import com.samskivert.util.CountHashMap;

import com.threerings.openal.config.SounderConfig;

import static com.threerings.ClydeLog.*;

public class SoundClipManager
{
    /**
     * Registers and plays a sound using the clip manager.
     */
    public void playSound (Sound sound, SounderConfig.Clip config)
    {
        String path = sound.getBuffer().getPath();
        int count = _counts.getCount(path);
        boolean canStop = false;
        if (sound.isPlaying() || count > 0) {
            for (int ii = _sounds.size() - 1; ii >= 0; ii--) {
                SoundEntry entry = _sounds.get(ii);
                if (sound.isPlaying()) {
                    if (entry.sound == sound) {
                        _sounds.remove(ii);
                        _counts.incrementCount(path, -1);
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
                    _counts.incrementCount(entry.sound.getBuffer().getPath(), -1);
                    log.debug("ClipManager sound popped", "path", path);
                    break;
                }
            }
        }
        if (sound.play(null, config.loop)) {
            _sounds.add(new SoundEntry(sound, config.gain));
            count = _counts.incrementCount(path, 1);
            sound.setGain(config.gain * GAIN_LEVEL[count - 1] / count);
            log.debug("ClipManager play sound", "count", count, "path", path);
        }
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
                _counts.incrementCount(entry.sound.getBuffer().getPath(), -1);
                log.debug("ClipManager sound ended", "path", entry.sound.getBuffer().getPath());
            }
        }
        for (int ii = _sounds.size() - 1; ii >= 0; ii--) {
            SoundEntry entry = _sounds.get(ii);
            entry.elapsed += elapsed;
            int count = _counts.getCount(entry.sound.getBuffer().getPath());
            entry.sound.setGain(entry.gain * GAIN_LEVEL[count - 1] / count);
        }
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

    /** The sound clips we're managing. */
    protected ArrayList<SoundEntry> _sounds = Lists.newArrayList();

    /** A count for currently playing clips. */
    protected CountHashMap<String> _counts = new CountHashMap<String>();

    /** A map for gain levels at different sound counts. */
    protected static final float[] GAIN_LEVEL = { 1f, 1.5f, 2.0f, 2.4f };

    /** The minimum time gap before playing an identical sound path. */
    protected static final float MIN_GAP = 0.05f;

    /** The minimum time gap before stopping a playing sound path. */
    protected static final float MIN_STOP = 0.15f;
}
