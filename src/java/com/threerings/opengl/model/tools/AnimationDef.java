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

import com.threerings.opengl.model.config.AnimationConfig;

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
         * Returns the transforms for each target.
         */
        public Transform3D[] getTransforms (String[] targets, float scale)
        {
            Transform3D[] xforms = new Transform3D[targets.length];
            for (int ii = 0; ii < targets.length; ii++) {
                TransformDef tdef = transforms.get(targets[ii]);
                xforms[ii] = (tdef == null) ? new Transform3D() : tdef.createTransform(scale);
            }
            return xforms;
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
     * Updates the supplied configuration with the animation data in this definition.
     */
    public void update (AnimationConfig.Imported config)
    {
        config.rate = frameRate;
        config.targets = getTargets();
        config.transforms = getTransforms(
            config.targets, config.scale, config.loop && config.skipLastFrame);
    }

    /**
     * Returns the names of all of the animation targets.
     */
    public String[] getTargets ()
    {
        HashSet<String> tset = new HashSet<String>();
        for (FrameDef frame : frames) {
            for (String target : frame.transforms.keySet()) {
                tset.add(target);
            }
        }
        return tset.toArray(new String[tset.size()]);
    }

    /**
     * Returns the transforms for each target, each frame.
     */
    public Transform3D[][] getTransforms (String[] targets, float scale, boolean omitLastFrame)
    {
        int nframes = frames.size() - (omitLastFrame ? 1 : 0);
        Transform3D[][] transforms = new Transform3D[nframes][];
        for (int ii = 0; ii < nframes; ii++) {
            transforms[ii] = frames.get(ii).getTransforms(targets, scale);
        }
        return transforms;
    }
}
