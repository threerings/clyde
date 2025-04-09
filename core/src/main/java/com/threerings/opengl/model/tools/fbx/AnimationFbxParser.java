package com.threerings.opengl.model.tools.fbx;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import com.lukaseichberg.fbxloader.FBXFile;
import com.lukaseichberg.fbxloader.FBXLoader;
import com.lukaseichberg.fbxloader.FBXNode;

import com.threerings.math.Transform3D;
import com.threerings.math.Quaternion;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepUtil;

import com.threerings.opengl.model.tools.AnimationDef;

import static com.threerings.opengl.Log.log;

public class AnimationFbxParser extends AbstractFbxParser
{
    /**
     * Parse the animation.
     */
    public static AnimationDef parseAnimation (InputStream in)
        throws IOException
    {
        return new AnimationFbxParser().parse(in);
    }

    /**
     * Parse the animation.
     */
    protected AnimationDef parse (InputStream in)
        throws IOException
    {
        FBXFile fbx = FBXLoader.loadFBXFile("anim", in);
        //FbxDumper.DumpToOut(fbx);

        AnimationDef anim = new AnimationDef();
        anim.frameRate = readFrameRate(fbx);

        init(fbx);

        FBXNode objects = fbx.getRootNode().getChildByName("Objects");
        populateObjects(objects, "AnimationCurve", "AnimationCurveNode", "AnimationLayer");

        // read in our default prototype limb transforms
        // NOTE ======> Unlike in the model parser, we read rotations as **Euler Angles** and
        // keep them as a 3-element array in the `rotation` property. Only at the end of
        // each frame do we process all the transforms and convert each to a quaternion
        // representation!
        // ===============
        Map<Long, AnimationDef.TransformDef> limbs = Maps.newHashMap();
        Map<String, Vector3f> rotPivots = Maps.newHashMap();
        Map<String, Vector3f> scalePivots = Maps.newHashMap();

        for (FBXNode model : objects.getChildrenByName("Model")) {
            Long id = model.getData();
            String type = model.getData(2);
            //if ("Mesh".equals(type)) continue;

            AnimationDef.TransformDef xform = new AnimationDef.TransformDef();
            xform.name = sanitizeName(model.<String>getData(1));
            xform.translation = newTranslation();
            xform.rotation = newRotationEuler();
            xform.scale = newScale();

            FBXNode props = model.getChildByName("Properties70");
            if (props == null) {
                log.warning("No props for node?", "name", xform.name);
                continue;
            }
            for (FBXNode prop : props.getChildrenByName("P")) {
                String pname = prop.getData(0);
                if ("Lcl Translation".equals(pname)) {
                    xform.translation = getXYZ(prop);
                } else if ("Lcl Rotation".equals(pname)) {
                    xform.rotation = getXYZUnsigned(prop); // EULER ANGLES
                } else if ("Lcl Scaling".equals(pname)) {
                    xform.scale = getXYZUnsigned(prop);
                } else if ("RotationPivot".equals(pname)) {
                    rotPivots.put(xform.name, new Vector3f(getXYZ(prop)));
                } else if ("ScalingPivot".equals(pname)) {
                    scalePivots.put(xform.name, new Vector3f(getXYZUnsigned(prop)));
                }
            }
            limbs.put(id, xform);
        }

        for (FBXNode stack : objects.getChildrenByName("AnimationStack")) {
            if (anim.frames.size() > 0) log.warning("Reading multiple animations? TODO");

            // create a place to stash all the frames in a sorted way
            Map<Long, AnimationDef.FrameDef> frames = Maps.newTreeMap();

            FBXNode layer = findNodeToDest(stack.<Long>getData(), "AnimationLayer");
            if (layer == null) {
                log.warning("Can't find layer?");
                continue;
            }
            for (Connection cc : connsByDest.get(layer.<Long>getData())) {
                Long curveNodeId = cc.srcId;
                FBXNode srcNode = findNode(curveNodeId, "AnimationCurveNode");
                if (srcNode == null) {
                    log.info("layer connects to whaaaat? " + formatObj(cc.srcId));
                    continue;
                }
                for (Connection nodeCon : connsBySrc.get(curveNodeId)) {
                    if (!nodeCon.type.equals("OP")) continue;
                    Long limbId = nodeCon.destId;
                    AnimationDef.TransformDef limbProto = limbs.get(limbId);
                    if (limbProto == null) {
                        // TODO: MaxHandle?
                        log.warning("Unable to find limb prototype for curve node?",
                                "src", cc.srcId, "prop", nodeCon.property);
                        continue;
                    }

                    // clone the limbProto so that we have a limb to work with...
                    AnimationDef.TransformDef limb = DeepUtil.copy(limbProto);
                    boolean trans, scale, rot;
                    if (trans = "Lcl Translation".equals(nodeCon.property)) {
                        limb.translation = parseCurveTriplet(srcNode);
                        scale = rot = false;

                    } else if (scale = "Lcl Scaling".equals(nodeCon.property)) {
                        limb.scale = parseCurveTripletUnsigned(srcNode);
                        rot = false;

                    } else if (rot = "Lcl Rotation".equals(nodeCon.property)) {
                        limb.rotation = parseCurveTripletUnsigned(srcNode); // EULER ANGLES
                    } else {
                        log.info("Unknown curve type?", "prop", nodeCon.property);
                        continue;
                    }
                    // Now find the curves that have this curvenode as their dest
                    for (Connection curveCon : connsByDest.get(curveNodeId)) {
                        FBXNode curve = findNode(curveCon.srcId, "AnimationCurve");
                        if (curve == null) {
                            log.warning("Can't find curve for node?",
                                "node", formatObj(srcNode, curveNodeId),
                                "curve", formatObj(curveCon.srcId));
                            continue;
                        }
                        int idx, sgn;
                        if ("d|X".equals(curveCon.property)) {
                            idx = xAxis;
                            sgn = xAxisSign;
                        } else if ("d|Y".equals(curveCon.property)) {
                            idx = yAxis;
                            sgn = yAxisSign;
                        } else if ("d|Z".equals(curveCon.property)) {
                            idx = zAxis;
                            sgn = zAxisSign;
                        } else {
                            log.warning("Unknown curve property?", "prop", curveCon.property);
                            continue;
                        }

                        long[] times = curve.getChildProperty("KeyTime");
                        float[] values = curve.getChildProperty("KeyValueFloat");
                        //float defVal = curve.<Double>getChildProperty("Default").floatValue();

                        for (int ii = 0, nn = times.length; ii < nn; ++ii) {
                            AnimationDef.FrameDef frame = frames.get(times[ii]);
                            if (frame == null) {
                                frames.put(times[ii], frame = new AnimationDef.FrameDef());
                            }
                            AnimationDef.TransformDef xform = frame.transforms.get(limb.name);
                            if (xform == null) {
                                frame.addTransform(xform = DeepUtil.copy(limb));
                            }
                            // now read the one value
                            if (trans) xform.translation[idx] = values[ii] * sgn;
                            else if (scale) xform.scale[idx] = values[ii];
                            else if (rot) xform.rotation[idx] = values[ii];
                            else throw new RuntimeException("Unhandled curve read");
                        }
                    }
                }
            }

            // now copy all the frames into the animation
            for (AnimationDef.FrameDef frame : frames.values()) { // this is sorted by timestamp
                // fix all the rotations to be quaternion based
                for (AnimationDef.TransformDef td : frame.transforms.values()) {
                    td.rotation = fromEuler(td.rotation[0], td.rotation[1], td.rotation[2]);
                    Vector3f rpivot = rotPivots.get(td.name);
                    Vector3f spivot = scalePivots.get(td.name);
                    if (rpivot != null || spivot != null) {
                        Transform3D xform = new Transform3D(
                            new Vector3f(td.translation), Quaternion.IDENTITY);
                        if (rpivot == null) rpivot = Vector3f.ZERO;
                        xform.composeLocal(new Transform3D(rpivot, new Quaternion(td.rotation)));
                        xform.composeLocal(new Transform3D(rpivot.negate(), Quaternion.IDENTITY));
                        if (spivot == null) spivot = Vector3f.ZERO;
                        xform.composeLocal(new Transform3D(spivot, Quaternion.IDENTITY));
                        xform.composeLocal(new Transform3D(
                            Vector3f.ZERO, Quaternion.IDENTITY, new Vector3f(td.scale)));
                        xform.composeLocal(new Transform3D(spivot.negate(), Quaternion.IDENTITY));
                        // read values back out... whee!
                        xform.extractTranslation().get(td.translation);
                        xform.extractRotation().get(td.rotation);
                        xform.extractScale().get(td.scale);
                    }
                }
                anim.addFrame(frame);
            }
        }

        // Debug
//        log.info("Parsed animation", "frameRate", anim.frameRate, "frames", anim.frames.size());
//        for (AnimationDef.FrameDef frame : anim.frames) {
//            log.info("Frame: " + frame.transforms.size() + " transforms.");
//            for (AnimationDef.TransformDef xform : frame.transforms.values()) {
//                log.info("  Node: " + xform.name, "trans", xform.translation, "rot", xform.rotation,
//                        "scale", xform.scale);
//            }
//        }

        return anim;
    }

