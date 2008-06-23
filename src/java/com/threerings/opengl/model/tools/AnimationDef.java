//
// $Id$

package com.threerings.opengl.model.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Animation.Frame;

/**
 * An intermediate representation for animations used to store data parsed from XML.
 */
public class AnimationDef
{
    /** The rate of the animation in frames per second. */
    public float frameRate;

    /**
     * A single frame of the animation.
     */
    public static class FrameDef
    {
        /** Transforms for affected nodes, mapped by name. */
        public HashMap<String, TransformDef> transforms = new HashMap<String, TransformDef>();

        /**
         * Called by the parser to add a transform to this frame.
         */
        public void addTransform (TransformDef transform)
        {
            transforms.put(transform.name, transform);
        }

        /**
         * Builds a {@link Frame} from this definition for the identified targets.
         */
        public Frame createFrame (String[] targets, float gscale)
        {
            Transform3D[] xforms = new Transform3D[targets.length];
            for (int ii = 0; ii < targets.length; ii++) {
                TransformDef tdef = transforms.get(targets[ii]);
                xforms[ii] = (tdef == null) ? new Transform3D() : tdef.createTransform(gscale);
            }
            return new Frame(xforms);
        }
    }

    /**
     * A transform for a single node.
     */
    public static class TransformDef
    {
        /** The name of the affected node. */
        public String name;

        /** The transformation parameters. */
        public float[] translation;
        public float[] rotation;
        public float[] scale;

        /**
         * Builds a {@link Transform3D} from this definition.
         */
        public Transform3D createTransform (float gscale)
        {
            return ModelDef.createTransform(translation, rotation, scale, gscale);
        }
    }

    /** The individual frames of the animation. */
    public ArrayList<FrameDef> frames = new ArrayList<FrameDef>();

    /**
     * Called by the parser to add a frame to this animation.
     */
    public void addFrame (FrameDef frame)
    {
        frames.add(frame);
    }

    /**
     * Builds an {@link Animation} from this definition and the supplied properties.
     */
    public Animation createAnimation (Properties props)
    {
        // find the names of all animation targets
        HashSet<String> tset = new HashSet<String>();
        for (FrameDef frame : frames) {
            for (String target : frame.transforms.keySet()) {
                tset.add(target);
            }
        }
        String[] targets = tset.toArray(new String[tset.size()]);

        // see if we should leave out the last frame
        boolean looping = Boolean.parseBoolean(props.getProperty("looping"));
        boolean skipLastFrame = Boolean.parseBoolean(props.getProperty("skip_last_frame", "true"));

        // read the global scale value
        float gscale = Float.parseFloat(props.getProperty("scale", "0.01"));

        // build the list of frames
        Frame[] aframes = new Frame[frames.size() - (looping && skipLastFrame ? 1 : 0)];
        for (int ii = 0; ii < aframes.length; ii++) {
            aframes[ii] = frames.get(ii).createFrame(targets, gscale);
        }

        // create and return the animation
        return new Animation(props, frameRate, targets, aframes);
    }
}
