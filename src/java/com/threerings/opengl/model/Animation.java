//
// $Id$

package com.threerings.opengl.model;

import java.util.Properties;

import com.threerings.math.Transform3D;

import com.threerings.export.Exportable;

/**
 * An animation for a model.
 */
public class Animation
    implements Exportable
{
    /**
     * A single animation frame.
     */
    public static class Frame
        implements Exportable
    {
        public Frame (Transform3D[] transforms)
        {
            _transforms = transforms;
        }

        public Frame ()
        {
        }

        /**
         * Returns the transforms for each animation target.
         */
        public Transform3D[] getTransforms ()
        {
            return _transforms;
        }

        /** The transforms for each animation target. */
        protected Transform3D[] _transforms;
    }

    /**
     * Creates a new animation.
     */
    public Animation (Properties props, float frameRate, String[] targets, Frame[] frames)
    {
        _props = props;
        _frameRate = frameRate;
        _targets = targets;
        _frames = frames;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Animation ()
    {
    }

    /**
     * Prepares the animation for use.
     */
    public void init ()
    {
        _looping = Boolean.parseBoolean(_props.getProperty("looping"));
    }

    /**
     * Returns a reference to the animation properties.
     */
    public Properties getProperties ()
    {
        return _props;
    }

    /**
     * Returns the base frame rate of the animation (frames per second).
     */
    public float getFrameRate ()
    {
        return _frameRate;
    }

    /**
     * Returns the names of the animation targets.
     */
    public String[] getTargets ()
    {
        return _targets;
    }

    /**
     * Returns the array of animation frames.
     */
    public Frame[] getFrames ()
    {
        return _frames;
    }

    /**
     * Checks whether or not the animation loops.
     */
    public boolean isLooping ()
    {
        return _looping;
    }

    /** The properties of the animation. */
    protected Properties _props;

    /** The base frame rate of the animation (in frames per second). */
    protected float _frameRate;

    /** The names of the animated nodes. */
    protected String[] _targets;

    /** The frames of the animation. */
    protected Frame[] _frames;

    /** Whether or not the animation loops. */
    protected transient boolean _looping;
}