    protected float[] parseCurveTriplet (FBXNode curveNode) {
        float[] triplet = parseCurveTripletUnsigned(curveNode);
        triplet[0] *= xAxisSign;
        triplet[1] *= yAxisSign;
        triplet[2] *= zAxisSign;
        return triplet;
    }

    protected float[] parseCurveTripletUnsigned (FBXNode curveNode) {
        FBXNode props = curveNode.getChildByName("Properties70");
        if (props != null && props.getNumChildren() > 2) {
            return new float[] {
                props.getChild(xAxis).<Double>getData(4).floatValue(),
                props.getChild(yAxis).<Double>getData(4).floatValue(),
                props.getChild(zAxis).<Double>getData(4).floatValue() };
        }
        log.warning("Oh shit!", "curveNode", formatObj(curveNode, null));
        return new float[3];
    }

    /**
     * Read the frame rate or return 0f if it can't be determined.
     */
    protected float readFrameRate (FBXFile fbx)
    {
        FBXNode settings = fbx.getRootNode().getChildByName("GlobalSettings");
        if (settings != null) {
            FBXNode props = settings.getChildByName("Properties70");
            FBXNode timeMode = null;
            for (FBXNode prop : props.getChildrenByName("P")) {
                String pname = prop.getData();
                if ("TimeMode".equals(pname)) timeMode = prop;
                else if ("CustomFrameRate".equals(pname)) {
                    double rate = prop.<Double>getData(4);
                    if (rate > -1) return (float)rate;
                }
            }
            if (timeMode != null) {
                int enumVal = timeMode.<Integer>getData(4);
                // https://help.autodesk.com/cloudhelp/2020/ENU/FBX-API-Reference/cpp_ref/class_fbx_time.html#a837590fd5310ff5df56ffcf7c394787e
                final float[] fbxTimeEMode = new float [] {
                     14f, // eDefaultMode
                    120f, // eFrames120
                    100f, // eFrames100
                     60f, // eFrames60
                     50f, // eFrames50
                     48f, // eFrames48
                     30f, // eFrames30
                     30f, // eFrames30Drop
                     29.9700262f, // eNTSCDropFrame
                     29.9700262f, // eNTSCFullFrame
                     25f, // ePAL
                     24f, // eFrames24
                   1000f, // eFrames1000
                     23.976f, // eFilmFullFrame
                      0f, // eCustom
                     96f, // eFrames96
                     72f, // eFrames72
                     59.94f, // eFrames59dot94
                    119.88f  // eFrames119dot88
                };
                if (enumVal >= 0 && enumVal < fbxTimeEMode.length) return fbxTimeEMode[enumVal];
            }
        }
        return 0f;
    }

    protected AnimationFbxParser () { /* instantiate via the static method. Bleah. */ }
}
